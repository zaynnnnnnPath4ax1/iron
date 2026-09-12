package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Sensitivity(val label: String, val minDisplacementY: Float, val minConsecutiveFrames: Int, val cooldownMs: Long) {
    LOW("Low", minDisplacementY = 0.22f, minConsecutiveFrames = 4, cooldownMs = 1000L),
    MEDIUM("Medium", minDisplacementY = 0.15f, minConsecutiveFrames = 3, cooldownMs = 800L),
    HIGH("High", minDisplacementY = 0.09f, minConsecutiveFrames = 2, cooldownMs = 600L)
}

enum class DrainRate(val label: String, val energyPerReel: Float) {
    LIGHT("Light (0.5⚡)", 0.5f),
    NORMAL("Normal (1⚡)", 1.0f),
    STRONG("Strong (2⚡)", 2.0f)
}

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("volt_settings", Context.MODE_PRIVATE)

    private val _gestureControlEnabled = MutableStateFlow(prefs.getBoolean(KEY_GESTURE_CONTROL, false))
    val gestureControlEnabled: StateFlow<Boolean> = _gestureControlEnabled.asStateFlow()

    private val _energySystemEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENERGY_SYSTEM, true))
    val energySystemEnabled: StateFlow<Boolean> = _energySystemEnabled.asStateFlow()

    private val _sensitivity = MutableStateFlow(
        Sensitivity.entries.find { it.name == prefs.getString(KEY_SENSITIVITY, Sensitivity.MEDIUM.name) } ?: Sensitivity.MEDIUM
    )
    val sensitivity: StateFlow<Sensitivity> = _sensitivity.asStateFlow()

    private val _drainRate = MutableStateFlow(
        DrainRate.entries.find { it.name == prefs.getString(KEY_DRAIN_RATE, DrainRate.NORMAL.name) } ?: DrainRate.NORMAL
    )
    val drainRate: StateFlow<DrainRate> = _drainRate.asStateFlow()

    fun setGestureControlEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GESTURE_CONTROL, enabled).apply()
        _gestureControlEnabled.value = enabled
    }

    fun setEnergySystemEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENERGY_SYSTEM, enabled).apply()
        _energySystemEnabled.value = enabled
    }

    fun setSensitivity(sensitivity: Sensitivity) {
        prefs.edit().putString(KEY_SENSITIVITY, sensitivity.name).apply()
        _sensitivity.value = sensitivity
    }

    fun setDrainRate(drainRate: DrainRate) {
        prefs.edit().putString(KEY_DRAIN_RATE, drainRate.name).apply()
        _drainRate.value = drainRate
    }

    companion object {
        private const val KEY_GESTURE_CONTROL = "gesture_control_enabled"
        private const val KEY_ENERGY_SYSTEM = "energy_system_enabled"
        private const val KEY_SENSITIVITY = "gesture_sensitivity"
        private const val KEY_DRAIN_RATE = "energy_drain_rate"

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
