package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.nfc.domain.MifareClassicReader
import com.abhishek.zerodroid.features.nfc.domain.MifareSectorData
import com.abhishek.zerodroid.ui.theme.*

private val MonoFont = FontFamily.Monospace
private val HexCharRegex = Regex("^[0-9A-Fa-f]*$")
private fun ByteArray.toHexDisplay(): String = joinToString("") { "%02X".format(it) }
private fun String.hexToByteArray(): ByteArray? {
    val clean = replace(" ", "").uppercase()
    if (clean.length % 2 != 0 || !clean.matches(Regex("^[0-9A-F]*$"))) return null
    return ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}
private fun byteToAscii(b: Byte): Char { val c = b.toInt() and 0xFF; return if (c in 0x20..0x7E) c.toChar() else '.' }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcMifareEnhancedPanel(
    sectors: List<MifareSectorData>, onAddCustomKey: (ByteArray) -> Unit,
    onRemoveCustomKey: (Int) -> Unit, customKeys: List<ByteArray>,
    onCopyDump: (String) -> Unit, onWriteBlock: (blockIndex: Int, data: ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val reader = remember { MifareClassicReader() }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CustomKeysSection(customKeys, onAddCustomKey, onRemoveCustomKey)
        KeyDictionarySection(customKeys)
        if (sectors.isNotEmpty()) {
            ExportSection(sectors, reader, onCopyDump)
            SectorDumpSection(sectors, reader, onWriteBlock)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomKeysSection(customKeys: List<ByteArray>, onAddCustomKey: (ByteArray) -> Unit, onRemoveCustomKey: (Int) -> Unit) {
    var keyInput by remember { mutableStateOf("") }
    var keyError by remember { mutableStateOf<String?>(null) }
    TerminalCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Key, null, tint = TerminalAmber, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("自定义密钥", color = TerminalAmber, fontFamily = MonoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { v -> val f = v.filter { it.isLetterOrDigit() }.uppercase(); if (f.length <= 12 && (f.isEmpty() || HexCharRegex.matches(f))) { keyInput = f; keyError = null } },
                    placeholder = { Text("FFFFFFFFFFFF", color = TerminalGreen.copy(alpha=.3f), fontFamily=MonoFont, fontSize=13.sp) },
                    textStyle = TextStyle(color=TerminalGreen, fontFamily=MonoFont, fontSize=13.sp), singleLine=true, modifier=Modifier.weight(1f),
                    keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters),
                    colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=TerminalGreen, unfocusedBorderColor=TerminalGreen.copy(alpha=.4f), cursorColor=TerminalGreen),
                    isError=keyError != null,
                    supportingText={{ Text(keyError ?: "${keyInput.length}/12 个 HEX 字符（6 字节）", color=if(keyError!=null) TerminalRed else TerminalGreen.copy(alpha=.5f), fontFamily=MonoFont, fontSize=11.sp) }}
                )
                Button(onClick={
                    if(keyInput.length != 12){ keyError="密钥必须为 12 个 HEX 字符（6 字节）"; return@Button }
                    val bytes=keyInput.hexToByteArray(); if(bytes==null || bytes.size!=6){ keyError="无效的 HEX 密钥"; return@Button }
                    if(customKeys.any { it.toHexDisplay()==bytes.toHexDisplay() }){ keyError="该密钥已添加"; return@Button }
                    onAddCustomKey(bytes); keyInput=""; keyError=null
                }, colors=ButtonDefaults.buttonColors(containerColor=TerminalGreen.copy(alpha=.15f),contentColor=TerminalGreen), modifier=Modifier.padding(top=4.dp)) {
                    Icon(Icons.Default.Add,"添加密钥",Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("添加",fontFamily=MonoFont,fontSize=12.sp,fontWeight=FontWeight.Bold)
                }
            }
            if(customKeys.isNotEmpty()) {
                Spacer(Modifier.height(8.dp)); FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    customKeys.forEachIndexed { i,k -> CustomKeyChip(k.toHexDisplay()){ onRemoveCustomKey(i) } }
                }
            }
        }
    }
}

@Composable
private fun CustomKeyChip(keyHex:String,onRemove:()->Unit){
    Row(Modifier.background(TerminalCyan.copy(alpha=.1f),RoundedCornerShape(4.dp)).border(1.dp,TerminalCyan.copy(alpha=.4f),RoundedCornerShape(4.dp)).padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
        Text(keyHex,color=TerminalCyan,fontFamily=MonoFont,fontSize=12.sp)
        Icon(Icons.Default.Close,"移除密钥",tint=TerminalRed.copy(alpha=.7f),modifier=Modifier.size(14.dp).clip(CircleShape).clickable{onRemove()})
    }
}

