package com.mtkclientandroid.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mtkclientandroid.engine.ConnectionMode
import com.mtkclientandroid.engine.DeviceUiState
import com.mtkclientandroid.ui.theme.StatusAmber
import com.mtkclientandroid.ui.theme.StatusBlue
import com.mtkclientandroid.ui.theme.StatusGray
import com.mtkclientandroid.ui.theme.StatusGreen
import com.mtkclientandroid.ui.theme.StatusRed

@Composable
fun StatusIndicator(state: DeviceUiState, modifier: Modifier = Modifier) {
    val (label, color, pulse) = when (state) {
        DeviceUiState.Disconnected -> Triple("No device", StatusGray, false)
        DeviceUiState.Detecting -> Triple("Detecting", StatusAmber, true)
        is DeviceUiState.Connected -> {
            val mode = when (state.mode) {
                ConnectionMode.PRELOADER -> "Preloader"
                ConnectionMode.BOOTROM -> "BootROM"
                ConnectionMode.DA -> "DA"
                null -> null
            }
            Triple(if (mode != null) "Connected · $mode" else "Connected", StatusGreen, false)
        }
        is DeviceUiState.Running -> {
            val pct = state.progress
            val text = if (pct != null) "Running · $pct%" else "Running"
            Triple(text, StatusBlue, false)
        }
        is DeviceUiState.Error -> Triple(
            if (state.message.equals("Connection lost", true)) "Connection lost" else "Error",
            StatusRed,
            false
        )
    }
    val alpha = if (pulse) {
        val t = rememberInfiniteTransition(label = "status-pulse")
        val a by t.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulse"
        )
        a
    } else 1f
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            if (state is DeviceUiState.Running && state.progressLabel.isNotBlank()) {
                Text(
                    state.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box(
            Modifier
                .size(10.dp)
                .alpha(alpha)
                .background(color, CircleShape)
        )
    }
}
