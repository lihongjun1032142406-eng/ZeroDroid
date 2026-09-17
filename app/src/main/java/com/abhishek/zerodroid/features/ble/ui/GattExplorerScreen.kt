package com.abhishek.zerodroid.features.ble.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.EmptyState
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ble.domain.*
import com.abhishek.zerodroid.features.ble.viewmodel.GattViewModel
import com.abhishek.zerodroid.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GattExplorerScreen(deviceAddress: String, deviceName: String?, onBack: () -> Unit, viewModel: GattViewModel = hiltViewModel()) {
    val connState by viewModel.connectionState.collectAsState()
    val detailState by viewModel.detailState.collectAsState()
    DisposableEffect(deviceAddress) { viewModel.connect(deviceAddress); onDispose { viewModel.disconnect() } }
    if (detailState.info != null) CharacteristicDetailPanel(detailState, { viewModel.clearDetailState() }, { viewModel.readCharacteristic() }, { viewModel.writeCharacteristic() }, { viewModel.toggleNotification() }, { viewModel.updateWriteInput(it) }, { viewModel.toggleWriteMode() }, { viewModel.readDescriptor(it) })
    else ServiceListPanel(connState, deviceName, deviceAddress, onBack) { viewModel.selectCharacteristic(it) }
}

@Composable
private fun ServiceListPanel(connectionState: GattConnectionState, deviceName: String?, deviceAddress: String, onBack: () -> Unit, onCharacteristicSelected: (GattCharacteristicInfo) -> Unit) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Spacer(Modifier.height(4.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TerminalGreen) }
            Column(Modifier.weight(1f)) { Text(deviceName ?: "未知设备", style = MaterialTheme.typography.titleMedium, color = TerminalGreen, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(deviceAddress, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = TextSecondary) }
            ConnectionBadge(connectionState.connectionStatus)
        } }
        if (connectionState.isConnected) item { TerminalCard { Text("> 连接信息", style = MaterialTheme.typography.titleSmall, color = TerminalGreen); Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { InfoColumn("MTU", "${connectionState.mtu}"); InfoColumn("载荷", "${connectionState.payloadSize} B"); InfoColumn("服务", "${connectionState.services.size}"); InfoColumn("特征", "${connectionState.totalCharacteristics}") } } }
        if (connectionState.connectionStatus == GattConnectionStatus.Connecting) item { TerminalCard(animated = true) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = TerminalGreen); Text("正在连接…", color = TerminalGreen) } } }
        connectionState.error?.let { e -> item { TerminalCard(glowColor = TerminalRed) { Text("> 错误：$e", style = MaterialTheme.typography.bodySmall, color = TerminalRed) } } }
        if (connectionState.services.isEmpty() && connectionState.connectionStatus == GattConnectionStatus.Disconnected) item { EmptyState(Icons.Default.BluetoothDisabled, "未连接", "GATT 服务器连接已断开或无法建立连接") }
        items(connectionState.services, key = { it.uuid }) { service -> val open = expanded[service.uuid] ?: false; ServiceCard(service, open, { expanded[service.uuid] = !open }, onCharacteristicSelected) }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ConnectionBadge(status: GattConnectionStatus) {
    val pair = when (status) { GattConnectionStatus.Connected -> TerminalGreen to "已连接"; GattConnectionStatus.Connecting -> TerminalAmber to "连接中"; GattConnectionStatus.Disconnecting -> TerminalAmber to "断开中"; GattConnectionStatus.Disconnected -> TerminalRed to "已断开" }
    Text(pair.second, style = MaterialTheme.typography.labelSmall, color = pair.first, modifier = Modifier.background(pair.first.copy(alpha=.15f), RoundedCornerShape(4.dp)).padding(horizontal=8.dp, vertical=2.dp))
}

