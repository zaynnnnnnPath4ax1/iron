package com.example.cv

enum class GestureType(val displayName: String, val description: String) {
    ONE_FINGER_DOWN("One Finger Down", "Next Reel / Scroll Down"),
    TWO_FINGER_UP("Two Fingers Up", "Previous Reel / Scroll Up"),
    NONE("None", "No gesture")
}

data class HandDetectionResult(
    val isHandPresent: Boolean,
    val fingerCount: Int,
    val normalizedTipX: Float,
    val normalizedTipY: Float,
    val confidence: Float,
    val timestampMs: Long
)
