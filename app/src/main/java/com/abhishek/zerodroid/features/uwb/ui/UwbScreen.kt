package com.abhishek.zerodroid.features.uwb.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.lifecycle.HardwareLifecycleEffect
import com.abhishek.zerodroid.core.permission.PermissionGate
import com.abhishek.zerodroid.core.permission.PermissionUtils
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.uwb.domain.UwbRole
import com.abhishek.zerodroid.features.uwb.domain.UwbState
import com.abhishek.zerodroid.features.uwb.viewmodel.UwbViewModel
import com.abhishek.zerodroid.ui.theme.TerminalRed

@Composable
fun UwbScreen(viewModel: UwbViewModel = hiltViewModel()) {
    PermissionGate(
        permissions = PermissionUtils.uwbPermissions(),
        rationale = "需要 UWB 测距权限才能与附近的 UWB 设备进行测距。"
    ) { UwbContent(viewModel = viewModel) }
}

@Composable
private fun UwbContent(viewModel: UwbViewModel) {
    val state by viewModel.state.collectAsState()
    HardwareLifecycleEffect(isActive = state.isRanging, onPause = viewModel::stopRanging, resumeOnForeground = false)

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(modifier = Modifier.height(4.dp)); StatusIndicator(isAvailable = state.isHardwareAvailable) }
        if (!state.isHardwareAvailable) {
            item { TerminalCard {
                Text(text = "> 未检测到 UWB 硬件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "超宽带（UWB）需要设备提供专用硬件支持（例如 Google Pixel 6 Pro+、Samsung Galaxy S21+）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        state.deviceInfo?.let { info -> item { UwbCapabilitiesCard(info = info) } }
        if (state.isHardwareAvailable) {
            item { TerminalCard {
                Text(text = "> 测距雷达", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp)); UwbRangingView(measurement = state.measurement); Spacer(modifier = Modifier.height(8.dp))
                Text(text = "测距需要另一台同样运行此界面的 UWB 设备。这里不会自动发现设备，请在下方选择角色，然后手动在两台设备之间复制会话参数。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (state.isRanging) RangingActiveContent(state = state, onStop = viewModel::stopRanging) else RoleSetupContent(viewModel = viewModel)
                state.statusMessage?.let { msg -> Spacer(modifier = Modifier.height(8.dp)); Text(text = "> $msg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                state.error?.let { err -> Spacer(modifier = Modifier.height(8.dp)); Text(text = "! $err", style = MaterialTheme.typography.labelSmall, color = TerminalRed) }
            } }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RoleSetupContent(viewModel: UwbViewModel) {
    val state by viewModel.state.collectAsState()
    Text(text = "请选择角色：控制端（创建固定会话并共享参数）或受控端（输入控制端的会话参数）。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = state.peerAddressInput, onValueChange = viewModel::updatePeerAddressInput, label = { Text("对端 UWB 地址（HEX）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = viewModel::startAsController, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("作为控制端启动") } }
    Spacer(modifier = Modifier.height(16.dp))
    Text(text = "受控端：请填写控制端显示的会话参数", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = state.sessionIdInput, onValueChange = viewModel::updateSessionIdInput, label = { Text("会话 ID") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = state.sessionKeyInput, onValueChange = viewModel::updateSessionKeyInput, label = { Text("会话密钥（16 个 HEX 字符）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = state.channelInput, onValueChange = viewModel::updateChannelInput, label = { Text("信道") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
        OutlinedTextField(value = state.preambleInput, onValueChange = viewModel::updatePreambleInput, label = { Text("前导码索引") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(8.dp)); OutlinedButton(onClick = viewModel::startAsControlee) { Text("作为受控端启动") }
}

@Composable
private fun RangingActiveContent(state: UwbState, onStop: () -> Unit) {
    Text(text = "角色：${if (state.role == UwbRole.CONTROLLER) "控制端" else "受控端"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    state.localSession?.let { session ->
        Spacer(modifier = Modifier.height(4.dp))
        if (state.role == UwbRole.CONTROLLER) Text(text = "请将以下参数填入对端的受控端输入框：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("本机地址：${session.localAddressHex}", style = MaterialTheme.typography.labelSmall)
        if (state.role == UwbRole.CONTROLLER) {
            Text("会话 ID：${session.sessionId}", style = MaterialTheme.typography.labelSmall)
            Text("会话密钥：${session.sessionKeyHex}", style = MaterialTheme.typography.labelSmall)
            Text("信道：${session.channel}  前导码：${session.preambleIndex}", style = MaterialTheme.typography.labelSmall)
        }
    }
    state.measurement?.let { m ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "距离：${m.distanceMeters?.let { "%.2f m".format(it) } ?: "--"}" + (m.azimuthDegrees?.let { "  方位角：%.0f°".format(it) } ?: ""), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(modifier = Modifier.height(8.dp)); OutlinedButton(onClick = onStop) { Text("停止测距") }
}
