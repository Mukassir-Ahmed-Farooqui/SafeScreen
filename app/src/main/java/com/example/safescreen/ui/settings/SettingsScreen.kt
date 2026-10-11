package com.example.safescreen.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safescreen.monitoring.PermissionChecker
import com.example.safescreen.security.PinHasher
import com.example.safescreen.security.PinResult
import com.example.safescreen.ui.components.SectionTitle
import com.example.safescreen.ui.components.SoftCard
import com.example.safescreen.ui.containerFactory
import com.example.safescreen.ui.theme.PtyxisPalette
import kotlin.math.ceil
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = containerFactory { SettingsViewModel(it) }),
) {
    val palette by viewModel.palette.collectAsStateWithLifecycle()
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showChangePin by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        SectionTitle("Appearance")
        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PtyxisPalette.entries.forEach { option ->
                    PaletteOption(option, selected = option == palette) { viewModel.setPalette(option) }
                }
            }
        }

        SectionTitle("Permissions")
        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionRow(
                    title = "Usage access",
                    body = "Measures how long each app is used today.",
                    granted = permissions.usageAccess,
                    actionLabel = "Open settings",
                    onAction = { PermissionChecker.openUsageAccessSettings(context) },
                )
                PermissionRow(
                    title = "Blocking service",
                    body = "Notices which app is open and covers restricted ones.",
                    granted = permissions.accessibility,
                    actionLabel = "Open settings",
                    onAction = { PermissionChecker.openAccessibilitySettings(context) },
                )
            }
        }

        SectionTitle("Security")
        SoftCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { showChangePin = true }, modifier = Modifier.fillMaxWidth()) { Text("Change PIN") }
                OutlinedButton(onClick = viewModel::lockNow, modifier = Modifier.fillMaxWidth()) { Text("Lock SafeScreen now") }
            }
        }

        SectionTitle("About")
        SoftCard {
            Text(
                "SafeScreen keeps everything on this device. Rules, usage and your PIN hash are never sent anywhere. " +
                    "Blocking works through an accessibility service you switch on yourself, and it can be switched off " +
                    "in Android settings, so treat it as a guardrail rather than a guarantee.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showChangePin) {
        ChangePinDialog(
            onDismiss = { showChangePin = false },
            onSubmit = { current, new -> viewModel.changePin(current, new) },
        )
    }
}

@Composable
private fun PaletteOption(option: PtyxisPalette, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(option.background),
        border = BorderStroke(2.dp, if (selected) scheme.primary else Color.Transparent),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(option.primaryC, option.secondaryC, option.tertiaryC).forEach {
                Box(Modifier.size(18.dp).clip(CircleShape).background(Color(it)))
            }
            Text(option.displayName, color = Color(option.foreground), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PermissionRow(title: String, body: String, granted: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                if (granted) "On. $body" else "Off. $body",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (granted) Icon(Icons.Filled.CheckCircle, contentDescription = "On", tint = MaterialTheme.colorScheme.primary)
        else FilledTonalButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun ChangePinDialog(onDismiss: () -> Unit, onSubmit: suspend (current: String, new: String) -> PinResult) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Change PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PinField("Current PIN", current) { current = it }
                PinField("New PIN", newPin) { newPin = it }
                PinField("Confirm new PIN", confirm) { confirm = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    when {
                        !PinHasher.isValidPin(newPin) -> error = "Use exactly ${PinHasher.PIN_LENGTH} digits for the new PIN."
                        newPin != confirm -> error = "The new PINs don't match."
                        else -> {
                            busy = true
                            error = null
                            scope.launch {
                                when (val result = onSubmit(current, newPin)) {
                                    PinResult.Success -> onDismiss()
                                    is PinResult.Wrong -> error = "Current PIN is wrong. ${result.attemptsLeft} attempts left."
                                    is PinResult.LockedOut -> error = "Too many attempts. Try again in ${ceil(result.remainingMs / 1000.0).toInt()}s."
                                    PinResult.NotSet -> error = "No PIN is set yet."
                                }
                                busy = false
                            }
                        }
                    }
                },
            ) { Text("Save PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> if (v.length <= PinHasher.PIN_LENGTH && v.all(Char::isDigit)) onChange(v) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}
