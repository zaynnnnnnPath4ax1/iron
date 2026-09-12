package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.VoltDarkBackground
import com.example.ui.theme.VoltPurpleAccent
import com.example.ui.theme.VoltPurpleContainer
import com.example.ui.theme.VoltPurpleLight
import com.example.ui.theme.VoltSurface
import com.example.ui.theme.VoltSurfaceBorder
import com.example.ui.theme.VoltSurfaceBorderLight
import com.example.ui.theme.VoltTextMuted
import com.example.ui.theme.VoltTextPrimary
import com.example.ui.theme.VoltTextSecondary
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    LOGS,
    PRIVACY,
    SETTINGS
}

@Composable
fun VoltApp(
    viewModel: VoltViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val alertToast by viewModel.alertToast.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            viewModel.toggleGestureControl(hasCameraPermission = true)
        } else {
            Toast.makeText(context, "Gesture Control needs camera access to detect hands.", Toast.LENGTH_LONG).show()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Processed */ }

    // Request notification permission on Android 13+ if not already granted
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Show alert toast/snackbar when energy warning triggers
    LaunchedEffect(alertToast) {
        alertToast?.let { message ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearAlertToast()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentScreen != Screen.SETTINGS) {
                GeometricBottomNav(
                    currentScreen = currentScreen,
                    onSelectScreen = { currentScreen = it }
                )
            }
        },
        containerColor = VoltDarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                Screen.HOME -> {
                    HomeScreen(
                        uiState = uiState,
                        hasCameraPermission = hasCameraPermission,
                        onRequestCameraPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        onToggleGestureControl = {
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                if (!Settings.canDrawOverlays(context)) {
                                    try {
                                        val overlayIntent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                        context.startActivity(overlayIntent)
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                }
                                viewModel.toggleGestureControl(hasCameraPermission = true)
                            }
                        },
                        onSimulateGesture = { gesture ->
                            viewModel.simulateGesture(gesture)
                        },
                        onRechargeEnergy = {
                            viewModel.rechargeEnergy()
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.SETTINGS
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.LOGS -> {
                    LogsScreen(
                        uiState = uiState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.PRIVACY -> {
                    PrivacyScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                Screen.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        hasCameraPermission = hasCameraPermission,
                        onToggleGestureControl = {
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                viewModel.toggleGestureControl(hasCameraPermission = true)
                            }
                        },
                        onToggleEnergySystem = { enabled ->
                            viewModel.setEnergySystemEnabled(enabled)
                        },
                        onSetSensitivity = { sens ->
                            viewModel.setSensitivity(sens)
                        },
                        onSetDrainRate = { rate ->
                            viewModel.setDrainRate(rate)
                        },
                        onNavigateBack = {
                            currentScreen = Screen.HOME
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Material 3 Geometric Balance Bottom Navigation Bar
 */
@Composable
private fun GeometricBottomNav(
    currentScreen: Screen,
    onSelectScreen: (Screen) -> Unit
) {
    Surface(
        color = Color(0xFF0A0A0A),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(listOf(VoltSurfaceBorder, Color.Transparent))
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Default.Home,
                label = "HOME",
                isSelected = currentScreen == Screen.HOME,
                onClick = { onSelectScreen(Screen.HOME) }
            )
            NavItem(
                icon = Icons.Outlined.History,
                label = "LOGS",
                isSelected = currentScreen == Screen.LOGS,
                onClick = { onSelectScreen(Screen.LOGS) }
            )
            NavItem(
                icon = Icons.Outlined.Shield,
                label = "PRIVACY",
                isSelected = currentScreen == Screen.PRIVACY,
                onClick = { onSelectScreen(Screen.PRIVACY) }
            )
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isSelected) VoltPurpleContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) VoltPurpleAccent else VoltTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = if (isSelected) VoltPurpleAccent else VoltTextMuted
        )
    }
}

@Composable
private fun LogsScreen(
    uiState: VoltUiState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBackground)
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 40.dp)
    ) {
        item {
            Text(
                text = "SCROLLING ACTIVITY & LOGS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = VoltTextMuted
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceBorderLight.copy(alpha = 0.3f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Session Energy Summary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltTextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Current Battery / Core Energy", color = VoltTextSecondary, fontSize = 14.sp)
                        Text("${uiState.energyState.currentEnergy}%", color = VoltPurpleAccent, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Reels Scrolled Today", color = VoltTextSecondary, fontSize = 14.sp)
                        Text("${uiState.energyState.reelsToday}", color = VoltTextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Viewing Duration", color = VoltTextSecondary, fontSize = 14.sp)
                        Text("${uiState.energyState.scrollingMinutesToday} minutes", color = VoltTextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Deduction Rate", color = VoltTextSecondary, fontSize = 14.sp)
                        Text(uiState.drainRate.label, color = VoltPurpleLight, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceBorderLight.copy(alpha = 0.3f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Sensor Efficiency",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = "The front sensor captures downsampled luminance matrices at 320x240, yielding an average frame computation latency of ~12ms with negligible battery consumption.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VoltTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBackground)
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 40.dp)
    ) {
        item {
            Text(
                text = "ZERO-UPLOAD PRIVACY POLICY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = VoltTextMuted
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = VoltSurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceBorderLight.copy(alpha = 0.3f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(VoltPurpleAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = VoltPurpleAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "On-Device Processing Only",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoltTextPrimary
                        )
                    }

                    Text(
                        text = "VOLT operates strictly on your local device. The camera sensor is accessed exclusively in volatile RAM to extract hand silhouettes and calculate fingertip trajectory vectors.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = VoltTextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PrivacyFeature(text = "No images or video frames are saved to disk")
                    PrivacyFeature(text = "No camera stream is uploaded or transmitted")
                    PrivacyFeature(text = "No analytics tracking or personal data collection")
                    PrivacyFeature(text = "Complies fully with Android system camera indicators")
                }
            }
        }
    }
}

@Composable
private fun PrivacyFeature(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircleOutline,
            contentDescription = null,
            tint = VoltPurpleAccent,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = VoltTextPrimary
        )
    }
}