@Composable private fun InfoColumn(label:String,value:String){ Column(horizontalAlignment=Alignment.CenterHorizontally){ Text(value,style=MaterialTheme.typography.titleMedium,fontFamily=FontFamily.Monospace,color=TerminalGreen);Text(label,style=MaterialTheme.typography.labelSmall,color=TextDim) } }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceCard(service: GattServiceInfo, isExpanded:Boolean, onToggleExpand:()->Unit, onCharacteristicSelected:(GattCharacteristicInfo)->Unit){ TerminalCard(onClick=onToggleExpand){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){ Column(Modifier.weight(1f)){Text(service.displayName,style=MaterialTheme.typography.titleSmall,color=TerminalGreen);Text(BleUuidDatabase.shortenUuid(service.uuid),style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TextDim)};Row(verticalAlignment=Alignment.CenterVertically){Text("${service.characteristics.size}",color=TextSecondary);Icon(if(isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,if(isExpanded)"收起" else "展开",tint=TextSecondary,modifier=Modifier.size(20.dp))}}
    AnimatedVisibility(isExpanded,enter=expandVertically(),exit=shrinkVertically()){Column{Spacer(Modifier.height(8.dp));HorizontalDivider(color=TextDim.copy(alpha=.3f));service.characteristics.forEach{c->CharacteristicRow(c){onCharacteristicSelected(c)}}}}
} }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacteristicRow(characteristic:GattCharacteristicInfo,onClick:()->Unit){Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(characteristic.displayName,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically){Text(BleUuidDatabase.shortenUuid(characteristic.uuid),style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TextDim,fontSize=10.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){characteristic.propertiesList.forEach{PropertyBadge(it)}}}};Icon(Icons.Default.ChevronRight,"详情",tint=TextDim,modifier=Modifier.size(16.dp))}}

@Composable
private fun PropertyBadge(property:String){ val color=when(property){"Read","读取"->TerminalGreen;"Write","WriteNoResp","SignedWrite","写入","无响应写入","签名写入"->TerminalAmber;"Notify","Indicate","通知","指示"->TerminalCyan;else->TextSecondary};Text(property,style=MaterialTheme.typography.labelSmall,color=color,fontSize=9.sp,modifier=Modifier.background(color.copy(alpha=.15f),RoundedCornerShape(3.dp)).padding(horizontal=4.dp,vertical=1.dp)) }

