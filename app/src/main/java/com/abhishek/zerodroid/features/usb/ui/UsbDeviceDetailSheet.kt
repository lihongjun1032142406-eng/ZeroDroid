package com.abhishek.zerodroid.features.usb.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.features.usb.domain.UsbDeviceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsbDeviceDetailSheet(device: UsbDeviceInfo, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(text = device.productName ?: device.deviceName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow("VID:PID", device.vidPid)
            DetailRow("设备类别", device.deviceClassName)
            DetailRow("子类别", device.deviceSubclass.toString())
            device.manufacturerName?.let { DetailRow("制造商", it) }
            device.productName?.let { DetailRow("产品", it) }
            DetailRow("接口数量", device.interfaceCount.toString())
            Spacer(modifier = Modifier.height(12.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outline); Spacer(modifier = Modifier.height(12.dp))
            Text(text = "> 接口树", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            device.interfaces.forEach { iface ->
                Text(text = "接口 #${iface.id}：${iface.className}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                Text(text = "类别：${iface.interfaceClass} | 子类：${iface.interfaceSubclass} | 协议：${iface.interfaceProtocol}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                iface.endpoints.forEach { ep ->
                    Text(text = "EP 0x%02X [${ep.direction}] ${ep.type}（${ep.maxPacketSize} 字节）".format(ep.address), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 24.dp))
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Text(text = "$label：$value", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
