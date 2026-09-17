package com.abhishek.zerodroid.features.ble.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
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
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.viewmodel.BleViewModel

@Composable
fun BleScreen(viewModel: BleViewModel = hiltViewModel()) {
    PermissionGate(permissions = PermissionUtils.blePermissions(), rationale = "需要蓝牙权限才能扫描附近的 BLE 设备。") {
        BleContent(viewModel = viewModel)
    }
}

@Composable
private fun BleContent(viewModel: BleViewModel) {
    val scanState by viewModel.scanState.collectAsState()
    HardwareLifecycleEffect(isActive = scanState.isScanning, onPause = viewModel::stopScan, onResume = viewModel::startScan)
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (scanState.isScanning) ScanningIndicator(isScanning = true, label = "已发现 ${scanState.devices.size} 个设备")
                else Text(text = "> 已发现 ${scanState.devices.size} 个设备", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                if (scanState.isScanning) {
                    OutlinedButton(onClick = { viewModel.toggleScan() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("停止") }
                } else {
                    Button(onClick = { viewModel.toggleScan() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("扫描") }
                }
            }
        }
        scanState.error?.let { error -> item { Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) } }
        if (!scanState.isBluetoothEnabled) {
            item {
                TerminalCard {
                    Text(text = "> 蓝牙已关闭", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "请在系统设置中开启蓝牙，然后再次点击“扫描”。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (scanState.devices.isEmpty() && !scanState.isScanning) {
            item { EmptyState(icon = Icons.Default.Bluetooth, title = "未发现 BLE 设备", subtitle = "点击“扫描”搜索附近的低功耗蓝牙设备") }
        }
        items(scanState.devices, key = { it.address }) { device -> BleDeviceItem(device = device, onBookmarkToggle = { viewModel.toggleBookmark(device) }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
