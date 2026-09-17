package com.abhishek.zerodroid.features.wifiaware.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abhishek.zerodroid.core.ui.TerminalCard
import com.abhishek.zerodroid.features.wifiaware.domain.WifiAwareState

@Composable
fun WifiAwareControlPanel(
    state: WifiAwareState,
    onServiceNameChange: (String) -> Unit,
    onAttach: () -> Unit,
    onDetach: () -> Unit,
    onTogglePublish: () -> Unit,
    onToggleSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

    TerminalCard(modifier = modifier) {
        Text(
            text = "> Wi-Fi Aware 控制",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.serviceName,
            onValueChange = onServiceNameChange,
            label = { Text("服务名称") },
            colors = fieldColors,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSessionAttached
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!state.isSessionAttached) {
            Button(
                onClick = onAttach,
                enabled = state.isAvailable,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (state.isAvailable) "连接会话" else "Wi-Fi Aware 不可用") }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTogglePublish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isPublishing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(if (state.isPublishing) "停止发布" else "发布") }

                Button(
                    onClick = onToggleSubscribe,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isSubscribing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text(if (state.isSubscribing) "停止订阅" else "订阅") }
            }

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onDetach,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) { Text("断开会话") }
        }
    }
}
