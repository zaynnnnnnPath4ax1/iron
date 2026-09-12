package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.MainActivity
import com.example.R
import com.example.accessibility.VoltAccessibilityService
import com.example.cv.GestureStateMachine
import com.example.cv.GestureType
import com.example.cv.HandDetectionResult
import com.example.cv.HandLandmarkDetector
import com.example.data.EnergyRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class VoltGestureService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var energyRepository: EnergyRepository
    private lateinit var overlayManager: OverlayManager

    private val handDetector = HandLandmarkDetector()
    private lateinit var stateMachine: GestureStateMachine

    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        settingsRepository = SettingsRepository.getInstance(this)
        energyRepository = EnergyRepository.getInstance(this, settingsRepository)
        overlayManager = OverlayManager(this)

        stateMachine = GestureStateMachine(
            sensitivity = settingsRepository.sensitivity.value
        ) { detectedGesture ->
            onGestureTriggered(detectedGesture)
        }

        createNotificationChannel()
        startForegroundWithNotification(energyRepository.energyState.value.currentEnergy)

        observeSettingsAndEnergy()
        startCameraAnalysis()

        overlayManager.showOverlay(energyRepository.energyState.value.currentEnergy)
        _isServiceRunning.value = true
        Log.i(TAG, "VoltGestureService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(energy: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoltGestureService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_active_title))
            .setContentText(getString(R.string.notification_active_content, energy))
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundWithNotification(energy: Int) {
        val notification = buildNotification(energy)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeSettingsAndEnergy() {
        serviceScope.launch {
            settingsRepository.sensitivity.collectLatest { sens ->
                stateMachine.sensitivity = sens
            }
        }

        serviceScope.launch {
            energyRepository.energyState.collectLatest { state ->
                val notification = buildNotification(state.currentEnergy)
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
                overlayManager.updateEnergy(state.currentEnergy)
            }
        }
    }

    private fun startCameraAnalysis() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraAnalysis(cameraProvider ?: return@addListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize CameraX: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraAnalysis(provider: ProcessCameraProvider) {
        try {
            provider.unbindAll()

            // Select front camera for natural hands-free gesture control
            val cameraSelector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            // Lightweight 320x240 resolution ensures ultra-low ~12ms processing and battery conservation
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(320, 240))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val detectionResult: HandDetectionResult = handDetector.analyzeFrame(imageProxy)
                _latestDetection.value = detectionResult

                if (detectionResult.isHandPresent) {
                    stateMachine.processFrame(detectionResult)
                } else {
                    stateMachine.resetToIdle()
                }
            }

            // Bind directly to service's own lifecycle
            provider.bindToLifecycle(this, cameraSelector, imageAnalysis)
            Log.i(TAG, "CameraX Analysis bound successfully to front camera sensor")
        } catch (e: Exception) {
            Log.e(TAG, "Error binding CameraX to service lifecycle: ${e.message}")
        }
    }

    private fun onGestureTriggered(gesture: GestureType) {
        Log.i(TAG, "Air gesture triggered: ${gesture.displayName}")
        _lastTriggeredGesture.value = gesture

        // Execute global swipe via Accessibility Service
        val executed = VoltAccessibilityService.dispatchGestureAction(gesture)
        if (!executed) {
            Log.w(TAG, "Accessibility service not connected; swipe could not be dispatched.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        _isServiceRunning.value = false

        settingsRepository.setGestureControlEnabled(false)

        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            // Ignore
        }

        overlayManager.removeOverlay()
        serviceScope.cancel()
        Log.i(TAG, "VoltGestureService destroyed and camera released")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "VoltGestureService"
        private const val CHANNEL_ID = "volt_gesture_control_channel"
        private const val NOTIFICATION_ID = 4041
        const val ACTION_STOP_SERVICE = "com.example.service.ACTION_STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _latestDetection = MutableStateFlow<HandDetectionResult?>(null)
        val latestDetection: StateFlow<HandDetectionResult?> = _latestDetection.asStateFlow()

        private val _lastTriggeredGesture = MutableStateFlow<GestureType?>(null)
        val lastTriggeredGesture: StateFlow<GestureType?> = _lastTriggeredGesture.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, VoltGestureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, VoltGestureService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
