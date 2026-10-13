package com.example.safescreen.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface PinResult {
    data object Success : PinResult
    data class Wrong(val attemptsLeft: Int) : PinResult
    data class LockedOut(val remainingMs: Long) : PinResult
    data object NotSet : PinResult
}

/**
 * Stores only salted PBKDF2 hashes: one for the PIN and one for the recovery
 * code, plus the failure counter and lockout deadline (so lockouts survive a
 * restart). Neither the PIN nor the recovery code is ever persisted or logged.
 * Wrong PINs and wrong recovery codes share one lockout counter.
 */
class PinManager(
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val hasPin: Flow<Boolean> = dataStore.data.map { it[HASH] != null }

    suspend fun hasRecoveryCodeNow(): Boolean = dataStore.data.first()[RECOVERY_HASH] != null

    suspend fun createPin(pin: String) {
        require(PinHasher.isValidPin(pin)) { "PIN must be ${PinHasher.PIN_LENGTH} digits" }
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        dataStore.edit {
            it[SALT] = encode(salt)
            it[HASH] = encode(hash)
            it[FAILURES] = 0
            it.remove(LOCKED_UNTIL)
        }
    }

    suspend fun verify(pin: String): PinResult = attempt(SALT, HASH, pin)

    /** Verifies [current] first; only replaces the PIN when it is correct. */
    suspend fun changePin(current: String, new: String): PinResult {
        val result = verify(current)
        if (result is PinResult.Success) createPin(new)
        return result
    }

    /**
     * Makes a new recovery code, replacing any old one, and returns it in
     * readable form (ABCD-EFGH-JKLM). This is the only time it exists in plain text.
     */
    suspend fun createRecoveryCode(): String {
        val raw = RecoveryCode.generate()
        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(raw, salt) }
        dataStore.edit {
            it[RECOVERY_SALT] = encode(salt)
            it[RECOVERY_HASH] = encode(hash)
        }
        return RecoveryCode.format(raw)
    }

    /** Sets a new PIN when [code] is the right recovery code. A wrong code counts toward the lockout. */
    suspend fun resetPinWithRecoveryCode(code: String, newPin: String): PinResult {
        require(PinHasher.isValidPin(newPin)) { "PIN must be ${PinHasher.PIN_LENGTH} digits" }
        val result = attempt(RECOVERY_SALT, RECOVERY_HASH, RecoveryCode.normalize(code))
        if (result is PinResult.Success) createPin(newPin)
        return result
    }

    private suspend fun attempt(
        saltKey: Preferences.Key<String>,
        hashKey: Preferences.Key<String>,
        secret: String,
    ): PinResult {
        val prefs = dataStore.data.first()
        val salt = prefs[saltKey]?.let(::decode)
        val expected = prefs[hashKey]?.let(::decode)
        if (salt == null || expected == null) return PinResult.NotSet

        val now = clock()
        val lockedUntil = prefs[LOCKED_UNTIL] ?: 0L
        if (lockedUntil > now) return PinResult.LockedOut(lockedUntil - now)

        val ok = withContext(Dispatchers.Default) { PinHasher.matches(secret, salt, expected) }
        if (ok) {
            dataStore.edit {
                it[FAILURES] = 0
                it.remove(LOCKED_UNTIL)
            }
            return PinResult.Success
        }

        val failures = (prefs[FAILURES] ?: 0) + 1
        val lockMs = PinLockout.durationMs(failures)
        dataStore.edit {
            it[FAILURES] = failures
            if (lockMs > 0) it[LOCKED_UNTIL] = now + lockMs
        }
        return if (lockMs > 0) PinResult.LockedOut(lockMs)
        else PinResult.Wrong(PinLockout.attemptsLeft(failures))
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)

    private companion object {
        val SALT = stringPreferencesKey("pin_salt")
        val HASH = stringPreferencesKey("pin_hash")
        val RECOVERY_SALT = stringPreferencesKey("recovery_salt")
        val RECOVERY_HASH = stringPreferencesKey("recovery_hash")
        val FAILURES = intPreferencesKey("pin_failures")
        val LOCKED_UNTIL = longPreferencesKey("pin_locked_until")
    }
}
