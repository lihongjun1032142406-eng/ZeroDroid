package com.abhishek.zerodroid.features.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.alert_center.ui.AlertCard
import com.abhishek.zerodroid.navigation.ScreenCategory
import com.abhishek.zerodroid.navigation.ZeroDroidScreen
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed
import com.abhishek.zerodroid.ui.theme.TextDim

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val hardwareItems by viewModel.hardwareItems.collectAsState()
    val lastUsed by viewModel.lastUsedFeature.collectAsState()
    val recentAlerts by viewModel.recentAlerts.collectAsState()
    val totalAlertCount by viewModel.totalAlertCount.collectAsState()
    val deviceInfo = viewModel.deviceInfo

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(title = "> 设备信息") }
        item {
            TerminalCard {
                InfoRow("型号", deviceInfo.model)
                InfoRow("Android", deviceInfo.androidVersion)
                InfoRow("设备", deviceInfo.device)
                InfoRow("主板", deviceInfo.board)
            }
        }

        item { SectionHeader(title = "> 硬件能力") }
        item {
            TerminalCard {
                val rows = hardwareItems.chunked(2)
                rows.forEach { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        pair.forEach { item ->
                            HardwareChipRow(item.name, item.isAvailable, Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item { SectionHeader(title = "> 快速扫描") }
        item {
            val feature = lastUsed
            TerminalCard(animated = feature != null, onClick = if (feature != null) {{ onNavigate(feature.route) }} else null) {
                if (feature != null) {
                    Text("上次使用：${feature.title}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("点击继续 →", style = MaterialTheme.typography.labelSmall, color = TerminalGreen)
                } else {
                    Text("暂无最近扫描", style = MaterialTheme.typography.bodyMedium, color = TextDim)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("从下方选择一个工具开始使用", style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(title = "> 最近威胁")
                if (totalAlertCount > 0) {
                    Text("查看全部（$totalAlertCount）→", style = MaterialTheme.typography.labelSmall, color = TerminalGreen,
                        modifier = Modifier.padding(top = 4.dp).clickable { onNavigate(ZeroDroidScreen.AlertCenter.route) })
                }
            }
        }
        if (recentAlerts.isEmpty()) {
            item {
                TerminalCard(onClick = { onNavigate(ZeroDroidScreen.AlertCenter.route) }) {
                    Text("暂未检测到威胁", style = MaterialTheme.typography.bodyMedium, color = TextDim)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("恶意 AP、断连攻击、GPS 欺骗、隐藏摄像头和追踪器扫描产生的警报会显示在这里", style = MaterialTheme.typography.labelSmall, color = TextDim)
                }
            }
        } else {
            items(recentAlerts, key = { it.id }) { alert -> AlertCard(alert = alert) }
        }

        item { SectionHeader(title = "> 工具箱") }
        val orderedCategories = listOf(ScreenCategory.WIRELESS, ScreenCategory.RF, ScreenCategory.SENSORS, ScreenCategory.NETWORK, ScreenCategory.SECURITY)
        orderedCategories.forEach { category ->
            val screens = (ZeroDroidScreen.byCategory[category] ?: return@forEach).filter { it != ZeroDroidScreen.Dashboard }
            item { Text("/* ${category.label} */", style = MaterialTheme.typography.labelSmall, color = TextDim, letterSpacing = 2.sp) }
            item {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    screens.forEach { screen ->
                        FeatureTile(screen) {
                            viewModel.saveLastUsed(screen.route, screen.title)
                            onNavigate(screen.route)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) = Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label:", style = MaterialTheme.typography.labelMedium, color = TextDim, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun HardwareChipRow(name: String, isAvailable: Boolean, modifier: Modifier = Modifier) {
    val statusText = if (isAvailable) "在线" else "离线"
    val statusColor = if (isAvailable) TerminalGreen else TerminalRed
    Row(modifier = modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.width(6.dp))
        Text("[$statusText]", style = MaterialTheme.typography.labelSmall, color = statusColor, fontSize = 9.sp)
    }
}

@Composable
private fun FeatureTile(screen: ZeroDroidScreen, onClick: () -> Unit) {
    TerminalCard(modifier = Modifier.width(100.dp), onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(screen.icon, contentDescription = screen.title, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(screen.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, fontSize = 10.sp)
        }
    }
}
