package com.example.safescreen.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safescreen.SafeScreenApp
import com.example.safescreen.ui.pin.PinMode
import com.example.safescreen.ui.pin.PinScreen
import com.example.safescreen.ui.pin.PinViewModel
import com.example.safescreen.ui.pin.RecoveryCodeDialog

private enum class Gate { LOADING, SETUP, LOCKED, UNLOCKED }

/** Decides what the parent sees: PIN setup, PIN unlock, or the app itself. */
@Composable
fun SafeScreenRoot() {
    val container = (LocalContext.current.applicationContext as SafeScreenApp).container
    val hasPin by container.pinManager.hasPin.collectAsStateWithLifecycle(initialValue = null)
    val unlocked by container.sessionManager.unlocked.collectAsStateWithLifecycle()
    val pendingRecoveryCode by container.sessionManager.pendingRecoveryCode.collectAsStateWithLifecycle()
    val pinViewModel: PinViewModel = viewModel(factory = containerFactory { PinViewModel(it) })

    val gate = when {
        hasPin == null -> Gate.LOADING
        hasPin == false -> Gate.SETUP
        !unlocked -> Gate.LOCKED
        else -> Gate.UNLOCKED
    }

    Box(Modifier.fillMaxSize()) {
        Crossfade(targetState = gate, label = "gate") { current ->
            when (current) {
                Gate.LOADING -> Box(Modifier.fillMaxSize().safeDrawingPadding())
                Gate.SETUP -> PinScreen(PinMode.SETUP, pinViewModel, Modifier.safeDrawingPadding())
                Gate.LOCKED -> PinScreen(PinMode.UNLOCK, pinViewModel, Modifier.safeDrawingPadding())
                Gate.UNLOCKED -> MainScaffold()
            }
        }
    }

    val recoveryCode = pendingRecoveryCode
    if (recoveryCode != null && gate == Gate.UNLOCKED) {
        RecoveryCodeDialog(code = recoveryCode, onSaved = container.sessionManager::clearRecoveryCode)
    }
}
