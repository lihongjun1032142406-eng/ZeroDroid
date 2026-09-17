package com.abhishek.zerodroid.features.wifi_direct.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifi_direct.domain.*
import com.abhishek.zerodroid.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GROUP_OWNER_IP = "192.168.49.1"

@Composable
fun WifiDirectTransferPanel(isGroupOwner: Boolean, groupOwnerAddress: String?, transfer: WifiDirectFileTransfer, modifier: Modifier = Modifier) {
    val progress by transfer.progress.collectAsState()
    val history by transfer.history.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var targetIp by remember { mutableStateOf(if (!isGroupOwner) GROUP_OWNER_IP else "") }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        selectedFileUri = uri
        selectedFileName = uri?.lastPathSegment?.substringAfterLast('/') ?: uri?.toString()
    }
    val active = progress.state == TransferState.Transferring || progress.state == TransferState.WaitingForConnection || progress.state == TransferState.Connecting
    Column(modifier.fillMaxWidth()) {
        RoleIndicatorSection(isGroupOwner)
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(progress.state != TransferState.Idle, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column { TransferProgressSection(progress) { transfer.cancel() }; Spacer(Modifier.height(12.dp)) }
        }
        AnimatedVisibility(!active) {
            Column {
                SendFileSection(selectedFileName, targetIp, isGroupOwner, { filePickerLauncher.launch(arrayOf("*/*")) }, { targetIp = it }, {
                    val uri = selectedFileUri ?: return@SendFileSection
                    val address = targetIp.ifBlank { return@SendFileSection }
                    scope.launch { transfer.sendFile(address, uri) }
                }, selectedFileUri != null && targetIp.isNotBlank())
                Spacer(Modifier.height(12.dp))
            }
        }
        AnimatedVisibility(!active) { Column { ReceiveFileSection { scope.launch { transfer.startReceiving() } }; Spacer(Modifier.height(12.dp)) } }
        if (history.isNotEmpty()) TransferHistorySection(history)
    }
}

@Composable private fun RoleIndicatorSection(isGroupOwner: Boolean) {
    TerminalCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("> 设备角色：", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(if (isGroupOwner) "群组所有者（服务器）" else "客户端", color = if (isGroupOwner) TerminalAmber else TerminalCyan, fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    } }
}

@Composable private fun SendFileSection(selectedFileName: String?, targetIp: String, isGroupOwner: Boolean, onSelectFile: () -> Unit, onTargetIpChanged: (String) -> Unit, onSend: () -> Unit, canSend: Boolean) {
    TerminalCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Text("> 发送文件", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onSelectFile, colors = ButtonDefaults.outlinedButtonColors(contentColor = TerminalCyan)) { Icon(Icons.Default.FilePresent, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("选择文件", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
            Spacer(Modifier.width(8.dp)); Text(selectedFileName ?: "未选择文件", color = if (selectedFileName != null) TerminalGreen else TerminalAmber.copy(alpha=.5f), fontFamily=FontFamily.Monospace, fontSize=11.sp, maxLines=1, overflow=TextOverflow.Ellipsis, modifier=Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(targetIp, onTargetIpChanged, label={Text("目标 IP 地址", fontFamily=FontFamily.Monospace, fontSize=11.sp)}, placeholder={Text(if(isGroupOwner) "输入客户端 IP" else GROUP_OWNER_IP, fontFamily=FontFamily.Monospace, fontSize=12.sp)}, singleLine=true, colors=OutlinedTextFieldDefaults.colors(focusedTextColor=TerminalGreen, unfocusedTextColor=TerminalGreen, cursorColor=TerminalGreen, focusedBorderColor=TerminalCyan, unfocusedBorderColor=TerminalGreen.copy(alpha=.3f), focusedLabelColor=TerminalCyan, unfocusedLabelColor=TerminalGreen.copy(alpha=.5f)), modifier=Modifier.fillMaxWidth(), textStyle=androidx.compose.ui.text.TextStyle(fontFamily=FontFamily.Monospace,fontSize=13.sp))
        Spacer(Modifier.height(12.dp))
        Button(onSend, enabled=canSend, colors=ButtonDefaults.buttonColors(containerColor=TerminalGreen,disabledContainerColor=TerminalGreen.copy(alpha=.2f)), modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Send,null,Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("发送",fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=13.sp) }
    } }
}

@Composable private fun ReceiveFileSection(onStartListening: () -> Unit) {
    TerminalCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
        Text("> 接收文件",color=TerminalGreen,fontFamily=FontFamily.Monospace,fontSize=14.sp,fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp))
        Text("打开服务器套接字，接收来自已连接对等设备的文件传输。",color=TerminalGreen.copy(alpha=.6f),fontFamily=FontFamily.Monospace,fontSize=11.sp); Spacer(Modifier.height(12.dp))
        Button(onStartListening,colors=ButtonDefaults.buttonColors(containerColor=TerminalCyan),modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.CloudDownload,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text("开始监听",fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=13.sp)}
    } }
}

