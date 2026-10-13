package com.example.safescreen.security

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory "parent is unlocked" flag. It is deliberately never persisted:
 * a killed process always comes back locked. The app also relocks when it has
 * been in the background longer than [AUTO_LOCK_GRACE_MS] (long enough to
 * visit Android settings and come back).
 */
class SessionManager(
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) {
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _pendingRecoveryCode = MutableStateFlow<String?>(null)

    /** A freshly generated recovery code waiting to be shown (once) and acknowledged. Memory only. */
    val pendingRecoveryCode: StateFlow<String?> = _pendingRecoveryCode.asStateFlow()

    private var backgroundedAt: Long? = null

    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }

    fun showRecoveryCode(code: String) { _pendingRecoveryCode.value = code }
    fun clearRecoveryCode() { _pendingRecoveryCode.value = null }

    fun onAppBackgrounded() {
        backgroundedAt = elapsedRealtime()
    }

    fun onAppForegrounded() {
        val since = backgroundedAt
        backgroundedAt = null
        if (since != null && elapsedRealtime() - since > AUTO_LOCK_GRACE_MS) lock()
    }

    companion object {
        const val AUTO_LOCK_GRACE_MS = 30_000L
    }
}
