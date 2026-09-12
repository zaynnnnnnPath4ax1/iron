package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.accessibility.AppActionResolver
import com.example.accessibility.SupportedApp
import com.example.cv.GestureStateMachine
import com.example.cv.GestureType
import com.example.cv.HandDetectionResult
import com.example.cv.MachineState
import com.example.data.EnergyRepository
import com.example.data.Sensitivity
import com.example.data.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoltGestureAndEnergyTest {

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var energyRepository: EnergyRepository
    private val actionResolver = AppActionResolver()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences for clean test state
        context.getSharedPreferences("volt_settings", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("volt_energy_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        settingsRepository = SettingsRepository(context)
        energyRepository = EnergyRepository(context, settingsRepository)
    }

    @Test
    fun `one finger down triggers exactly one action`() {
        var actionCount = 0
        var emittedGesture: GestureType? = null

        val stateMachine = GestureStateMachine(sensitivity = Sensitivity.MEDIUM) { gesture ->
            actionCount++
            emittedGesture = gesture
        }

        var time = 1000L
        // Sequence of frames moving downward with 1 extended finger
        val frames = listOf(
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.20f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.26f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.32f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.42f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.44f, confidence = 0.9f, timestampMs = time.also { time += 33 })
        )

        for (frame in frames) {
            stateMachine.processFrame(frame)
        }

        assertEquals("Should trigger exactly once", 1, actionCount)
        assertEquals(GestureType.ONE_FINGER_DOWN, emittedGesture)
    }

    @Test
    fun `two fingers up triggers exactly one action`() {
        var actionCount = 0
        var emittedGesture: GestureType? = null

        val stateMachine = GestureStateMachine(sensitivity = Sensitivity.MEDIUM) { gesture ->
            actionCount++
            emittedGesture = gesture
        }

        var time = 2000L
        // Sequence of frames moving upward with 2 extended fingers
        val frames = listOf(
            HandDetectionResult(isHandPresent = true, fingerCount = 2, normalizedTipX = 0.5f, normalizedTipY = 0.60f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 2, normalizedTipX = 0.5f, normalizedTipY = 0.53f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 2, normalizedTipX = 0.5f, normalizedTipY = 0.46f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 2, normalizedTipX = 0.5f, normalizedTipY = 0.38f, confidence = 0.9f, timestampMs = time.also { time += 33 })
        )

        for (frame in frames) {
            stateMachine.processFrame(frame)
        }

        assertEquals("Should trigger exactly once", 1, actionCount)
        assertEquals(GestureType.TWO_FINGER_UP, emittedGesture)
    }

    @Test
    fun `random hand movement causes no action`() {
        var actionCount = 0
        val stateMachine = GestureStateMachine(sensitivity = Sensitivity.MEDIUM) { actionCount++ }

        var time = 3000L
        // Erratic movements: down then up then down without consistency
        val frames = listOf(
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.30f, confidence = 0.85f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.35f, confidence = 0.85f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.28f, confidence = 0.85f, timestampMs = time.also { time += 33 }), // reversed!
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.31f, confidence = 0.85f, timestampMs = time.also { time += 33 })
        )

        for (frame in frames) {
            stateMachine.processFrame(frame)
        }

        assertEquals("Random erratic movement must not trigger action", 0, actionCount)
    }

    @Test
    fun `one gesture held in position causes no repeated actions`() {
        var actionCount = 0
        val stateMachine = GestureStateMachine(sensitivity = Sensitivity.MEDIUM) { actionCount++ }

        var time = 4000L
        // Stationary hand held in place for 15 frames
        for (i in 0 until 15) {
            stateMachine.processFrame(
                HandDetectionResult(
                    isHandPresent = true,
                    fingerCount = 1,
                    normalizedTipX = 0.5f,
                    normalizedTipY = 0.35f + (i % 2) * 0.005f, // Micro-jitter
                    confidence = 0.85f,
                    timestampMs = time.also { time += 33 }
                )
            )
        }

        assertEquals("Stationary hand must not trigger any actions", 0, actionCount)
    }

    @Test
    fun `low confidence detection causes no action`() {
        var actionCount = 0
        val stateMachine = GestureStateMachine(sensitivity = Sensitivity.MEDIUM) { actionCount++ }

        var time = 5000L
        val frames = listOf(
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.20f, confidence = 0.40f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.40f, confidence = 0.45f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.55f, confidence = 0.35f, timestampMs = time.also { time += 33 })
        )

        for (frame in frames) {
            stateMachine.processFrame(frame)
        }

        assertEquals("Low confidence frames must be ignored", 0, actionCount)
    }

    @Test
    fun `cooldown prevents accidental double action`() {
        var actionCount = 0
        val stateMachine = GestureStateMachine(sensitivity = Sensitivity.MEDIUM) { actionCount++ }

        var time = 6000L
        // 1. First successful gesture
        val firstGestureFrames = listOf(
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.20f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.28f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.36f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.42f, confidence = 0.9f, timestampMs = time.also { time += 33 })
        )
        for (frame in firstGestureFrames) stateMachine.processFrame(frame)

        assertEquals("First gesture should trigger", 1, actionCount)

        // 2. Immediate second downward motion within 200ms (during 800ms cooldown)
        time += 50
        val immediateFrames = listOf(
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.20f, confidence = 0.9f, timestampMs = time.also { time += 33 }),
            HandDetectionResult(isHandPresent = true, fingerCount = 1, normalizedTipX = 0.5f, normalizedTipY = 0.42f, confidence = 0.9f, timestampMs = time.also { time += 33 })
        )
        for (frame in immediateFrames) stateMachine.processFrame(frame)

        assertEquals("Immediate repeated gesture must be blocked by cooldown", 1, actionCount)
    }

    @Test
    fun `energy system starts at 100 and decreases on reel scrolls`() {
        assertEquals(100, energyRepository.energyState.value.currentEnergy)

        val counted1 = energyRepository.onContentScrolled("com.instagram.android", "reel-1")
        assertTrue(counted1)
        assertEquals(99, energyRepository.energyState.value.currentEnergy)

        // Advance simulated time past debounce
        Thread.sleep(50) // Small tick
        val counted2 = energyRepository.onContentScrolled("com.google.android.youtube", "short-1")
        assertTrue(counted2)
        assertEquals(98, energyRepository.energyState.value.currentEnergy)
    }

    @Test
    fun `same reel detected repeatedly is counted only once`() {
        val firstCount = energyRepository.onContentScrolled("com.instagram.android", "identical-reel-123")
        assertTrue("First appearance of reel is counted", firstCount)

        val duplicateCount = energyRepository.onContentScrolled("com.instagram.android", "identical-reel-123")
        assertFalse("Duplicate reel detection must be debounced", duplicateCount)

        assertEquals(99, energyRepository.energyState.value.currentEnergy)
        assertEquals(1, energyRepository.energyState.value.reelsToday)
    }

    @Test
    fun `app action resolver correctly maps target packages and calculates relative coordinates`() {
        assertEquals(SupportedApp.INSTAGRAM, actionResolver.resolveTargetApp("com.instagram.android"))
        assertEquals(SupportedApp.YOUTUBE, actionResolver.resolveTargetApp("com.google.android.youtube"))
        assertEquals(SupportedApp.FACEBOOK, actionResolver.resolveTargetApp("com.facebook.katana"))
        assertEquals(SupportedApp.GENERIC, actionResolver.resolveTargetApp("com.other.browser"))

        val screenWidth = 1080
        val screenHeight = 2400

        // ONE FINGER DOWN -> Next Reel swipe (upwards from bottom)
        val nextCoords = actionResolver.resolveSwipe(
            GestureType.ONE_FINGER_DOWN, screenWidth, screenHeight, "com.instagram.android"
        )
        assertNotNull(nextCoords)
        assertTrue("Next reel swipe starts at lower screen", nextCoords!!.startY > nextCoords.endY)
        assertEquals(screenWidth * 0.52f, nextCoords.startX, 1f)

        // TWO FINGERS UP -> Previous Reel swipe (downwards from top)
        val prevCoords = actionResolver.resolveSwipe(
            GestureType.TWO_FINGER_UP, screenWidth, screenHeight, "com.google.android.youtube"
        )
        assertNotNull(prevCoords)
        assertTrue("Previous reel swipe starts at upper screen", prevCoords!!.startY < prevCoords.endY)
    }
}
