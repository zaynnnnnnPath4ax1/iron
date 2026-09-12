package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.cv.GestureType
import com.example.data.EnergyRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoltAccessibilityService : AccessibilityService() {

    private val actionResolver = AppActionResolver()

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.i(TAG, "VoltAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrEmpty() && !pkg.contains("com.android.systemui") && !pkg.contains("com.aistudio.volt")) {
            _currentForegroundPackage.value = pkg
        }

        // Heuristic detection of short-form video navigation
        val targetApp = actionResolver.resolveTargetApp(pkg)
        if (targetApp != SupportedApp.GENERIC) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                    val className = event.className?.toString() ?: ""
                    val contentDescription = event.contentDescription?.toString() ?: ""
                    val text = event.text.joinToString()
                    val combined = "$className|$contentDescription|$text"

                    // If it's Instagram, YouTube or Facebook, check for Reels / Shorts patterns
                    val isShortFormContent = when (targetApp) {
                        SupportedApp.INSTAGRAM -> combined.contains("Reel", ignoreCase = true) ||
                                className.contains("ViewPager", ignoreCase = true) ||
                                className.contains("Clips", ignoreCase = true) ||
                                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                        SupportedApp.YOUTUBE -> combined.contains("Shorts", ignoreCase = true) ||
                                className.contains("Reel", ignoreCase = true) ||
                                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                        SupportedApp.FACEBOOK -> combined.contains("Reel", ignoreCase = true) ||
                                event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
                        SupportedApp.GENERIC -> false
                    }

                    if (isShortFormContent && pkg != null) {
                        val contentSignature = "$pkg-${combined.hashCode()}"
                        val settings = SettingsRepository.getInstance(applicationContext)
                        val energyRepo = EnergyRepository.getInstance(applicationContext, settings)
                        energyRepo.onContentScrolled(pkg, contentSignature)
                    }
                }
                else -> {}
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "VoltAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isServiceConnected.value = false
        }
        Log.i(TAG, "VoltAccessibilityService destroyed")
    }

    private fun performGlobalSwipe(gesture: GestureType): Boolean {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        val currentPkg = _currentForegroundPackage.value
        val coords = actionResolver.resolveSwipe(gesture, width, height, currentPkg) ?: return false
        val path = actionResolver.createPath(coords)

        val stroke = GestureDescription.StrokeDescription(path, 0L, coords.durationMs)
        val gestureDesc = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gestureDesc, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                _lastExecutedGesture.value = gesture
                triggerHaptic()

                // Register gesture-driven reel viewing in energy system
                currentPkg?.let { pkg ->
                    val settings = SettingsRepository.getInstance(applicationContext)
                    val energyRepo = EnergyRepository.getInstance(applicationContext, settings)
                    energyRepo.onContentScrolled(pkg, "gesture-${System.currentTimeMillis()}")
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "Gesture swipe cancelled")
            }
        }, null)

        return dispatched
    }

    private fun triggerHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            // Ignore if vibration unavailable or permission not enabled
        }
    }

    companion object {
        private const val TAG = "VoltAccessibility"

        @Volatile
        var instance: VoltAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _currentForegroundPackage = MutableStateFlow<String?>(null)
        val currentForegroundPackage: StateFlow<String?> = _currentForegroundPackage.asStateFlow()

        private val _lastExecutedGesture = MutableStateFlow<GestureType?>(null)
        val lastExecutedGesture: StateFlow<GestureType?> = _lastExecutedGesture.asStateFlow()

        fun dispatchGestureAction(gesture: GestureType): Boolean {
            val service = instance ?: return false
            return service.performGlobalSwipe(gesture)
        }
    }
}
