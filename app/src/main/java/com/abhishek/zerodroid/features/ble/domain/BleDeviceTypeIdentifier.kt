package com.abhishek.zerodroid.features.ble.domain

import androidx.compose.ui.graphics.Color
import com.abhishek.zerodroid.ui.theme.TerminalAmber
import com.abhishek.zerodroid.ui.theme.TerminalCyan
import com.abhishek.zerodroid.ui.theme.TerminalGreen

data class BleDeviceType(val category: String, val icon: String, val color: Color)

object BleDeviceTypeIdentifier {
    private val namePatterns = listOf(
        Regex("(?i)(airpods|buds|earbuds|pods|jabra|jbl|beats|bose|sony|wf-|wh-)") to BleDeviceType("音频", "headphones", TerminalCyan),
        Regex("(?i)(band|mi band|fitbit|garmin|watch|huawei band|amazfit|versa|charge)") to BleDeviceType("运动健康", "watch", TerminalGreen),
        Regex("(?i)(tile|airtag|smarttag|chipolo|nut|finder|tracker)") to BleDeviceType("追踪器", "location", TerminalAmber),
        Regex("(?i)(keyboard|mouse|trackpad|logitech|mx|k380)") to BleDeviceType("输入设备", "keyboard", TerminalCyan),
        Regex("(?i)(tv|roku|fire|chromecast|shield|apple.tv)") to BleDeviceType("电视/媒体", "tv", TerminalCyan),
        Regex("(?i)(iphone|pixel|galaxy|oneplus|samsung|huawei|xiaomi|oppo|vivo|redmi)") to BleDeviceType("手机", "phone", TerminalGreen),
        Regex("(?i)(ipad|tab|tablet|surface)") to BleDeviceType("平板", "tablet", TerminalCyan),
        Regex("(?i)(printer|hp |epson|canon|brother)") to BleDeviceType("打印机", "print", TerminalCyan),
        Regex("(?i)(thermostat|nest|ecobee|hue|light|bulb|plug|switch|smart)") to BleDeviceType("智能家居", "home", TerminalGreen),
        Regex("(?i)(car|tesla|bmw|ford|obd|elm327)") to BleDeviceType("车载设备", "car", TerminalAmber)
    )

    fun identify(name: String?, serviceUuids: List<String> = emptyList()): BleDeviceType {
        if (name != null) {
            for ((pattern, type) in namePatterns) if (pattern.containsMatchIn(name)) return type
        }
        return BleDeviceType("未知", "bluetooth", TerminalCyan)
    }
}
