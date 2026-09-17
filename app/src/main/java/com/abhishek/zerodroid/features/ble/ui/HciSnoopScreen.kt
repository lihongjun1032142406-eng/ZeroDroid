package com.abhishek.zerodroid.features.ble.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.HciPacket
import com.abhishek.zerodroid.features.ble.domain.HciPacketType
import com.abhishek.zerodroid.features.ble.domain.HciSnoopLog
import com.abhishek.zerodroid.features.ble.domain.toHexDump
import com.abhishek.zerodroid.features.ble.viewmodel.HciSnoopViewModel
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import java.util.Locale

@Composable
fun HciSnoopPanel(viewModel: HciSnoopViewModel) {
    val state by viewModel.state.collectAsState()
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.loadFromUri(it) } }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { TerminalCard { Column(modifier = Modifier.padding(12.dp)) {
            Text("BLE HCI Snoop 日志分析器", color = TerminalGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(8.dp))
            Text("1. 进入 设置 > 开发者选项\n2. 开启“蓝牙 HCI 信息收集日志”\n3. 关闭蓝牙后重新开启\n4. 重现 BLE 活动，然后加载日志", color = TerminalGreen.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
        } } }
        item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.loadLog() }, modifier = Modifier.weight(1f), enabled = !state.isLoading) { Text("加载日志", color = TerminalGreen, fontFamily = FontFamily.Monospace) }
            OutlinedButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.weight(1f), enabled = !state.isLoading) { Text("选择文件", color = TerminalCyan, fontFamily = FontFamily.Monospace) }
        } }
        if (state.isLoading) item { Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TerminalGreen, modifier = Modifier.size(32.dp), strokeWidth = 2.dp); Spacer(Modifier.height(8.dp)); Text("正在解析 HCI Snoop 日志…", color = TerminalGreen.copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        } } }
        state.error?.let { error -> item { TerminalCard { Column(modifier = Modifier.padding(12.dp)) { Text("错误", color = TerminalRed, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(4.dp)); Text(error, color = TerminalRed.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp) } } } }
        state.log?.let { log ->
            item { LogInfoCard(log, state.loadedFromPath) }
            item { FilterChipRow(state.filter, { viewModel.setFilter(it) }, log) }
            val packets = state.filter?.let { f -> log.packets.filter { it.packetType == f } } ?: log.packets
            items(packets, key = { it.index }) { PacketCard(it) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LogInfoCard(log: HciSnoopLog, loadedFrom: String?) { TerminalCard { Column(modifier = Modifier.padding(12.dp)) {
    Text("日志信息", color = TerminalAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(6.dp))
    val size = when { log.fileSize < 0 -> "未知"; log.fileSize < 1024 -> "${log.fileSize} B"; log.fileSize < 1024 * 1024 -> "${log.fileSize / 1024} KB"; else -> String.format(Locale.US, "%.1f MB", log.fileSize / (1024.0 * 1024.0)) }
    val cmd = log.packets.count { it.packetType == HciPacketType.Command }; val evt = log.packets.count { it.packetType == HciPacketType.Event }; val acl = log.packets.count { it.packetType == HciPacketType.AclData }
    val info = buildString { loadedFrom?.let { append("来源：$it\n") }; append("数据包：${log.packetCount}  |  大小：$size\n"); append("版本：${log.version}  |  数据链路：${log.datalinkType}\n"); append("CMD：$cmd  |  EVT：$evt  |  ACL：$acl") }
    Text(info, color = TerminalGreen.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
} } }

@Composable
private fun FilterChipRow(currentFilter: HciPacketType?, onFilterSelected: (HciPacketType?) -> Unit, log: HciSnoopLog) {
    val scrollState = rememberScrollState()
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        data class ChipInfo(val label: String, val filter: HciPacketType?, val color: Color, val count: Int)
        val chips = listOf(ChipInfo("全部", null, TerminalGreen, log.packetCount), ChipInfo("CMD", HciPacketType.Command, TerminalAmber, log.packets.count { it.packetType == HciPacketType.Command }), ChipInfo("ACL", HciPacketType.AclData, TerminalCyan, log.packets.count { it.packetType == HciPacketType.AclData }), ChipInfo("EVT", HciPacketType.Event, TerminalAmber, log.packets.count { it.packetType == HciPacketType.Event }))
        chips.forEach { chip -> FilterChip(selected = currentFilter == chip.filter, onClick = { onFilterSelected(chip.filter) }, label = { Text("${chip.label} (${chip.count})", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = if (currentFilter == chip.filter) Color.Black else chip.color) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = chip.color, containerColor = Color.Transparent), border = FilterChipDefaults.filterChipBorder(borderColor = chip.color.copy(alpha = 0.5f), enabled = true, selected = currentFilter == chip.filter)) }
    }
}

@Composable
private fun PacketCard(packet: HciPacket) {
    var expanded by remember { mutableStateOf(false) }
    val accent = when { packet.summary.contains("ATT Error") || packet.summary.startsWith("ATT Error") -> TerminalRed; packet.packetType == HciPacketType.Command || packet.packetType == HciPacketType.Event -> TerminalAmber; packet.isSent -> TerminalCyan; else -> TerminalGreen }
    val arrow = if (packet.isSent) "→" else "←"; val direction = if (packet.isSent) "TX" else "RX"
    val total = packet.timestampMicros / 1_000_000; val millis = (packet.timestampMicros % 1_000_000) / 1_000; val time = String.format(Locale.US, "%02d:%02d:%02d.%03d", (total / 3600) % 24, (total % 3600) / 60, total % 60, millis)
    TerminalCard { Column(modifier = Modifier.clickable { expanded = !expanded }.padding(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("#${packet.index}", color = TerminalGreen.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.width(6.dp)); Text("$arrow $direction", color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.width(6.dp))
            Surface(color = accent.copy(alpha = 0.15f), shape = RoundedCornerShape(3.dp)) { Text(packet.packetType.label, color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)) }
            Spacer(Modifier.weight(1f)); Text(time, color = TerminalGreen.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(4.dp)); Text(packet.summary, color = accent.copy(alpha = 0.9f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = if (expanded) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis, lineHeight = 15.sp)
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) { Column { Spacer(Modifier.height(8.dp)); Text("原始=${packet.originalLength}，包含=${packet.includedLength} 字节", color = TerminalGreen.copy(alpha = 0.4f), fontSize = 9.sp, fontFamily = FontFamily.Monospace); Spacer(Modifier.height(4.dp)); Surface(color = Color.Black.copy(alpha = 0.3f), shape = RoundedCornerShape(4.dp)) { val s = rememberScrollState(); Text(packet.data.toHexDump(), color = TerminalGreen.copy(alpha = 0.7f), fontSize = 9.sp, fontFamily = FontFamily.Monospace, lineHeight = 13.sp, modifier = Modifier.padding(8.dp).horizontalScroll(s)) } } }
    } }
}
