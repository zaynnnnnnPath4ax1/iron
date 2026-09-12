package com.example.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cv.GestureType
import com.example.ui.theme.VoltAmber
import com.example.ui.theme.VoltDarkBackground
import com.example.ui.theme.VoltGreenGlow
import com.example.ui.theme.VoltGreenStatus
import com.example.ui.theme.VoltPurpleAccent
import com.example.ui.theme.VoltPurpleGlow
import com.example.ui.theme.VoltPurpleLight
import com.example.ui.theme.VoltRed
import com.example.ui.theme.VoltSurface
import com.example.ui.theme.VoltSurfaceBorder
import com.example.ui.theme.VoltSurfaceBorderLight
import com.example.ui.theme.VoltSurfaceVariant
import com.example.ui.theme.VoltTextMuted
import com.example.ui.theme.VoltTextPrimary
import com.example.ui.theme.VoltTextSecondary
import com.example.ui.theme.VoltTrackDark

@Composable
fun HomeScreen(
    uiState: VoltUiState,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onToggleGestureControl: () -> Unit,
    onSimulateGesture: (GestureType) -> Unit,
    onRechargeEnergy: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBackground)
            .padding(horizontal = 22.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP APP BAR (Geometric Balance Header)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Geometric "V" Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(10.dp),
                                spotColor = VoltPurpleAccent,
                                ambientColor = VoltPurpleAccent
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .background(VoltPurpleAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }

                    Text(
                        text = "VOLT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = VoltTextPrimary
                    )
                }

                // Rounded Settings Icon Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(VoltSurface)
                        .border(1.dp, VoltSurfaceBorder, CircleShape)
                        .clickable { onNavigateToSettings() }
                        .testTag("settings_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = VoltTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ENERGY WARNING BANNER (if applicable)
        if (uiState.energyState.warningMessage != null && uiState.isEnergySystemEnabled) {
            item {
                EnergyWarningBanner(
                    message = uiState.energyState.warningMessage,
                    energy = uiState.energyState.currentEnergy,
                    onRecharge = onRechargeEnergy
                )
            }
        }

        // GEOMETRIC BALANCE ENERGY CORE
        item {
            GeometricEnergyCore(
                energy = uiState.energyState.currentEnergy,
                isActive = uiState.isGestureControlActive,
                onRecharge = onRechargeEnergy
            )
        }

        // QUICK STATS CARDS (Geometric 2-Column Grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GeometricStatCard(
                    modifier = Modifier.weight(1f),
                    label = "REELS TODAY",
                    value = "${uiState.energyState.reelsToday}"
                )
                GeometricStatCard(
                    modifier = Modifier.weight(1f),
                    label = "ACTIVE TIME",
                    value = "${uiState.energyState.scrollingMinutesToday}",
                    unit = "min"
                )
            }
        }

        // STATUS INDICATORS ROW
        item {
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GeometricStatusIndicator(
                    label = if (uiState.isGestureControlActive) "Camera Active" else "Camera Idle",
                    isActive = uiState.isGestureControlActive
                )
                GeometricStatusIndicator(
                    label = if (uiState.isGestureControlActive) "Gestures ON" else "Gestures OFF",
                    isActive = uiState.isGestureControlActive
                )
            }
        }

        // PRIMARY CONTROL TOGGLE BUTTON
        item {
            val isRunning = uiState.isGestureControlActive
            Button(
                onClick = {
                    if (!hasCameraPermission) {
                        onRequestCameraPermission()
                    } else {
                        onToggleGestureControl()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(
                        elevation = if (isRunning) 14.dp else 18.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = if (isRunning) VoltRed.copy(alpha = 0.5f) else VoltPurpleGlow,
                        ambientColor = if (isRunning) VoltRed.copy(alpha = 0.3f) else VoltPurpleGlow
                    )
                    .testTag("toggle_gesture_control_button"),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) VoltRed else VoltPurpleAccent,
                    contentColor = Color.Black
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isRunning) "STOP GESTURE CONTROL" else "GESTURE CONTROL ON",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = Color.Black
                    )
                }
            }
        }

        // FLOATING OVERLAY PREVIEW CHIP
        item {
            GeometricOverlayPreview(
                energy = uiState.energyState.currentEnergy
            )
        }

        // SYSTEM PERMISSION & SERVICE STATUS
        item {
            GeometricStatusCard(
                uiState = uiState,
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = onRequestCameraPermission
            )
        }

        // ONLY TWO GESTURES GUIDE
        item {
            GeometricGestureGuideCard()
        }

        // LIVE CALIBRATION & SIMULATION
        item {
            GeometricCalibrationCard(
                lastGesture = uiState.lastExecutedGesture,
                detection = uiState.latestDetection,
                isControlActive = uiState.isGestureControlActive,
                onSimulateGesture = onSimulateGesture
            )
        }
    }
}

