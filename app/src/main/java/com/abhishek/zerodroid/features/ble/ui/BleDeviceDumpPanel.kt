package com.abhishek.zerodroid.features.ble.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.*
import com.abhishek.zerodroid.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DUMPS_DIR = "ble_dumps"

private fun getDumpsDir(context: Context): File {
    val dir = File(context.filesDir, DUMPS_DIR)
    if (!dir.exists()) dir.mkdirs()
    return dir
}

private fun saveDump(context: Context, dump: BleDeviceDump): File {
    val dir = getDumpsDir(context)
    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    val name = "${dump.deviceName ?: dump.deviceAddress}_${sdf.format(Date(dump.timestamp))}.json"
    val sanitized = name.replace(Regex("[^a-zA-Z0-9._\\-]"), "_")
    return File(dir, sanitized).also { it.writeText(dump.toJson().toString(2)) }
}

private fun loadSavedDumps(context: Context): List<Pair<String, BleDeviceDump>> {
    val dir = getDumpsDir(context)
    if (!dir.exists()) return emptyList()
    return dir.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() }?.mapNotNull { file ->
        try { file.name to BleDeviceDump.fromJson(JSONObject(file.readText())) } catch (_: Exception) { null }
    } ?: emptyList()
}

private fun deleteDump(context: Context, fileName: String) {
    File(getDumpsDir(context), fileName).let { if (it.exists()) it.delete() }
}