@Composable private fun TransferProgressSection(progress: TransferProgress, onCancel: () -> Unit) {
    val transition=rememberInfiniteTransition(label="waiting_pulse")
    val pulse by transition.animateFloat(.3f,1f,infiniteRepeatable(tween(800),RepeatMode.Reverse),label="pulse_alpha")
    val color=when(progress.state){TransferState.Completed->TerminalGreen;TransferState.Failed->TerminalRed;TransferState.WaitingForConnection->TerminalAmber;TransferState.Connecting,TransferState.Transferring->TerminalCyan;TransferState.Idle->TerminalGreen}
    val text=when(progress.state){TransferState.Idle->"空闲";TransferState.WaitingForConnection->"等待连接...";TransferState.Connecting->"正在连接...";TransferState.Transferring->"正在传输";TransferState.Completed->"传输完成";TransferState.Failed->"传输失败"}
    val waiting=progress.state==TransferState.WaitingForConnection||progress.state==TransferState.Connecting
    val active=progress.state==TransferState.Transferring||waiting
    TerminalCard(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
        Text("> 传输状态",color=TerminalGreen,fontFamily=FontFamily.Monospace,fontSize=14.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp))
        Row(verticalAlignment=Alignment.CenterVertically,modifier=if(waiting)Modifier.alpha(pulse)else Modifier){Icon(when(progress.state){TransferState.Completed->Icons.Default.CheckCircle;TransferState.Failed->Icons.Default.Error;TransferState.Transferring->Icons.Default.CloudUpload;else->Icons.Default.CloudDownload},null,tint=color,modifier=Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(text,color=color,fontFamily=FontFamily.Monospace,fontSize=12.sp,fontWeight=FontWeight.Bold)}
        if(progress.fileName.isNotBlank()){Spacer(Modifier.height(6.dp));Text("文件：${progress.fileName}",color=TerminalGreen,fontFamily=FontFamily.Monospace,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)}
        if(progress.state==TransferState.Transferring){Spacer(Modifier.height(10.dp));LinearProgressIndicator({progress.progressPercent},Modifier.fillMaxWidth().height(6.dp),color=TerminalCyan,trackColor=TerminalGreen.copy(alpha=.15f));Spacer(Modifier.height(6.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}",color=TerminalGreen.copy(alpha=.7f),fontFamily=FontFamily.Monospace,fontSize=11.sp);Text("${(progress.progressPercent*100).toInt()}%",color=TerminalCyan,fontFamily=FontFamily.Monospace,fontSize=11.sp,fontWeight=FontWeight.Bold)};if(progress.speedBytesPerSec>0){Spacer(Modifier.height(4.dp));Text("速度：${formatBytes(progress.speedBytesPerSec)}/s",color=TerminalAmber,fontFamily=FontFamily.Monospace,fontSize=11.sp)}}
        progress.error?.let{Spacer(Modifier.height(6.dp));Text("错误：$it",color=TerminalRed,fontFamily=FontFamily.Monospace,fontSize=11.sp)}
        if(active){Spacer(Modifier.height(10.dp));OutlinedButton(onCancel,colors=ButtonDefaults.outlinedButtonColors(contentColor=TerminalRed),modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.Cancel,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("取消",fontFamily=FontFamily.Monospace,fontWeight=FontWeight.Bold,fontSize=12.sp)}}
    }}
}

@Composable private fun TransferHistorySection(history: List<TransferHistoryEntry>){TerminalCard(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text("> 传输历史",color=TerminalGreen,fontFamily=FontFamily.Monospace,fontSize=14.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));LazyColumn(Modifier.height((history.size.coerceAtMost(5)*56).dp)){items(history){entry->TransferHistoryItem(entry);HorizontalDivider(color=TerminalGreen.copy(alpha=.1f),thickness=1.dp)}}}}}

@Composable private fun TransferHistoryItem(entry: TransferHistoryEntry){Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically){Icon(if(entry.isSent)Icons.Default.CloudUpload else Icons.Default.CloudDownload,null,tint=if(entry.success)TerminalGreen else TerminalRed,modifier=Modifier.size(16.dp));Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(entry.fileName.ifBlank{"未知"},color=TerminalGreen,fontFamily=FontFamily.Monospace,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis);Row{Text(if(entry.isSent)"已发送" else "已接收",color=TerminalCyan.copy(alpha=.7f),fontFamily=FontFamily.Monospace,fontSize=10.sp);Spacer(Modifier.width(8.dp));Text(formatBytes(entry.fileSize),color=TerminalAmber.copy(alpha=.7f),fontFamily=FontFamily.Monospace,fontSize=10.sp);Spacer(Modifier.width(8.dp));Text(formatTimestamp(entry.timestamp),color=TerminalGreen.copy(alpha=.4f),fontFamily=FontFamily.Monospace,fontSize=10.sp)}};Icon(if(entry.success)Icons.Default.CheckCircle else Icons.Default.Error,null,tint=if(entry.success)TerminalGreen else TerminalRed,modifier=Modifier.size(14.dp))}}

private fun formatBytes(bytes:Long)=when{bytes<1024->"$bytes B";bytes<1024*1024->"%.1f KB".format(bytes/1024.0);bytes<1024L*1024*1024->"%.1f MB".format(bytes/(1024.0*1024.0));else->"%.2f GB".format(bytes/(1024.0*1024.0*1024.0))}
private fun formatTimestamp(timestamp:Long)=SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date(timestamp))
