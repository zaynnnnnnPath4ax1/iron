package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.accessibility.VoltAccessibilityService
import com.example.cv.GestureType
import com.example.cv.HandDetectionResult
import com.example.data.DrainRate
import com.example.data.EnergyRepository
import com.example.data.EnergyState
import com.example.data.Sensitivity
import com.example.data.SettingsRepository
import com.example.service.VoltGestureService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VoltUiState(
    val isGestureControlActive: Boolean = false,
    val energyState: EnergyState = EnergyState(),
    val isAccessibilityConnected: Boolean = false,
    val currentForegroundPackage: String? = null,
    val sensitivity: Sensitivity = Sensitivity.MEDIUM,
    val drainRate: DrainRate = DrainRate.NORMAL,
    val isEnergySystemEnabled: Boolean = true,
    val lastExecutedGesture: GestureType? = null,
    val latestDetection: HandDetectionResult? = null,
    val alertToast: String? = null
)

class VoltViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository.getInstance(application)
    private val energyRepository = EnergyRepository.getInstance(application, settingsRepository)

    private val _alertToast = MutableStateFlow<String?>(null)
    val alertToast: StateFlow<String?> = _alertToast.asStateFlow()

    init {
        viewModelScope.launch {
            energyRepository.warningEvents.collect { warning ->
                _alertToast.value = warning
            }
        }
    }

    val uiState: StateFlow<VoltUiState> = combine(
        VoltGestureService.isServiceRunning,
        energyRepository.energyState,
        VoltAccessibilityService.isServiceConnected,
        VoltAccessibilityService.currentForegroundPackage,
        settingsRepository.sensitivity,
        settingsRepository.drainRate,
        settingsRepository.energySystemEnabled,
        VoltGestureService.lastTriggeredGesture,
        VoltGestureService.latestDetection
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        VoltUiState(
            isGestureControlActive = params[0] as Boolean,
            energyState = params[1] as EnergyState,
            isAccessibilityConnected = params[2] as Boolean,
            currentForegroundPackage = params[3] as String?,
            sensitivity = params[4] as Sensitivity,
            drainRate = params[5] as DrainRate,
            isEnergySystemEnabled = params[6] as Boolean,
            lastExecutedGesture = params[7] as GestureType?,
            latestDetection = params[8] as HandDetectionResult?,
            alertToast = _alertToast.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VoltUiState()
    )

    fun toggleGestureControl(hasCameraPermission: Boolean) {
        val current = uiState.value.isGestureControlActive
        if (!current) {
            if (!hasCameraPermission) {
                _alertToast.value = "Camera permission is required to detect air gestures."
                return
            }
            settingsRepository.setGestureControlEnabled(true)
            VoltGestureService.start(getApplication())
        } else {
            settingsRepository.setGestureControlEnabled(false)
            VoltGestureService.stop(getApplication())
        }
    }

    fun stopGestureControl() {
        settingsRepository.setGestureControlEnabled(false)
        VoltGestureService.stop(getApplication())
    }

    fun simulateGesture(gesture: GestureType) {
        val executed = VoltAccessibilityService.dispatchGestureAction(gesture)
        if (!executed) {
            // Local fallback simulation if accessibility is not enabled yet
            val currentPkg = uiState.value.currentForegroundPackage ?: "simulation"
            energyRepository.onContentScrolled(currentPkg, "sim-${System.currentTimeMillis()}")
            _alertToast.value = "Simulated ${gesture.displayName} (Enable Accessibility for global apps)"
        }
    }

    fun rechargeEnergy() {
        energyRepository.manualRecharge(25)
    }

    fun setSensitivity(sensitivity: Sensitivity) {
        settingsRepository.setSensitivity(sensitivity)
    }

    fun setDrainRate(drainRate: DrainRate) {
        settingsRepository.setDrainRate(drainRate)
    }

    fun setEnergySystemEnabled(enabled: Boolean) {
        settingsRepository.setEnergySystemEnabled(enabled)
    }

    fun clearAlertToast() {
        _alertToast.value = null
    }
}
