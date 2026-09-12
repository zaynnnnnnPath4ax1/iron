package com.example.accessibility

import android.graphics.Path
import com.example.cv.GestureType

data class SwipeCoordinates(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val durationMs: Long
)

enum class SupportedApp(val packageName: String, val appName: String) {
    INSTAGRAM("com.instagram.android", "Instagram"),
    YOUTUBE("com.google.android.youtube", "YouTube"),
    FACEBOOK("com.facebook.katana", "Facebook"),
    GENERIC("", "Standard Android Apps")
}

class AppActionResolver {

    fun resolveTargetApp(packageName: String?): SupportedApp {
        if (packageName == null) return SupportedApp.GENERIC
        return when {
            packageName.contains("instagram") -> SupportedApp.INSTAGRAM
            packageName.contains("youtube") -> SupportedApp.YOUTUBE
            packageName.contains("facebook") || packageName.contains("katana") -> SupportedApp.FACEBOOK
            else -> SupportedApp.GENERIC
        }
    }

    /**
     * Resolves swipe coordinates based on dynamic screen metrics and target app.
     * ONE_FINGER_DOWN: performs Next Reel action (swipes content upward)
     * TWO_FINGER_UP: performs Previous Reel action (swipes content downward)
     */
    fun resolveSwipe(
        gesture: GestureType,
        screenWidth: Int,
        screenHeight: Int,
        currentPackage: String?
    ): SwipeCoordinates? {
        val app = resolveTargetApp(currentPackage)
        val width = screenWidth.toFloat()
        val height = screenHeight.toFloat()

        // Center X coordinate slightly offset from center to avoid center play/pause controls in some apps
        val centerX = when (app) {
            SupportedApp.INSTAGRAM -> width * 0.52f
            SupportedApp.YOUTUBE -> width * 0.50f
            SupportedApp.FACEBOOK -> width * 0.52f
            SupportedApp.GENERIC -> width * 0.50f
        }

        val duration = when (app) {
            SupportedApp.INSTAGRAM -> 200L
            SupportedApp.YOUTUBE -> 220L
            SupportedApp.FACEBOOK -> 220L
            SupportedApp.GENERIC -> 240L
        }

        return when (gesture) {
            GestureType.ONE_FINGER_DOWN -> {
                // Next Reel: Swipe up from 75% height to 25% height
                SwipeCoordinates(
                    startX = centerX,
                    startY = height * 0.76f,
                    endX = centerX,
                    endY = height * 0.22f,
                    durationMs = duration
                )
            }
            GestureType.TWO_FINGER_UP -> {
                // Previous Reel: Swipe down from 25% height to 75% height
                SwipeCoordinates(
                    startX = centerX,
                    startY = height * 0.22f,
                    endX = centerX,
                    endY = height * 0.76f,
                    durationMs = duration
                )
            }
            GestureType.NONE -> null
        }
    }

    fun createPath(coords: SwipeCoordinates): Path {
        val path = Path()
        path.moveTo(coords.startX, coords.startY)
        path.lineTo(coords.endX, coords.endY)
        return path
    }
}
