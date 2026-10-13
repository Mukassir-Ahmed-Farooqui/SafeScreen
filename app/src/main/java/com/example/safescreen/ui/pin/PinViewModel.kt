package com.example.safescreen.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safescreen.AppContainer
import com.example.safescreen.security.PinHasher
import com.example.safescreen.security.PinResult
import kotlin.math.ceil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinMode { SETUP, UNLOCK }

data class PinUiState(
    val entered: String = "",
    val confirming: Boolean = false,
    val message: String? = null,
    val errorTick: Int = 0,
    val lockedSeconds: Int = 0,
    val busy: Boolean = false,
)

class PinViewModel(container: AppContainer) : ViewModel() {
    private val pinManager = container.pinManager
    private val session = container.sessionManager

    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    private var firstEntry: String? = null
    private var countdown: Job? = null

    fun onDigit(mode: PinMode, digit: Char) {
        val current = _state.value
        if (current.busy || current.lockedSeconds > 0 || current.entered.length >= PinHasher.PIN_LENGTH) return
        val entered = current.entered + digit
        _state.update { it.copy(entered = entered, message = null) }
        if (entered.length == PinHasher.PIN_LENGTH) submit(mode, entered)
    }

    fun onDelete() {
        _state.update { if (it.busy || it.lockedSeconds > 0) it else it.copy(entered = it.entered.dropLast(1)) }
    }

    private fun submit(mode: PinMode, pin: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                when (mode) {
                    PinMode.SETUP -> submitSetup(pin)
                    PinMode.UNLOCK -> submitUnlock(pin)
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun submitSetup(pin: String) {
        val first = firstEntry
        when {
            first == null -> {
                firstEntry = pin
                _state.value = PinUiState(confirming = true)
            }
            first == pin -> {
                firstEntry = null
                session.unlock() // safe: the setup screen is shown until the PIN exists
                pinManager.createPin(pin)
                session.showRecoveryCode(pinManager.createRecoveryCode())
                _state.value = PinUiState()
            }
            else -> {
                firstEntry = null
                fail("Those PINs didn't match. Start again.")
            }
        }
    }

    private suspend fun submitUnlock(pin: String) {
        when (val result = pinManager.verify(pin)) {
            PinResult.Success -> {
                session.unlock()
                // Installs created before recovery codes existed get one on their next unlock.
                if (!pinManager.hasRecoveryCodeNow()) session.showRecoveryCode(pinManager.createRecoveryCode())
                _state.value = PinUiState()
            }
            is PinResult.Wrong -> fail("Wrong PIN. ${result.attemptsLeft} attempts left.")
            is PinResult.LockedOut -> startLockout(result.remainingMs)
            PinResult.NotSet -> fail("No PIN is set yet.")
        }
    }

    /**
     * "Forgot PIN": a correct recovery code sets the new PIN and unlocks. The
     * code is single-use, so a fresh one is issued right away.
     */
    suspend fun resetWithRecoveryCode(code: String, newPin: String): PinResult {
        val result = pinManager.resetPinWithRecoveryCode(code, newPin)
        if (result is PinResult.Success) {
            session.unlock()
            session.showRecoveryCode(pinManager.createRecoveryCode())
            _state.value = PinUiState()
        }
        return result
    }

    private fun fail(message: String) {
        _state.update { PinUiState(message = message, errorTick = it.errorTick + 1) }
    }

    private fun startLockout(remainingMs: Long) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            var seconds = ceil(remainingMs / 1000.0).toInt()
            _state.update { it.copy(errorTick = it.errorTick + 1) }
            while (seconds > 0) {
                _state.update {
                    it.copy(
                        entered = "",
                        lockedSeconds = seconds,
                        message = "Too many attempts. Try again in ${seconds}s.",
                    )
                }
                delay(1_000)
                seconds--
            }
            _state.update { it.copy(lockedSeconds = 0, message = null) }
        }
    }
}