@Composable
private fun KeyDictionarySection(customKeys:List<ByteArray>){
    var showAllKeys by remember{ mutableStateOf(false) }; val defaultCount=MifareClassicReader.DEFAULT_KEYS.size; val customCount=customKeys.size; val totalCount=defaultCount+customCount
    TerminalCard { Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(Modifier.fillMaxWidth().clickable{showAllKeys=!showAllKeys},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Row(verticalAlignment=Alignment.CenterVertically){ Icon(if(showAllKeys) Icons.Default.Visibility else Icons.Default.VisibilityOff,null,tint=TerminalGreen.copy(alpha=.7f),modifier=Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("密钥字典",color=TerminalGreen,fontFamily=MonoFont,fontWeight=FontWeight.Bold,fontSize=13.sp) }
            Row(verticalAlignment=Alignment.CenterVertically){ Text("$totalCount 个密钥（$defaultCount 个默认 + $customCount 个自定义）",color=TerminalGreen.copy(alpha=.6f),fontFamily=MonoFont,fontSize=11.sp); Spacer(Modifier.width(4.dp)); Icon(if(showAllKeys) Icons.Default.ExpandLess else Icons.Default.ExpandMore,if(showAllKeys)"隐藏密钥" else "显示密钥",tint=TerminalGreen.copy(alpha=.6f),modifier=Modifier.size(18.dp)) }
        }
        AnimatedVisibility(showAllKeys,enter=expandVertically(),exit=shrinkVertically()){ Column(Modifier.padding(top=8.dp)){
            if(customKeys.isNotEmpty()){ Text("-- 自定义密钥 --",color=TerminalCyan.copy(alpha=.7f),fontFamily=MonoFont,fontSize=11.sp); customKeys.forEachIndexed{i,k->Text("  [${i+1}] ${k.toHexDisplay()}",color=TerminalCyan,fontFamily=MonoFont,fontSize=11.sp)}; Spacer(Modifier.height(4.dp)) }
            Text("-- 默认密钥 --",color=TerminalGreen.copy(alpha=.7f),fontFamily=MonoFont,fontSize=11.sp); MifareClassicReader.DEFAULT_KEYS.forEachIndexed{i,k->Text("  [${i+1}] ${k.toHexDisplay()}",color=TerminalGreen.copy(alpha=.8f),fontFamily=MonoFont,fontSize=11.sp)}
        }}
    }}
}

@Composable
private fun ExportSection(sectors:List<MifareSectorData>,reader:MifareClassicReader,onCopyDump:(String)->Unit){
    val authCount=sectors.count{it.isAuthenticated}; TerminalCard { Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
        Column{Text("转储摘要",color=TerminalGreen,fontFamily=MonoFont,fontWeight=FontWeight.Bold,fontSize=13.sp);Text("已认证 $authCount/${sectors.size} 个扇区",color=if(authCount==sectors.size)TerminalGreen else TerminalAmber,fontFamily=MonoFont,fontSize=11.sp)}
        Button(onClick={onCopyDump(reader.formatDump(sectors))},colors=ButtonDefaults.buttonColors(containerColor=TerminalCyan.copy(alpha=.15f),contentColor=TerminalCyan)){Icon(Icons.Default.ContentCopy,null,Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("复制转储",fontFamily=MonoFont,fontSize=12.sp,fontWeight=FontWeight.Bold)}
    }}
}

@Composable
private fun SectorDumpSection(sectors:List<MifareSectorData>,reader:MifareClassicReader,onWriteBlock:(Int,ByteArray)->Unit){ Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ sectors.forEach{SectorCard(it,reader,onWriteBlock)} } }

