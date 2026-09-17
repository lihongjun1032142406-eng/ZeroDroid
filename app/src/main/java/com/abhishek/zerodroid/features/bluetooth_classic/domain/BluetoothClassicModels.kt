package com.abhishek.zerodroid.features.bluetooth_classic.domain

data class ClassicBluetoothDevice(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    val bondState: Int = 0,
    val majorClass: String = "未知",
    val minorClass: String = "",
    val isPaired: Boolean = false
) {
    val displayName: String get() = name ?: "未知设备"
    val bondStateLabel: String
        get() = when (bondState) {
            10 -> "未配对"
            11 -> "正在配对..."
            12 -> "已配对"
            else -> "未知"
        }
}

data class SppState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val lines: List<TerminalLine> = emptyList(),
    val error: String? = null
)

data class TerminalLine(
    val text: String,
    val isOutgoing: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class BluetoothClassicState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<ClassicBluetoothDevice> = emptyList(),
    val pairedDevices: List<ClassicBluetoothDevice> = emptyList(),
    val error: String? = null
)