@Composable
private fun CharacteristicDetailPanel(state:CharacteristicDetailState,onBack:()->Unit,onRead:()->Unit,onWrite:()->Unit,onToggleNotify:()->Unit,onWriteInputChanged:(String)->Unit,onToggleWriteMode:()->Unit,onReadDescriptor:(String)->Unit){
    val info=state.info?:return;val tf=remember{SimpleDateFormat("HH:mm:ss.SSS",Locale.US)}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        item{Spacer(Modifier.height(4.dp));Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"返回",tint=TerminalGreen)};Column(Modifier.weight(1f)){Text(info.displayName,style=MaterialTheme.typography.titleMedium,color=TerminalGreen,maxLines=1,overflow=TextOverflow.Ellipsis);Text(info.uuid,style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TextDim,maxLines=1,overflow=TextOverflow.Ellipsis)}}}
        item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){
            if(info.isReadable) Button(onClick=onRead,enabled=!state.isLoading,colors=ButtonDefaults.buttonColors(containerColor=TerminalGreen.copy(alpha=.2f)),modifier=Modifier.weight(1f)){Text("读取",color=TerminalGreen)}
            if(info.isNotifiable||info.isIndicatable) Button(onClick=onToggleNotify,enabled=!state.isLoading,colors=ButtonDefaults.buttonColors(containerColor=TerminalCyan.copy(alpha=if(state.isNotifying).3f else .15f)),modifier=Modifier.weight(1f)){Icon(if(state.isNotifying)Icons.Default.Notifications else Icons.Default.NotificationsOff,null,tint=TerminalCyan,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text(if(state.isNotifying)"停止" else if(info.isIndicatable)"指示" else "通知",color=TerminalCyan)}
        }}
        if(state.isLoading)item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=TerminalGreen)}}
        state.error?.let{e->item{TerminalCard(glowColor=TerminalRed){Text("> $e",style=MaterialTheme.typography.bodySmall,color=TerminalRed)}}}
        if(info.isWritable)item{TerminalCard(glowColor=TerminalAmber,glowAlpha=TerminalAmberGlow,borderColor=TerminalAmber.copy(alpha=.3f)){Text("> 写入值",style=MaterialTheme.typography.titleSmall,color=TerminalAmber);Spacer(Modifier.height(8.dp));Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(value=state.writeInput,onValueChange=onWriteInputChanged,modifier=Modifier.weight(1f),placeholder={Text(if(state.writeMode==WriteMode.Hex)"FF 00 1A..." else "输入文本…",color=TextDim)},textStyle=MaterialTheme.typography.bodySmall.copy(fontFamily=FontFamily.Monospace,color=TerminalAmber),singleLine=true,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=TerminalAmber,unfocusedBorderColor=TerminalAmber.copy(alpha=.3f),cursorColor=TerminalAmber))};Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick=onToggleWriteMode,colors=ButtonDefaults.outlinedButtonColors(contentColor=TerminalAmber)){Text(if(state.writeMode==WriteMode.Hex)"HEX" else "TXT",style=MaterialTheme.typography.labelSmall)};Button(onClick=onWrite,enabled=!state.isLoading&&state.writeInput.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=TerminalAmber.copy(alpha=.3f))){Icon(Icons.Default.Send,"发送",tint=TerminalAmber,modifier=Modifier.size(16.dp));Spacer(Modifier.width(4.dp));Text("发送",color=TerminalAmber)}}}}
        state.lastReadValue?.let{v->item{ValueDisplayCard(v,state.parsedDisplay,tf)}}
        if(state.notificationValues.isNotEmpty()){item{TerminalCard(glowColor=TerminalCyan,glowAlpha=TerminalCyanGlow,borderColor=TerminalCyan.copy(alpha=.3f)){Text("> 通知日志（${state.notificationValues.size}）",style=MaterialTheme.typography.titleSmall,color=TerminalCyan)}};items(state.notificationValues.reversed().take(50),key={it.timestamp}){v->TerminalCard{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(tf.format(Date(v.timestamp)),style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TextDim,fontSize=10.sp);Text("${v.byteCount} B",color=TextDim,fontSize=10.sp)};Text(v.hexString,style=MaterialTheme.typography.bodySmall,fontFamily=FontFamily.Monospace,color=TerminalCyan)}}}
        if(info.descriptors.isNotEmpty())item{TerminalCard{Text("> 描述符（${info.descriptors.size}）",style=MaterialTheme.typography.titleSmall,color=TerminalGreen);Spacer(Modifier.height(4.dp));info.descriptors.forEach{d->Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(d.displayName,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Text(BleUuidDatabase.shortenUuid(d.uuid),style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TextDim,fontSize=10.sp);state.descriptorValues[d.uuid]?.let{Text(it.hexString,style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TerminalGreen,fontSize=10.sp)}};OutlinedButton(onClick={onReadDescriptor(d.uuid)},colors=ButtonDefaults.outlinedButtonColors(contentColor=TerminalGreen)){Text("读取",style=MaterialTheme.typography.labelSmall)}}}}}
        item{Spacer(Modifier.height(16.dp))}
    }
}

@Composable
private fun ValueDisplayCard(value:CharacteristicValue,parsedDisplay:String?,timeFormatter:SimpleDateFormat){TerminalCard(glowColor=TerminalGreen,glowAlpha=TerminalGreenGlow){Text("> 值",style=MaterialTheme.typography.titleSmall,color=TerminalGreen);Spacer(Modifier.height(4.dp));parsedDisplay?.let{Text(it,style=MaterialTheme.typography.bodyMedium,color=TerminalGreen);Spacer(Modifier.height(8.dp))};Text("HEX：",style=MaterialTheme.typography.labelSmall,color=TextDim);Text(value.hexString,style=MaterialTheme.typography.bodySmall,fontFamily=FontFamily.Monospace,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(4.dp));Text("ASCII：",style=MaterialTheme.typography.labelSmall,color=TextDim);Text(value.asciiString,style=MaterialTheme.typography.bodySmall,fontFamily=FontFamily.Monospace,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(4.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${value.byteCount} 字节",style=MaterialTheme.typography.labelSmall,color=TextDim);Text(timeFormatter.format(Date(value.timestamp)),style=MaterialTheme.typography.labelSmall,fontFamily=FontFamily.Monospace,color=TextDim)}}}