@Composable
private fun SectorCard(sector:MifareSectorData,reader:MifareClassicReader,onWriteBlock:(Int,ByteArray)->Unit){
    var expanded by remember{mutableStateOf(false)}; var showAccessBits by remember{mutableStateOf(false)}
    TerminalCard { Column(Modifier.fillMaxWidth().padding(12.dp)){
        Row(Modifier.fillMaxWidth().clickable{expanded=!expanded},verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            Row(verticalAlignment=Alignment.CenterVertically){Icon(if(sector.isAuthenticated)Icons.Default.LockOpen else Icons.Default.Lock,null,tint=if(sector.isAuthenticated)TerminalGreen else TerminalRed,modifier=Modifier.size(16.dp));Spacer(Modifier.width(8.dp));Text("扇区 ${sector.sectorIndex}",color=if(sector.isAuthenticated)TerminalGreen else TerminalRed,fontFamily=MonoFont,fontWeight=FontWeight.Bold,fontSize=13.sp)}
            Row(verticalAlignment=Alignment.CenterVertically){if(sector.isAuthenticated)Text("密钥 ${sector.keyType}：${sector.keyUsed}",color=TerminalGreen.copy(alpha=.6f),fontFamily=MonoFont,fontSize=10.sp) else Text("未认证",color=TerminalRed.copy(alpha=.7f),fontFamily=MonoFont,fontSize=10.sp);Spacer(Modifier.width(4.dp));Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,if(expanded)"收起" else "展开",tint=TerminalGreen.copy(alpha=.6f),modifier=Modifier.size(18.dp))}
        }
        AnimatedVisibility(expanded,enter=expandVertically(),exit=shrinkVertically()){Column(Modifier.padding(top=8.dp)){
            if(!sector.isAuthenticated) Text("认证失败 - 没有匹配的密钥",color=TerminalRed.copy(alpha=.7f),fontFamily=MonoFont,fontSize=11.sp,modifier=Modifier.padding(vertical=4.dp)) else {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){Text("块",color=TerminalGreen.copy(alpha=.5f),fontFamily=MonoFont,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.width(36.dp));Text("HEX 数据",color=TerminalGreen.copy(alpha=.5f),fontFamily=MonoFont,fontSize=10.sp,fontWeight=FontWeight.Bold,modifier=Modifier.width(290.dp));Text("ASCII",color=TerminalGreen.copy(alpha=.5f),fontFamily=MonoFont,fontSize=10.sp,fontWeight=FontWeight.Bold)}
                HorizontalDivider(color=TerminalGreen.copy(alpha=.2f),modifier=Modifier.padding(vertical=2.dp)); sector.blocks.forEachIndexed{i,b->BlockRow(b,i==sector.blocks.lastIndex,sector.isAuthenticated,onWriteBlock)}
                val trailer=sector.blocks.lastOrNull(); if(trailer!=null && trailer.data.size>=10){Spacer(Modifier.height(6.dp));Row(Modifier.fillMaxWidth().clickable{showAccessBits=!showAccessBits},verticalAlignment=Alignment.CenterVertically){Text(if(showAccessBits)"[-] 访问位" else "[+] 访问位",color=TerminalAmber.copy(alpha=.8f),fontFamily=MonoFont,fontSize=11.sp,fontWeight=FontWeight.Bold)};AnimatedVisibility(showAccessBits,enter=expandVertically(),exit=shrinkVertically()){Column(Modifier.padding(top=4.dp).background(TerminalAmber.copy(alpha=.05f),RoundedCornerShape(4.dp)).padding(8.dp)){reader.interpretAccessBits(trailer.data).forEach{line->Text(line,color=TerminalAmber.copy(alpha=.9f),fontFamily=MonoFont,fontSize=10.sp,modifier=Modifier.padding(vertical=1.dp))}}}}
            }
        }}
    }}
}

@Composable
private fun BlockRow(block:com.abhishek.zerodroid.features.nfc.domain.MifareBlockData,isTrailer:Boolean,isAuthenticated:Boolean,onWriteBlock:(Int,ByteArray)->Unit){
    var showWriteDialog by remember{mutableStateOf(false)}; val blockColor=if(isTrailer)TerminalAmber else TerminalGreen; val blockLabel=if(isTrailer)"[T]" else ""
    Row(Modifier.fillMaxWidth().padding(vertical=2.dp).horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){
        Text("%3d".format(block.blockIndex),color=blockColor.copy(alpha=.7f),fontFamily=MonoFont,fontSize=11.sp,modifier=Modifier.width(36.dp))
        if(block.hexString == "READ ERROR") Text("[读取错误]",color=TerminalRed,fontFamily=MonoFont,fontSize=11.sp) else {
            Text(buildAnnotatedString{if(isTrailer && block.data.size>=16){withStyle(SpanStyle(color=TerminalCyan)){append(block.data.take(6).joinToString(" "){"%02X".format(it)})};append(" ");withStyle(SpanStyle(color=TerminalAmber)){append(block.data.drop(6).take(4).joinToString(" "){"%02X".format(it)})};append(" ");withStyle(SpanStyle(color=TerminalCyan)){append(block.data.drop(10).take(6).joinToString(" "){"%02X".format(it)})}}else withStyle(SpanStyle(color=blockColor)){append(block.hexString)}},fontFamily=MonoFont,fontSize=11.sp,modifier=Modifier.width(290.dp))
            Text(block.data.map{byteToAscii(it)}.joinToString(""),color=blockColor.copy(alpha=.5f),fontFamily=MonoFont,fontSize=11.sp,modifier=Modifier.width(100.dp));if(isTrailer)Text(blockLabel,color=TerminalAmber,fontFamily=MonoFont,fontSize=10.sp,fontWeight=FontWeight.Bold)
            if(!isTrailer && isAuthenticated){Spacer(Modifier.width(4.dp));IconButton(onClick={showWriteDialog=true},modifier=Modifier.size(20.dp)){Icon(Icons.Default.Edit,"写入块 ${block.blockIndex}",tint=TerminalAmber.copy(alpha=.6f),modifier=Modifier.size(14.dp))}}
        }
    }
    if(showWriteDialog)WriteBlockDialog(block.blockIndex,block.hexString.replace(" ",""),{showWriteDialog=false}){data->onWriteBlock(block.blockIndex,data);showWriteDialog=false}
}

