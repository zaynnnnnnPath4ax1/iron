package com.example.cv

import com.example.data.Sensitivity

enum class MachineState {
    IDLE,
    HAND_DETECTED,
    GESTURE_CANDIDATE,
    DIRECTION_CONFIRMED,
    GESTURE_COMPLETED,
    ACTION_TRIGGERED,
    COOLDOWN
}

class GestureStateMachine(
    var sensitivity: Sensitivity = Sensitivity.MEDIUM,
    private val onGestureAction: (GestureType) -> Unit = {}
) {
    var currentState: MachineState = MachineState.IDLE
        private set

    private var startX: Float = 0f
    private var startY: Float = 0f
    private var lastY: Float = 0f
    private var targetFingerCount: Int = 0
    private var candidateGesture: GestureType = GestureType.NONE
    private var consecutiveDirectionFrames: Int = 0
    private var candidateStartTime: Long = 0L
    private var lastActionTimestamp: Long = 0L

    fun processFrame(detection: HandDetectionResult): GestureType? {
        val now = detection.timestampMs

        // Handle COOLDOWN state
        if (currentState == MachineState.COOLDOWN) {
            if (now - lastActionTimestamp >= sensitivity.cooldownMs) {
                currentState = MachineState.IDLE
            } else {
                return null
            }
        }

        // Low confidence or hand lost -> reset unless currently triggering
        if (!detection.isHandPresent || detection.confidence < MIN_CONFIDENCE) {
            if (currentState != MachineState.COOLDOWN) {
                resetToIdle()
            }
            return null
        }

        // Must be either 1 or 2 fingers for our two target gestures
        if (detection.fingerCount !in 1..2) {
            resetToIdle()
            return null
        }

        when (currentState) {
            MachineState.IDLE -> {
                currentState = MachineState.HAND_DETECTED
                targetFingerCount = detection.fingerCount
                startX = detection.normalizedTipX
                startY = detection.normalizedTipY
                lastY = startY
                consecutiveDirectionFrames = 0
                candidateStartTime = now
                candidateGesture = GestureType.NONE
            }

            MachineState.HAND_DETECTED -> {
                // Check if finger count changed unexpectedly
                if (detection.fingerCount != targetFingerCount) {
                    resetToIdle()
                    return null
                }

                val dy = detection.normalizedTipY - startY
                val isDown = dy > JITTER_THRESHOLD && targetFingerCount == 1
                val isUp = dy < -JITTER_THRESHOLD && targetFingerCount == 2

                if (isDown) {
                    currentState = MachineState.GESTURE_CANDIDATE
                    candidateGesture = GestureType.ONE_FINGER_DOWN
                    consecutiveDirectionFrames = 1
                    lastY = detection.normalizedTipY
                } else if (isUp) {
                    currentState = MachineState.GESTURE_CANDIDATE
                    candidateGesture = GestureType.TWO_FINGER_UP
                    consecutiveDirectionFrames = 1
                    lastY = detection.normalizedTipY
                } else {
                    // Hand held stationary in place: update baseline slightly to prevent accumulation
                    if (now - candidateStartTime > 500L) {
                        startY = detection.normalizedTipY
                        startX = detection.normalizedTipX
                    }
                }
            }

            MachineState.GESTURE_CANDIDATE -> {
                // Invalidate if finger count changed
                if (detection.fingerCount != targetFingerCount) {
                    resetToIdle()
                    return null
                }

                val frameDy = detection.normalizedTipY - lastY
                val totalDy = detection.normalizedTipY - startY

                val directionConsistent = when (candidateGesture) {
                    GestureType.ONE_FINGER_DOWN -> frameDy >= -0.02f // Allow minor micro-jitter, but general progression downwards
                    GestureType.TWO_FINGER_UP -> frameDy <= 0.02f   // General progression upwards
                    else -> false
                }

                if (!directionConsistent) {
                    // Abrupt reversal or unstable movement -> reset to IDLE (anti-false-trigger)
                    resetToIdle()
                    return null
                }

                consecutiveDirectionFrames++
                lastY = detection.normalizedTipY

                if (consecutiveDirectionFrames >= sensitivity.minConsecutiveFrames) {
                    currentState = MachineState.DIRECTION_CONFIRMED
                    val passesThreshold = when (candidateGesture) {
                        GestureType.ONE_FINGER_DOWN -> totalDy >= sensitivity.minDisplacementY
                        GestureType.TWO_FINGER_UP -> totalDy <= -sensitivity.minDisplacementY
                        else -> false
                    }

                    if (passesThreshold) {
                        val gestureToEmit = candidateGesture
                        currentState = MachineState.ACTION_TRIGGERED
                        lastActionTimestamp = now
                        onGestureAction(gestureToEmit)
                        currentState = MachineState.COOLDOWN
                        return gestureToEmit
                    }
                }

                // Timeout candidate if it takes too long (> 1.2s without confirmation)
                if (now - candidateStartTime > 1200L) {
                    resetToIdle()
                }
            }

            MachineState.DIRECTION_CONFIRMED -> {
                if (detection.fingerCount != targetFingerCount) {
                    resetToIdle()
                    return null
                }

                val totalDy = detection.normalizedTipY - startY

                val passesThreshold = when (candidateGesture) {
                    GestureType.ONE_FINGER_DOWN -> totalDy >= sensitivity.minDisplacementY
                    GestureType.TWO_FINGER_UP -> totalDy <= -sensitivity.minDisplacementY
                    else -> false
                }

                if (passesThreshold) {
                    currentState = MachineState.GESTURE_COMPLETED
                    val gestureToEmit = candidateGesture
                    currentState = MachineState.ACTION_TRIGGERED
                    lastActionTimestamp = now
                    onGestureAction(gestureToEmit)

                    // Immediately transition to COOLDOWN to guarantee zero duplicate triggers
                    currentState = MachineState.COOLDOWN
                    return gestureToEmit
                } else {
                    // Still moving, keep checking
                    lastY = detection.normalizedTipY
                    if (now - candidateStartTime > 1500L) {
                        resetToIdle()
                    }
                }
            }

            MachineState.GESTURE_COMPLETED,
            MachineState.ACTION_TRIGGERED,
            MachineState.COOLDOWN -> {
                // Handled at top of function
            }
        }

        return null
    }

    fun resetToIdle() {
        currentState = MachineState.IDLE
        targetFingerCount = 0
        candidateGesture = GestureType.NONE
        consecutiveDirectionFrames = 0
        startX = 0f
        startY = 0f
        lastY = 0f
    }

    companion object {
        const val MIN_CONFIDENCE = 0.60f
        const val JITTER_THRESHOLD = 0.035f // Minimum movement to be considered candidate
    }
}
