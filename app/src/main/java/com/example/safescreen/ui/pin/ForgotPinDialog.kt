package com.example.safescreen.ui.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.safescreen.security.PinHasher
import com.example.safescreen.security.PinResult
import com.example.safescreen.security.RecoveryCode
import kotlin.math.ceil
import kotlinx.coroutines.launch

/** Reset a forgotten PIN with the recovery code that was shown when the PIN was created. */
@Composable
fun ForgotPinDialog(
    onDismiss: () -> Unit,
    onSubmit: suspend (code: String, newPin: String) -> PinResult,
) {
    var code by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Reset your PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter the recovery code you saved when you created your PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { v -> if (v.length <= 14 && v.all { it.isLetterOrDigit() || it == '-' || it == ' ' }) code = v },
                    label = { Text("Recovery code") },
                    placeholder = { Text("ABCD-EFGH-JKLM") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                    ),
                )
                PinBox("New PIN", newPin) { newPin = it }
                PinBox("Confirm new PIN", confirm) { confirm = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy,
                onClick = {
                    when {
                        !RecoveryCode.isValid(code) -> error = "The recovery code has 12 letters and numbers, like ABCD-EFGH-JKLM."
                        !PinHasher.isValidPin(newPin) -> error = "Use exactly ${PinHasher.PIN_LENGTH} digits for the new PIN."
                        newPin != confirm -> error = "The new PINs don't match."
                        else -> {
                            busy = true
                            error = null
                            scope.launch {
                                when (val result = onSubmit(code, newPin)) {
                                    PinResult.Success -> onDismiss()
                                    is PinResult.Wrong -> error = "That code isn't right. ${result.attemptsLeft} attempts left."
                                    is PinResult.LockedOut -> error = "Too many attempts. Try again in ${ceil(result.remainingMs / 1000.0).toInt()}s."
                                    PinResult.NotSet -> error = "No recovery code was saved on this phone. " +
                                        "The only way back in is clearing SafeScreen's data in Android settings, which also removes all rules."
                                }
                                busy = false
                            }
                        }
                    }
                },
            ) { Text("Reset PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}

@Composable
private fun PinBox(label: String, value: String, onChange: (String) -> Unit) {
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
