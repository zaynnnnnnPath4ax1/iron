package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class EnergyState(
    val currentEnergy: Int = 100,
    val reelsToday: Int = 0,
    val scrollingMinutesToday: Int = 0,
    val warningMessage: String? = null,
    val isRecharging: Boolean = false
)

class EnergyRepository(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("volt_energy_prefs", Context.MODE_PRIVATE)

    private val _energyState = MutableStateFlow(loadInitialState())
    val energyState: StateFlow<EnergyState> = _energyState.asStateFlow()

    private val _warningEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val warningEvents: SharedFlow<String> = _warningEvents.asSharedFlow()

    // Anti-duplicate content detection cache: package -> (contentSignature, timestamp)
    private val recentContentCache = mutableMapOf<String, Pair<String, Long>>()
    private var lastActivityTimestamp: Long = prefs.getLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis())

    init {
        checkDailyReset()
        applyPeriodicRecharge()
    }

    private fun loadInitialState(): EnergyState {
        val energy = prefs.getInt(KEY_CURRENT_ENERGY, 100)
        val reels = prefs.getInt(KEY_REELS_TODAY, 0)
        val minutes = prefs.getInt(KEY_MINUTES_TODAY, 0)
        val warning = calculateWarning(energy)
        return EnergyState(
            currentEnergy = energy,
            reelsToday = reels,
            scrollingMinutesToday = minutes,
            warningMessage = warning
        )
    }

    private fun getTodayEpochDay(): Long {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.YEAR) * 1000L + calendar.get(Calendar.DAY_OF_YEAR)
    }

    @Synchronized
    fun checkDailyReset() {
        val today = getTodayEpochDay()
        val storedDay = prefs.getLong(KEY_LAST_SAVED_DAY, 0L)
        if (storedDay != today) {
            prefs.edit()
                .putLong(KEY_LAST_SAVED_DAY, today)
                .putInt(KEY_CURRENT_ENERGY, 100)
                .putInt(KEY_REELS_TODAY, 0)
                .putInt(KEY_MINUTES_TODAY, 0)
                .putLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis())
                .apply()

            _energyState.value = EnergyState(
                currentEnergy = 100,
                reelsToday = 0,
                scrollingMinutesToday = 0,
                warningMessage = null,
                isRecharging = false
            )
        }
    }

    @Synchronized
    fun onContentScrolled(packageName: String, contentSignature: String): Boolean {
        if (!settingsRepository.energySystemEnabled.value) return false
        checkDailyReset()

        val now = System.currentTimeMillis()
        val lastRecord = recentContentCache[packageName]

        // Content / session debounce system:
        // Same signature within 15 seconds or any scroll on same package within 2.2 seconds counts only once
        if (lastRecord != null) {
            val (lastSig, lastTime) = lastRecord
            val elapsed = now - lastTime
            if (lastSig == contentSignature && elapsed < 15_000L) {
                return false // duplicate piece of content
            }
            if (elapsed < 2_200L) {
                return false // rapid successive scroll debounce
            }
        }

        // Valid new Reel / Short detected!
        recentContentCache[packageName] = Pair(contentSignature, now)
        lastActivityTimestamp = now
        prefs.edit().putLong(KEY_LAST_ACTIVITY_TIME, now).apply()

        val drainAmount = settingsRepository.drainRate.value.energyPerReel
        val currentEnergy = _energyState.value.currentEnergy
        val newEnergy = (currentEnergy - drainAmount.toInt().coerceAtLeast(1)).coerceAtLeast(0)
        val newReels = _energyState.value.reelsToday + 1
        val newMinutes = (newReels * 40 / 60).coerceAtLeast(1) // Average ~40s per reel heuristic

        val warning = calculateWarning(newEnergy)

        prefs.edit()
            .putInt(KEY_CURRENT_ENERGY, newEnergy)
            .putInt(KEY_REELS_TODAY, newReels)
            .putInt(KEY_MINUTES_TODAY, newMinutes)
            .apply()

        _energyState.value = _energyState.value.copy(
            currentEnergy = newEnergy,
            reelsToday = newReels,
            scrollingMinutesToday = newMinutes,
            warningMessage = warning,
            isRecharging = false
        )

        if (warning != null && shouldTriggerWarningAlert(currentEnergy, newEnergy)) {
            _warningEvents.tryEmit(warning)
        }

        return true
    }

    private fun calculateWarning(energy: Int): String? {
        return when {
            energy <= 0 -> "Energy Empty ⚡ Take a short break and recharge."
            energy <= 10 -> "Take a break?"
            energy <= 25 -> "Low Energy"
            energy <= 50 -> "Energy is getting low ⚡"
            else -> null
        }
    }

    private fun shouldTriggerWarningAlert(oldEnergy: Int, newEnergy: Int): Boolean {
        return (oldEnergy > 50 && newEnergy <= 50) ||
                (oldEnergy > 25 && newEnergy <= 25) ||
                (oldEnergy > 10 && newEnergy <= 10) ||
                (oldEnergy > 0 && newEnergy <= 0)
    }

    @Synchronized
    fun applyPeriodicRecharge() {
        val now = System.currentTimeMillis()
        val elapsedMs = now - lastActivityTimestamp
        val elapsedMinutes = (elapsedMs / (1000 * 60)).toInt()

        // 10 minutes away -> +10 Energy (1 energy per min away)
        if (elapsedMinutes >= 1) {
            val rechargeGain = elapsedMinutes.coerceAtMost(100)
            val current = _energyState.value.currentEnergy
            val newEnergy = (current + rechargeGain).coerceAtMost(100)

            if (newEnergy != current) {
                prefs.edit().putInt(KEY_CURRENT_ENERGY, newEnergy).apply()
                _energyState.value = _energyState.value.copy(
                    currentEnergy = newEnergy,
                    warningMessage = calculateWarning(newEnergy),
                    isRecharging = true
                )
            }
        }
    }

    fun manualRecharge(amount: Int = 20) {
        val newEnergy = (_energyState.value.currentEnergy + amount).coerceAtMost(100)
        prefs.edit().putInt(KEY_CURRENT_ENERGY, newEnergy).apply()
        _energyState.value = _energyState.value.copy(
            currentEnergy = newEnergy,
            warningMessage = calculateWarning(newEnergy),
            isRecharging = true
        )
    }

    companion object {
        private const val KEY_CURRENT_ENERGY = "current_energy"
        private const val KEY_REELS_TODAY = "reels_today"
        private const val KEY_MINUTES_TODAY = "minutes_today"
        private const val KEY_LAST_SAVED_DAY = "last_saved_day"
        private const val KEY_LAST_ACTIVITY_TIME = "last_activity_time"

        @Volatile
        private var INSTANCE: EnergyRepository? = null

        fun getInstance(context: Context, settingsRepo: SettingsRepository): EnergyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnergyRepository(context.applicationContext, settingsRepo).also { INSTANCE = it }
            }
        }
    }
}