@Composable
private fun WriteBlockDialog(blockIndex:Int,currentData:String,onDismiss:()->Unit,onConfirmWrite:(ByteArray)->Unit){
    var hexInput by remember{mutableStateOf(currentData)};var error by remember{mutableStateOf<String?>(null)};var confirmStep by remember{mutableIntStateOf(0)}
    AlertDialog(onDismissRequest=onDismiss,containerColor=Color(0xFF0D0D0D),titleContentColor=TerminalAmber,
        title={Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Warning,null,tint=TerminalAmber,modifier=Modifier.size(20.dp));Spacer(Modifier.width(8.dp));Text(if(confirmStep==0)"写入块 $blockIndex" else "确认写入",fontFamily=MonoFont,fontWeight=FontWeight.Bold,fontSize=14.sp)}},
        text={Column{if(confirmStep==0){Text("输入 32 个 HEX 字符（16 字节）以写入：",color=TerminalGreen.copy(alpha=.8f),fontFamily=MonoFont,fontSize=11.sp);Spacer(Modifier.height(8.dp));OutlinedTextField(value=hexInput,onValueChange={v->val f=v.filter{it.isLetterOrDigit()}.uppercase();if(f.length<=32&&(f.isEmpty()||HexCharRegex.matches(f))){hexInput=f;error=null}},textStyle=TextStyle(color=TerminalGreen,fontFamily=MonoFont,fontSize=12.sp),singleLine=true,modifier=Modifier.fillMaxWidth(),keyboardOptions=KeyboardOptions(capitalization=KeyboardCapitalization.Characters),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=TerminalAmber,unfocusedBorderColor=TerminalAmber.copy(alpha=.4f),cursorColor=TerminalAmber),isError=error!=null,supportingText={{Text(error?:"${hexInput.length}/32 个 HEX 字符",color=if(error!=null)TerminalRed else TerminalGreen.copy(alpha=.5f),fontFamily=MonoFont,fontSize=10.sp)}})}else{Text("警告：这将永久覆盖块 $blockIndex。",color=TerminalRed,fontFamily=MonoFont,fontSize=12.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("待写入数据：",color=TerminalGreen.copy(alpha=.7f),fontFamily=MonoFont,fontSize=11.sp);Spacer(Modifier.height(4.dp));Box(Modifier.fillMaxWidth().background(TerminalAmber.copy(alpha=.08f),RoundedCornerShape(4.dp)).border(1.dp,TerminalAmber.copy(alpha=.3f),RoundedCornerShape(4.dp)).padding(8.dp)){Text(hexInput.chunked(2).joinToString(" "),color=TerminalAmber,fontFamily=MonoFont,fontSize=12.sp,textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())}}}},
        confirmButton={if(confirmStep==0)Button(onClick={if(hexInput.length!=32){error="必须恰好为 32 个 HEX 字符";return@Button};val b=hexInput.hexToByteArray();if(b==null||b.size!=16){error="无效的 HEX 数据";return@Button};confirmStep=1},colors=ButtonDefaults.buttonColors(containerColor=TerminalAmber.copy(alpha=.2f),contentColor=TerminalAmber)){Text("下一步",fontFamily=MonoFont,fontWeight=FontWeight.Bold,fontSize=12.sp)}else Button(onClick={val b=hexInput.hexToByteArray();if(b!=null&&b.size==16)onConfirmWrite(b)},colors=ButtonDefaults.buttonColors(containerColor=TerminalRed.copy(alpha=.3f),contentColor=TerminalRed)){Text("写入",fontFamily=MonoFont,fontWeight=FontWeight.Bold,fontSize=12.sp)}},
        dismissButton={TextButton(onClick={if(confirmStep==1)confirmStep=0 else onDismiss()}){Text(if(confirmStep==1)"返回" else "取消",color=TerminalGreen.copy(alpha=.7f),fontFamily=MonoFont,fontSize=12.sp)}})
}
