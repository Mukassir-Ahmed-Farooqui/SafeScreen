package com.example.safescreen.security

/**
 * Escalating lockout for wrong PINs: every 5th consecutive failure locks the
 * PIN pad for 30s, then 60s, 120s ... capped at 10 minutes.
 */
object PinLockout {
    const val ATTEMPTS_PER_ROUND = 5
    private const val BASE_MS = 30_000L
    private const val MAX_MS = 10 * 60_000L

    /** Lockout to apply after [consecutiveFailures] wrong PINs, or 0 if none is due yet. */
    fun durationMs(consecutiveFailures: Int): Long {
        if (consecutiveFailures <= 0 || consecutiveFailures % ATTEMPTS_PER_ROUND != 0) return 0L
        val round = consecutiveFailures / ATTEMPTS_PER_ROUND
        val shift = (round - 1).coerceAtMost(10)
        return (BASE_MS shl shift).coerceAtMost(MAX_MS)
    }

    fun attemptsLeft(consecutiveFailures: Int): Int =
        ATTEMPTS_PER_ROUND - (consecutiveFailures % ATTEMPTS_PER_ROUND)
}