@Composable
fun BleDeviceDumpPanel(explorer: GattExplorer, connectionState: GattConnectionState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dumper = remember(explorer) { BleDeviceDumper(explorer) }
    val isConnected = connectionState.isConnected
    var isDumping by remember { mutableStateOf(false) }
    var dumpProgress by remember { mutableStateOf<BleDeviceDumper.DumpProgress?>(null) }
    var currentDump by remember { mutableStateOf<BleDeviceDump?>(null) }
    var isReplaying by remember { mutableStateOf(false) }
    var replayProgress by remember { mutableStateOf<BleDeviceDumper.DumpProgress?>(null) }
    val savedDumps = remember { mutableStateListOf<Pair<String, BleDeviceDump>>() }
    var showSaved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { loadSavedDumps(context) }.let { savedDumps.clear(); savedDumps.addAll(it) }
    }

    fun replay(dump: BleDeviceDump) {
        scope.launch {
            isReplaying = true
            replayProgress = null
            dumper.replayWrites(dump) { replayProgress = it }
            isReplaying = false
            Toast.makeText(context, "重放完成", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TerminalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("> 设备转储", color = TerminalCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            isDumping = true; dumpProgress = null
                            val result = dumper.dumpDevice { dumpProgress = it }
                            currentDump = result; isDumping = false
                            if (result != null) {
                                withContext(Dispatchers.IO) { saveDump(context, result) }
                                val refreshed = withContext(Dispatchers.IO) { loadSavedDumps(context) }
                                savedDumps.clear(); savedDumps.addAll(refreshed)
                            }
                        }
                    }, enabled = isConnected && !isDumping && !isReplaying, colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen.copy(alpha = 0.2f), contentColor = TerminalGreen)) {
                        Icon(Icons.Default.FileDownload, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("全部转储", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { showSaved = !showSaved }, colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalAmber)) {
                        Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("已保存（${savedDumps.size}）", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
                if (isDumping && dumpProgress != null) {
                    val p = dumpProgress!!; Spacer(Modifier.height(8.dp))
                    Text("正在读取 ${p.current}/${p.total}：${p.currentChar}", color = TerminalAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp)); LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth(), color = TerminalGreen, trackColor = TerminalGreen.copy(alpha = 0.1f))
                }
                if (isReplaying && replayProgress != null) {
                    val p = replayProgress!!; Spacer(Modifier.height(8.dp))
                    Text("正在写入 ${p.current}/${p.total}：${p.currentChar}", color = TerminalAmber, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp)); LinearProgressIndicator(progress = { p.fraction }, modifier = Modifier.fillMaxWidth(), color = TerminalCyan, trackColor = TerminalCyan.copy(alpha = 0.1f))
                }
            }
        }

        currentDump?.let { dump ->
            DumpResultCard(dump, isConnected, isReplaying, onCopyJson = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("BLE 设备转储", dump.toJson().toString(2)))
                Toast.makeText(context, "转储 JSON 已复制到剪贴板", Toast.LENGTH_SHORT).show()
            }, onReplay = { replay(dump) })
        }

        AnimatedVisibility(showSaved) {
            TerminalCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("> 已保存的转储", color = TerminalAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    if (savedDumps.isEmpty()) Text("未找到已保存的转储。", color = TerminalAmber.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    else savedDumps.forEach { (fileName, dump) ->
                        SavedDumpRow(fileName, dump, isConnected, isReplaying, { currentDump = dump }, { replay(dump) }, { deleteDump(context, fileName); savedDumps.removeAll { it.first == fileName } })
                        HorizontalDivider(color = TerminalGreen.copy(alpha = 0.1f), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DumpResultCard(dump: BleDeviceDump, isConnected: Boolean, isReplaying: Boolean, onCopyJson: () -> Unit, onReplay: () -> Unit) {
    TerminalCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp).animateContentSize()) {
            Text("> 转储结果", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            val writableCount = dump.services.sumOf { svc -> svc.characteristics.count { it.isReplayable } }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SummaryLine("设备", dump.deviceName ?: "未知"); SummaryLine("地址", dump.deviceAddress); SummaryLine("时间", dump.formattedTimestamp); SummaryLine("MTU", dump.mtu.toString()); SummaryLine("服务", dump.services.size.toString()); SummaryLine("已读特征", "${dump.successfulReads} 成功 / ${dump.failedReads} 失败"); SummaryLine("可写", "$writableCount 个可重放")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCopyJson, colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan)) { Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("复制 JSON", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                Button(onClick = onReplay, enabled = isConnected && !isReplaying && writableCount > 0, colors = ButtonDefaults.buttonColors(containerColor = TerminalCyan.copy(alpha = 0.2f), contentColor = TerminalCyan)) { Icon(Icons.Default.Replay, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("重放写入", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
            }
            Spacer(Modifier.height(12.dp)); dump.services.forEach { ExpandableServiceSection(it); Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun ExpandableServiceSection(service: DumpedService) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TerminalCyan, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
            Text(service.displayName.ifBlank { service.uuid }, color = TerminalCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${service.characteristics.size} 个特征", color = TerminalGreen.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        AnimatedVisibility(expanded) { Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { service.characteristics.forEach { CharacteristicDumpRow(it) } } }
    }
}

@Composable
private fun CharacteristicDumpRow(char: DumpedCharacteristic) {
    Column(Modifier.fillMaxWidth()) {
        Text(char.displayName.ifBlank { char.uuid }, color = if (char.value != null) TerminalGreen else if (char.readError != null) TerminalRed else TerminalAmber.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (char.value != null) {
            Text("HEX: ${char.hexString}", color = TerminalGreen.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val ascii = char.value.map { b -> val c = b.toInt().toChar(); if (c.isLetterOrDigit() || c.isWhitespace() || c in "!@#\$%^&*()-_=+[]{}|;:',.<>?/`~\"\\") c else '.' }.joinToString("")
            Text("ASCII: $ascii", color = TerminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else if (char.readError != null) Text("错误：${char.readError}", color = TerminalRed.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        else Text("不可读取", color = TerminalAmber.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        val props = buildList { if ((char.properties and 0x02) != 0) add("R"); if ((char.properties and 0x04) != 0) add("W"); if ((char.properties and 0x08) != 0) add("WNR"); if ((char.properties and 0x10) != 0) add("N"); if ((char.properties and 0x20) != 0) add("I") }
        if (props.isNotEmpty()) Text(props.joinToString(" | "), color = TerminalCyan.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 9.sp)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) { Row { Text("$label：", color = TerminalGreen.copy(alpha = 0.6f), fontFamily = FontFamily.Monospace, fontSize = 11.sp); Text(value, color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }

@Composable
private fun SavedDumpRow(fileName: String, dump: BleDeviceDump, isConnected: Boolean, isReplaying: Boolean, onLoad: () -> Unit, onReplay: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(dump.deviceName ?: dump.deviceAddress, color = TerminalGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${dump.formattedTimestamp} | ${dump.totalCharacteristics} 个特征", color = TerminalGreen.copy(alpha = 0.5f), fontFamily = FontFamily.Monospace, fontSize = 10.sp)
        }
        IconButton(onClick = onLoad, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.PlayArrow, "加载转储", tint = TerminalCyan, modifier = Modifier.size(16.dp)) }
        IconButton(onClick = onReplay, enabled = isConnected && !isReplaying, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Replay, "重放写入", tint = if (isConnected && !isReplaying) TerminalAmber else TerminalAmber.copy(alpha = 0.3f), modifier = Modifier.size(16.dp)) }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "删除转储", tint = TerminalRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp)) }
    }
}
