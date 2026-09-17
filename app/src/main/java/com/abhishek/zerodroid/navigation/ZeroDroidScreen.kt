package com.abhishek.zerodroid.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class ScreenCategory(val label: String) {
    WIRELESS("无线通信"), RF("射频与信号"), SENSORS("传感器"), NETWORK("网络"), SECURITY("安全工具")
}

sealed class ZeroDroidScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val category: ScreenCategory
) {
    data object Dashboard : ZeroDroidScreen("dashboard", "仪表盘", Icons.Default.Home, ScreenCategory.SENSORS)
    data object Sensors : ZeroDroidScreen("sensors", "传感器仪表盘", Icons.Default.Sensors, ScreenCategory.SENSORS)
    data object Wifi : ZeroDroidScreen("wifi", "Wi-Fi 分析器", Icons.Default.Wifi, ScreenCategory.WIRELESS)
    data object Ble : ZeroDroidScreen("ble", "BLE 扫描器", Icons.Default.Bluetooth, ScreenCategory.WIRELESS)
    data object Nfc : ZeroDroidScreen("nfc", "NFC 工具", Icons.Default.Nfc, ScreenCategory.WIRELESS)
    data object Ir : ZeroDroidScreen("ir", "红外遥控", Icons.Default.SettingsRemote, ScreenCategory.RF)
    data object Uwb : ZeroDroidScreen("uwb", "UWB 雷达", Icons.Default.GraphicEq, ScreenCategory.RF)
    data object Usb : ZeroDroidScreen("usb", "USB 设备", Icons.Default.Usb, ScreenCategory.NETWORK)
    data object Sdr : ZeroDroidScreen("sdr", "SDR 无线电", Icons.Default.Radio, ScreenCategory.RF)
    data object Camera : ZeroDroidScreen("camera", "二维码扫描器", Icons.Default.CameraAlt, ScreenCategory.SENSORS)
    data object Ultrasonic : ZeroDroidScreen("ultrasonic", "超声波", Icons.Default.GraphicEq, ScreenCategory.RF)
    data object Wardriving : ZeroDroidScreen("wardriving", "无线网络测绘", Icons.Default.Map, ScreenCategory.NETWORK)
    data object WifiAware : ZeroDroidScreen("wifi_aware", "Wi-Fi Aware", Icons.Default.WifiFind, ScreenCategory.WIRELESS)
    data object CellTower : ZeroDroidScreen("cell_tower", "蜂窝基站", Icons.Default.CellTower, ScreenCategory.NETWORK)
    data object UsbCamera : ZeroDroidScreen("usb_camera", "USB 摄像头", Icons.Default.Videocam, ScreenCategory.SENSORS)
    data object Gps : ZeroDroidScreen("gps", "GPS 跟踪器", Icons.Default.MyLocation, ScreenCategory.SENSORS)
    data object BluetoothClassic : ZeroDroidScreen("bluetooth_classic", "经典蓝牙", Icons.Default.BluetoothSearching, ScreenCategory.WIRELESS)
    data object WifiDirect : ZeroDroidScreen("wifi_direct", "Wi-Fi Direct", Icons.Default.Share, ScreenCategory.WIRELESS)
    data object HiddenCamera : ZeroDroidScreen("hidden_camera", "隐藏摄像头检测", Icons.Default.Security, ScreenCategory.SECURITY)
    data object GpsSpoofDetector : ZeroDroidScreen("gps_spoof_detector", "GPS 欺骗检测", Icons.Default.GpsFixed, ScreenCategory.SECURITY)
    data object BluetoothTracker : ZeroDroidScreen("bluetooth_tracker", "蓝牙追踪器扫描", Icons.Default.LocationSearching, ScreenCategory.SECURITY)
    data object RogueAp : ZeroDroidScreen("rogue_ap", "恶意 AP 检测", Icons.Default.WifiOff, ScreenCategory.SECURITY)
    data object NetworkScanner : ZeroDroidScreen("network_scanner", "网络扫描器", Icons.Default.Lan, ScreenCategory.SECURITY)
    data object RfBugSweeper : ZeroDroidScreen("rf_bug_sweeper", "射频窃听器扫描", Icons.Default.BugReport, ScreenCategory.SECURITY)
    data object ProximityRadar : ZeroDroidScreen("proximity_radar", "近距雷达", Icons.Default.Radar, ScreenCategory.SECURITY)
    data object PrivacyScore : ZeroDroidScreen("privacy_score", "隐私评分", Icons.Default.Shield, ScreenCategory.SECURITY)
    data object DeauthDetector : ZeroDroidScreen("deauth_detector", "去认证攻击检测", Icons.Default.Router, ScreenCategory.SECURITY)
    data object EmfMapper : ZeroDroidScreen("emf_mapper", "电磁场测绘", Icons.Default.Dashboard, ScreenCategory.SENSORS)
    data object SignalLogger : ZeroDroidScreen("signal_logger", "信号记录器", Icons.Default.Timeline, ScreenCategory.SECURITY)
    data object AlertCenter : ZeroDroidScreen("alert_center", "警报中心", Icons.Default.NotificationsActive, ScreenCategory.SECURITY)

    companion object {
        val all: List<ZeroDroidScreen> = listOf(
            Dashboard, Sensors, Wifi, Ble, Nfc, Ir, Uwb, Usb, Sdr,
            Camera, Ultrasonic, Wardriving, WifiAware, CellTower, UsbCamera,
            Gps, BluetoothClassic, WifiDirect, HiddenCamera, GpsSpoofDetector,
            BluetoothTracker, RogueAp, NetworkScanner, RfBugSweeper, ProximityRadar,
            PrivacyScore, DeauthDetector, EmfMapper, SignalLogger, AlertCenter
        )
        val byCategory: Map<ScreenCategory, List<ZeroDroidScreen>> get() = all.groupBy { it.category }
    }
}
