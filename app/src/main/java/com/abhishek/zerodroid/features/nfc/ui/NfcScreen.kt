package com.abhishek.zerodroid.features.nfc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.abhishek.zerodroid.core.ui.ScanningIndicator
import com.abhishek.zerodroid.core.ui.StatusIndicator
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.nfc.viewmodel.NfcViewModel

@Composable
fun NfcScreen(
    viewModel: NfcViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            StatusIndicator(isAvailable = state.isNfcAvailable)
        }

        if (!state.isNfcAvailable) {
            item {
                TerminalCard {
                    Text(
                        text = "> 此设备不支持 NFC",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (!state.isNfcEnabled) {
            item {
                TerminalCard {
                    Text(
                        text = "> NFC 已关闭，请在系统设置中开启。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !state.writeMode,
                    onClick = { viewModel.setWriteMode(false) },
                    label = { Text("读取") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                FilterChip(
                    selected = state.writeMode,
                    onClick = { viewModel.setWriteMode(true) },
                    label = { Text("写入") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (state.writeMode) {
            item {
                NfcWritePanel(
                    writeResult = state.writeResult,
                    onWriteText = viewModel::writeText,
                    onWriteUri = viewModel::writeUri
                )
            }
        } else {
            state.lastTag?.let { tag ->
                item {
                    Text(
                        text = "> 最近扫描的标签",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item { NfcTagCard(tag = tag) }
            }

            if (state.lastTag == null && state.isNfcAvailable && state.isNfcEnabled) {
                item {
                    TerminalCard(animated = true) {
                        ScanningIndicator(
                            isScanning = true,
                            label = "正在等待 NFC 标签…"
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "请将 NFC 标签靠近设备背面",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (state.tagHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "> 标签历史（${state.tagHistory.size}）",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(state.tagHistory) { tag -> NfcTagCard(tag = tag) }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
