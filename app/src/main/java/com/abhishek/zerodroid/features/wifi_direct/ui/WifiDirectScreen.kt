package com.abhishek.zerodroid.features.wifi_direct.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectGroup
import com.abhishek.zerodroid.features.wifi_direct.domain.WifiDirectPeer
import com.abhishek.zerodroid.features.wifi_direct.viewmodel.WifiDirectViewModel
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun WifiDirectScreen(viewModel: WifiDirectViewModel = hiltViewModel()) {
    PermissionGate(
        permissions = PermissionUtils.wifiDirectPermissions(),
        rationale = "Wi-Fi Direct 需要附近设备和位置权限才能发现并连接对等设备。"
    ) { WifiDirectContent(viewModel = viewModel) }
}

@Composable
private fun WifiDirectContent(viewModel: WifiDirectViewModel) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.initialize() }
    HardwareLifecycleEffect(isActive = state.isDiscovering, onPause = viewModel::stopDiscovery, onResume = viewModel::startDiscovery)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "> Wi-Fi Direct", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (state.isDiscovering) {
                    OutlinedButton(onClick = { viewModel.stopDiscovery() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("停止") }
                } else {
                    Button(onClick = { viewModel.startDiscovery() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("发现设备") }
                }
            }
        }
        item { WifiDirectStatusCard(isEnabled = state.isEnabled, peerCount = state.peers.size, isDiscovering = state.isDiscovering) }
        state.error?.let { error -> item { Text(text = error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 4.dp)) } }
        item { ScanningIndicator(isScanning = state.isDiscovering, label = "正在发现对等设备...") }
        state.connectedGroup?.let { group -> item { ConnectedGroupCard(group = group, onDisconnect = { viewModel.disconnect() }) } }
        if (state.peers.isEmpty() && !state.isDiscovering) {
            item { EmptyState(icon = Icons.Default.WifiFind, title = "未发现对等设备", subtitle = "点击“发现设备”搜索附近的 Wi-Fi Direct 设备") }
        }
        items(state.peers, key = { it.deviceAddress }) { peer -> PeerItem(peer = peer, onConnect = { viewModel.connect(peer.deviceAddress) }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun WifiDirectStatusCard(isEnabled: Boolean, peerCount: Int, isDiscovering: Boolean) {
    TerminalCard {
        Text(text = "> 状态", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Wi-Fi Direct", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = if (isEnabled) "已启用" else "已禁用", style = MaterialTheme.typography.bodySmall, color = if (isEnabled) TerminalGreen else TerminalRed)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "已发现对等设备", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "$peerCount", style = MaterialTheme.typography.bodySmall, color = TerminalCyan)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "设备发现", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = if (isDiscovering) "进行中" else "空闲", style = MaterialTheme.typography.bodySmall, color = if (isDiscovering) TerminalAmber else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectedGroupCard(group: WifiDirectGroup, onDisconnect: () -> Unit) {
    TerminalCard(animated = true) {
        Text(text = "> 已连接群组", style = MaterialTheme.typography.titleMedium, color = TerminalGreen)
        Spacer(modifier = Modifier.height(8.dp))
        LabelValue(label = "网络", value = group.networkName)
        Spacer(modifier = Modifier.height(4.dp))
        group.passphrase?.let { passphrase -> LabelValue(label = "密码", value = passphrase); Spacer(modifier = Modifier.height(4.dp)) }
        LabelValue(label = "角色", value = if (group.isGroupOwner) "群组所有者" else "客户端", valueColor = if (group.isGroupOwner) TerminalAmber else TerminalCyan)
        Spacer(modifier = Modifier.height(4.dp))
        group.ownerAddress?.let { address -> LabelValue(label = "所有者", value = address); Spacer(modifier = Modifier.height(4.dp)) }
        if (group.clients.isNotEmpty()) {
            Text(text = "客户端（${group.clients.size}）：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            group.clients.forEach { client -> Text(text = "  - $client", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = TerminalCyan) }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onDisconnect, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("断开连接") }
    }
}

@Composable
private fun PeerItem(peer: WifiDirectPeer, onConnect: () -> Unit) {
    TerminalCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = peer.deviceName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = peer.deviceAddress, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = peer.statusLabel, style = MaterialTheme.typography.labelSmall, color = when (peer.status) { 0 -> TerminalGreen; 1 -> TerminalAmber; 2 -> TerminalRed; 3 -> TerminalCyan; else -> MaterialTheme.colorScheme.onSurfaceVariant })
                    if (peer.isGroupOwner) { Spacer(modifier = Modifier.width(8.dp)); Text(text = "[GO]", style = MaterialTheme.typography.labelSmall, color = TerminalAmber) }
                }
            }
            if (peer.status != 0) {
                Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("连接") }
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TerminalGreen) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = valueColor)
    }
}
