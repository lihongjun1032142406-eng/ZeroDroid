package com.abhishek.zerodroid.features.wifi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen
import com.abhishek.zerodroid.ui.theme.TerminalRed

data class SecurityRating(val level: String, val label: String, val color: Color, val description: String)

fun getSecurityRating(securityLabel: String): SecurityRating {
    val upper = securityLabel.uppercase()
    return when {
        upper.contains("WPA3") -> SecurityRating("A+", "WPA3", TerminalCyan, "企业级安全保护")
        upper.contains("WPA2") && upper.contains("AES") -> SecurityRating("A", "WPA2-AES", TerminalGreen, "高强度加密")
        upper.contains("WPA2") -> SecurityRating("B+", "WPA2", TerminalGreen, "安全性良好")
        upper.contains("WPA") -> SecurityRating("C", "WPA", TerminalAmber, "加密方式已过时")
        upper.contains("WEP") -> SecurityRating("D", "WEP", TerminalRed, "容易被破解")
        upper.contains("OPEN") || upper.isEmpty() -> SecurityRating("F", "开放网络", TerminalRed, "未启用加密！")
        else -> SecurityRating("B", securityLabel, TerminalGreen, "已加密")
    }
}

@Composable
fun WifiSecurityBadge(securityLabel: String, modifier: Modifier = Modifier) {
    val rating = getSecurityRating(securityLabel)
    Row(modifier = modifier.background(color = rating.color.copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraSmall).padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = when { rating.level.startsWith("A") -> Icons.Default.Shield; rating.level == "F" -> Icons.Default.LockOpen; else -> Icons.Default.Lock }, contentDescription = null, tint = rating.color, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "${rating.level} ${rating.label}", style = MaterialTheme.typography.labelSmall, color = rating.color)
    }
}