/**
 * The signature Geometric Balance Energy Core
 * Features concentric geometric rings, dashed orbital track, and glowing violet energy gauge.
 */
@Composable
private fun GeometricEnergyCore(
    energy: Int,
    isActive: Boolean,
    onRecharge: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (energy / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700),
        label = "energy_core_progress"
    )

    val dialColor by animateColorAsState(
        targetValue = when {
            energy <= 10 -> VoltRed
            energy <= 30 -> VoltAmber
            else -> VoltPurpleAccent
        },
        label = "energy_color"
    )

    // Slow atmospheric rotation for inner decorative dashed ring
    val infiniteTransition = rememberInfiniteTransition(label = "ring_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Solid Ring Decor (Geometric Balance)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF0F172A).copy(alpha = 0.6f),
                radius = size.minDimension / 2 - 2.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Middle Dashed Geometric Ring Decor
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .rotate(if (isActive) rotationAngle else 0f)
        ) {
            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
            drawCircle(
                color = Color(0xFF1E293B),
                radius = size.minDimension / 2 - 2.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashPathEffect)
            )
        }

        // Inner Progress Arc & Track
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp)
        ) {
            val strokeWidth = 8.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth / 2

            // Dark track
            drawCircle(
                color = VoltTrackDark,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Active Sweep Arc
            drawArc(
                color = dialColor,
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Center Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = dialColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ENERGY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = dialColor
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$energy%",
                fontSize = 54.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                color = VoltTextPrimary
            )

            if (energy < 100) {
                Text(
                    text = "Recharge",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = VoltPurpleLight,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onRecharge() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GeometricStatCard(
    label: String,
    value: String,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceBorderLight.copy(alpha = 0.4f)))
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
                color = VoltTextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VoltTextPrimary
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = VoltTextSecondary,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GeometricStatusIndicator(
    label: String,
    isActive: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .shadow(
                    elevation = if (isActive) 8.dp else 0.dp,
                    shape = CircleShape,
                    spotColor = VoltGreenGlow,
                    ambientColor = VoltGreenGlow
                )
                .clip(CircleShape)
                .background(if (isActive) VoltGreenStatus else VoltTextMuted)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isActive) VoltTextSecondary else VoltTextMuted
        )
    }
}

@Composable
private fun GeometricOverlayPreview(
    energy: Int
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = VoltSurface.copy(alpha = 0.85f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(VoltSurfaceBorderLight, VoltSurfaceBorder))
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "OVERLAY PREVIEW",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = VoltTextMuted
            )

            // Floating Pill representation
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(listOf(VoltPurpleAccent.copy(alpha = 0.8f), VoltSurfaceBorder))
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚡",
                        fontSize = 11.sp,
                        color = VoltPurpleAccent
                    )
                    Text(
                        text = "$energy%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun GeometricStatusCard(
    uiState: VoltUiState,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceVariant))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SYSTEM STATUS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = VoltTextMuted
            )

            GeometricStatusRow(
                title = "Camera sensor",
                status = if (uiState.isGestureControlActive) "Active (Invisible)" else "Idle",
                isPositive = uiState.isGestureControlActive,
                actionLabel = if (!hasCameraPermission) "Grant" else null,
                onAction = onRequestCameraPermission
            )

            GeometricStatusRow(
                title = "Gesture control",
                status = if (uiState.isGestureControlActive) "Running" else "Inactive",
                isPositive = uiState.isGestureControlActive,
                actionLabel = null,
                onAction = {}
            )

            GeometricStatusRow(
                title = "Accessibility service",
                status = if (uiState.isAccessibilityConnected) "Connected (Global)" else "Disabled",
                isPositive = uiState.isAccessibilityConnected,
                actionLabel = if (!uiState.isAccessibilityConnected) "Enable" else null,
                onAction = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            )

            if (!uiState.currentForegroundPackage.isNullOrEmpty()) {
                val appName = when {
                    uiState.currentForegroundPackage.contains("instagram") -> "Instagram"
                    uiState.currentForegroundPackage.contains("youtube") -> "YouTube Shorts"
                    uiState.currentForegroundPackage.contains("facebook") -> "Facebook Reels"
                    else -> "Android Foreground App"
                }
                Text(
                    text = "Active app: $appName",
                    fontSize = 12.sp,
                    color = VoltPurpleLight
                )
            }
        }
    }
}

