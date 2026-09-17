package com.abhishek.zerodroid.features.sdr.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard

@Composable
fun SdrInfoPanel(modifier: Modifier = Modifier) {
    TerminalCard(modifier = modifier) {
        Text(text = "> SDR 信息", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "ZeroDroid 可以检测通过 USB OTG 连接的 RTL-SDR 及其他兼容 SDR 硬件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "完整的信号处理需要原生 librtlsdr 实现，目前不在本功能范围内。如需完整 SDR 功能，可使用 SDR Touch 或 RF Analyzer 等应用。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "支持：RTL2832U、RTL2838、HackRF、AirSpy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
