package com.abhishek.zerodroid.features.sensors.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.sensors.viewmodel.SensorViewModel
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import java.util.Locale

@Composable
fun SensorScreen(
    viewModel: SensorViewModel = hiltViewModel()
) {
    val isMonitoring by viewModel.isMonitoring.collectAsState()

    HardwareLifecycleEffect(
        isActive = isMonitoring,
        onPause = viewModel::stopSensors,
        onResume = viewModel::startSensors
    )
    val accelerometer by viewModel.accelerometer.collectAsState()
    val gyroscope by viewModel.gyroscope.collectAsState()
    val magnetometer by viewModel.magnetometer.collectAsState()
    val barometer by viewModel.barometer.collectAsState()
    val light by viewModel.light.collectAsState()
    val proximity by viewModel.proximity.collectAsState()
    val metalDetector by viewModel.metalDetectorState.collectAsState()
    val floorState by viewModel.floorState.collectAsState()
    val compassHeading by viewModel.compassHeading.collectAsState()
    val tiltState by viewModel.tiltState.collectAsState()
    val vibrationState by viewModel.vibrationState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Start/Stop control
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = if (isMonitoring) "> 监测中" else "> 待机")
                if (isMonitoring) {
                    OutlinedButton(onClick = { viewModel.toggleMonitoring() }) {
                        Text("停止")
                    }
                } else {
                    Button(
                        onClick = { viewModel.toggleMonitoring() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TerminalGreen.copy(alpha = 0.15f),
                            contentColor = TerminalGreen
                        )
                    ) {
                        Text("开始监测")
                    }
                }
            }
        }

        if (!isMonitoring) {
            item {
                EmptyState(
                    icon = Icons.Default.Sensors,
                    title = "传感器待机",
                    subtitle = "点击“开始监测”读取传感器数据。\n60 秒后自动停止。"
                )
            }
        } else {
            // --- Motion ---
            item { SectionHeader(title = "> 运动") }
            item { SensorCard(reading = accelerometer) }
            item { LevelMeterView(tiltState = tiltState) }
            item { VibrationDetectorView(state = vibrationState, onReset = { viewModel.resetVibrationPeak() }) }
            item { SensorCard(reading = gyroscope) }

            // --- Magnetic ---
            item { SectionHeader(title = "> 磁场") }
            item { SensorCard(reading = magnetometer) }
            item { CompassView(heading = compassHeading) }
            item { MetalDetectorView(state = metalDetector, onReset = { viewModel.resetMetalDetector() }) }

            // --- Environment ---
            item { SectionHeader(title = "> 环境") }
            item {
                TerminalCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "> 气压计",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (barometer.isAvailable) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", floorState.pressureHpa)} hPa · ${String.format(Locale.US, "%.1f", floorState.altitudeM)}m · 楼层 ${floorState.estimatedFloor}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (!barometer.isAvailable) {
                        Text(
                            text = "传感器不可用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { SensorCard(reading = light) }
            item { SensorCard(reading = proximity) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 4.dp)
    )
}
