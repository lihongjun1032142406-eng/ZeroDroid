package com.abhishek.zerodroid.features.ir.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.ir.domain.IrScreenTab
import com.abhishek.zerodroid.features.ir.viewmodel.IrViewModel

@Composable
fun IrScreen(viewModel: IrViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = state.isIrAvailable)
        }

        if (!state.isIrAvailable) {
            item {
                TerminalCard {
                    Text(text = "> 未检测到红外发射器", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "此设备没有红外发射器", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (state.isIrAvailable) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.activeTab == IrScreenTab.REMOTE, onClick = { viewModel.setActiveTab(IrScreenTab.REMOTE) },
                        label = { Text("遥控器", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(imageVector = Icons.Default.SettingsRemote, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), selectedLabelColor = MaterialTheme.colorScheme.primary, selectedLeadingIconColor = MaterialTheme.colorScheme.primary))
                    FilterChip(selected = state.activeTab == IrScreenTab.CUSTOM, onClick = { viewModel.setActiveTab(IrScreenTab.CUSTOM) },
                        label = { Text("自定义", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), selectedLabelColor = MaterialTheme.colorScheme.primary, selectedLeadingIconColor = MaterialTheme.colorScheme.primary))
                    FilterChip(selected = state.activeTab == IrScreenTab.IMPORT, onClick = { viewModel.setActiveTab(IrScreenTab.IMPORT) },
                        label = { Text("导入", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), selectedLabelColor = MaterialTheme.colorScheme.primary, selectedLeadingIconColor = MaterialTheme.colorScheme.primary))
                }
            }

            when (state.activeTab) {
                IrScreenTab.REMOTE -> {
                    item {
                        Text(
                            text = "发送所选品牌的标准行业红外码。红外通信是单向的，无需配对或扫描。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item { IrRemoteGridPanel(selectedProfile = state.selectedProfile, lastTransmitResult = state.lastTransmitResult,
                        onProfileSelected = viewModel::selectProfile, onButtonPress = viewModel::transmitRemoteButton) }
                }
                IrScreenTab.CUSTOM -> {
                    item { IrTransmitPanel(state = state, onProtocolChange = viewModel::setProtocol, onFrequencyChange = viewModel::setFrequency,
                        onCodeChange = viewModel::setCode, onTransmit = viewModel::transmit) }
                }
                IrScreenTab.IMPORT -> {
                    item { IrImportPanel(signals = state.importedSignals, onImportFile = viewModel::importFlipperFile, onTransmitSignal = viewModel::transmitSignal) }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