@Composable
private fun GeometricStatusRow(
    title: String,
    status: String,
    isPositive: Boolean,
    actionLabel: String?,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) VoltGreenStatus else VoltTextMuted)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = VoltTextSecondary
            )
        }

        if (actionLabel != null) {
            Text(
                text = actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VoltPurpleAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onAction() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (isPositive) VoltGreenStatus else VoltTextMuted
            )
        }
    }
}

@Composable
private fun GeometricGestureGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceVariant))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ONLY TWO GESTURES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = VoltTextMuted
            )

            // Gesture 1
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(VoltPurpleAccent.copy(alpha = 0.15f))
                        .border(1.dp, VoltPurpleAccent.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        tint = VoltPurpleAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "1 Finger DOWN",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = "Moves to NEXT Reel / Short",
                        fontSize = 12.sp,
                        color = VoltTextSecondary
                    )
                }
            }

            // Gesture 2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(VoltPurpleLight.copy(alpha = 0.15f))
                        .border(1.dp, VoltPurpleLight.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = VoltPurpleLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "2 Fingers UP",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = "Returns to PREVIOUS Reel / Short",
                        fontSize = 12.sp,
                        color = VoltTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun GeometricCalibrationCard(
    lastGesture: GestureType?,
    detection: com.example.cv.HandDetectionResult?,
    isControlActive: Boolean,
    onSimulateGesture: (GestureType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceVariant))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SENSOR CALIBRATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = VoltTextMuted
                )
                if (isControlActive && detection != null) {
                    Text(
                        text = if (detection.isHandPresent) "${detection.fingerCount} finger(s)" else "Waiting...",
                        fontSize = 11.sp,
                        color = if (detection.isHandPresent) VoltGreenStatus else VoltTextMuted
                    )
                }
            }

            if (lastGesture != null && lastGesture != GestureType.NONE) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VoltPurpleAccent.copy(alpha = 0.18f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(VoltPurpleAccent.copy(alpha = 0.5f), Color.Transparent))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = VoltPurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Last triggered: ${lastGesture.displayName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = VoltPurpleLight
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulateGesture(GestureType.ONE_FINGER_DOWN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_one_finger_down_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = Brush.linearGradient(listOf(VoltSurfaceBorderLight, VoltSurfaceBorder))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        tint = VoltPurpleAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Next Reel", fontSize = 13.sp, color = VoltTextPrimary)
                }

                OutlinedButton(
                    onClick = { onSimulateGesture(GestureType.TWO_FINGER_UP) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_two_fingers_up_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(
                        brush = Brush.linearGradient(listOf(VoltSurfaceBorderLight, VoltSurfaceBorder))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = VoltPurpleLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Previous", fontSize = 13.sp, color = VoltTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun EnergyWarningBanner(
    message: String,
    energy: Int,
    onRecharge: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (energy <= 10) VoltRed.copy(alpha = 0.2f) else VoltAmber.copy(alpha = 0.2f),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(if (energy <= 10) VoltRed else VoltAmber, Color.Transparent)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (energy <= 10) VoltRed else VoltAmber,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = message,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = VoltTextPrimary
                    )
                    Text(
                        text = "Short breaks recharge energy automatically",
                        fontSize = 11.sp,
                        color = VoltTextSecondary
                    )
                }
            }

            IconButton(onClick = onRecharge) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recharge",
                    tint = VoltAmber
                )
            }
        }
    }
}
