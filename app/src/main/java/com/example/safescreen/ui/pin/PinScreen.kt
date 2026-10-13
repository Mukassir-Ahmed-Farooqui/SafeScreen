package com.example.safescreen.ui.pin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safescreen.security.PinHasher
import kotlin.math.roundToInt

@Composable
fun PinScreen(mode: PinMode, viewModel: PinViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val shake = remember { Animatable(0f) }
    var showForgot by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorTick) {
        if (state.errorTick > 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            repeat(3) {
                shake.animateTo(14f, tween(45))
                shake.animateTo(-14f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
    }

    val title = when {
        mode == PinMode.UNLOCK -> "Enter parent PIN"
        state.confirming -> "Confirm your PIN"
        else -> "Create a parent PIN"
    }
    val subtitle = when {
        mode == PinMode.UNLOCK -> "Only a parent can change app rules."
        state.confirming -> "Enter the same 4 digits again."
        else -> "Choose 4 digits. You'll need it to open SafeScreen."
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.offset { IntOffset(shake.value.dp.roundToPx(), 0) },
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            repeat(PinHasher.PIN_LENGTH) { index -> PinDot(filled = index < state.entered.length, isError = state.message != null && state.entered.isEmpty()) }
        }

        Box(Modifier.height(48.dp).padding(top = 14.dp), contentAlignment = Alignment.Center) {
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(12.dp))

        Keypad(
            enabled = !state.busy && state.lockedSeconds == 0,
            onDigit = { viewModel.onDigit(mode, it) },
            onDelete = viewModel::onDelete,
        )

        if (mode == PinMode.UNLOCK) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showForgot = true }) { Text("Forgot PIN?") }
        }
    }

    if (showForgot) {
        ForgotPinDialog(
            onDismiss = { showForgot = false },
            onSubmit = { code, newPin -> viewModel.resetWithRecoveryCode(code, newPin) },
        )
    }
}

@Composable
private fun PinDot(filled: Boolean, isError: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val color by animateColorAsState(
        targetValue = when {
            isError -> scheme.error
            filled -> scheme.primary
            else -> scheme.surfaceVariant
        },
        label = "dot-color",
    )
    val scale by animateFloatAsState(if (filled) 1.15f else 1f, label = "dot-scale")
    Box(Modifier.size(16.dp).scale(scale).clip(CircleShape).background(color))
}

@Composable
private fun Keypad(enabled: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { digit -> KeyButton(digit.toString(), enabled) { onDigit(digit) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Spacer(Modifier.size(KEY_SIZE))
            KeyButton("0", enabled) { onDigit('0') }
            KeyButton("⌫", enabled, onClick = onDelete)
        }
    }
}

private val KEY_SIZE = 76.dp

@Composable
private fun KeyButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(KEY_SIZE)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.headlineSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
