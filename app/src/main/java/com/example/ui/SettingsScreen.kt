package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.SupportedApp
import com.example.data.DrainRate
import com.example.data.Sensitivity
import com.example.ui.theme.VoltDarkBackground
import com.example.ui.theme.VoltPurpleAccent
import com.example.ui.theme.VoltPurpleLight
import com.example.ui.theme.VoltSurface
import com.example.ui.theme.VoltSurfaceBorder
import com.example.ui.theme.VoltSurfaceBorderLight
import com.example.ui.theme.VoltTextMuted
import com.example.ui.theme.VoltTextPrimary
import com.example.ui.theme.VoltTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: VoltUiState,
    hasCameraPermission: Boolean,
    onToggleGestureControl: () -> Unit,
    onToggleEnergySystem: (Boolean) -> Unit,
    onSetSensitivity: (Sensitivity) -> Unit,
    onSetDrainRate: (DrainRate) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VoltDarkBackground)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = VoltTextPrimary
                )
            },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VoltSurface)
                        .border(1.dp, VoltSurfaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = VoltTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = VoltDarkBackground
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TOGGLE: Gesture Control
            item {
                GeometricSettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gesture Control",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Invisible air-gesture recognition for scrolling",
                                fontSize = 13.sp,
                                color = VoltTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isGestureControlActive,
                            onCheckedChange = { onToggleGestureControl() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = VoltPurpleAccent,
                                uncheckedTrackColor = VoltSurfaceBorder
                            ),
                            modifier = Modifier.testTag("settings_gesture_switch")
                        )
                    }
                }
            }

            // TOGGLE: Energy System
            item {
                GeometricSettingsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Energy System",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Tracks scrolling habits and nudges healthy limits",
                                fontSize = 13.sp,
                                color = VoltTextSecondary
                            )
                        }
                        Switch(
                            checked = uiState.isEnergySystemEnabled,
                            onCheckedChange = onToggleEnergySystem,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = VoltPurpleAccent,
                                uncheckedTrackColor = VoltSurfaceBorder
                            ),
                            modifier = Modifier.testTag("settings_energy_switch")
                        )
                    }
                }
            }

            // GESTURE SENSITIVITY
            item {
                GeometricSettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Gesture Sensitivity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoltTextPrimary
                        )
                        Text(
                            text = "Adjust the required hand displacement distance",
                            fontSize = 13.sp,
                            color = VoltTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Sensitivity.entries.forEach { sens ->
                                val selected = uiState.sensitivity == sens
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSetSensitivity(sens) },
                                    label = { Text(sens.label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VoltPurpleAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = VoltPurpleAccent
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = if (selected) VoltPurpleAccent else VoltSurfaceBorder
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ENERGY DRAIN RATE
            item {
                GeometricSettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Energy Drain",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoltTextPrimary
                        )
                        Text(
                            text = "Rate of energy deduction per detected Reel or Short",
                            fontSize = 13.sp,
                            color = VoltTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DrainRate.entries.forEach { rate ->
                                val selected = uiState.drainRate == rate
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSetDrainRate(rate) },
                                    label = { Text(rate.label) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VoltPurpleAccent.copy(alpha = 0.2f),
                                        selectedLabelColor = VoltPurpleAccent
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = if (selected) VoltPurpleAccent else VoltSurfaceBorder
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // SUPPORTED APPS
            item {
                GeometricSettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Supported Apps",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VoltTextPrimary
                        )
                        Text(
                            text = "Optimized swipe trajectories and Reel/Short detection:",
                            fontSize = 13.sp,
                            color = VoltTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SupportedApp.entries.forEach { app ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(VoltPurpleAccent.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = VoltPurpleAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = app.appName,
                                    fontSize = 14.sp,
                                    color = VoltTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // PRIVACY
            item {
                GeometricSettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = VoltPurpleAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy & Local Processing",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary
                            )
                        }
                        Text(
                            text = "Camera is used only to detect hand gestures. No camera images or video are recorded or uploaded. Processing runs 100% locally on device.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = VoltTextSecondary
                        )
                    }
                }
            }

            // ABOUT VOLT & ANDROID ARCHITECTURE
            item {
                GeometricSettingsCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = VoltTextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About VOLT",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = VoltTextPrimary
                            )
                        }
                        Text(
                            text = "VOLT Version 1.0.0 • Geometric Balance Edition\n" +
                                    "Built with CameraX invisible foreground analysis, dynamic accessibility swipe injection, and an on-device state machine. Complies with all Android security disclosures.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = VoltTextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GeometricSettingsCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = VoltSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(VoltSurfaceBorder, VoltSurfaceBorderLight.copy(alpha = 0.3f)))
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}
