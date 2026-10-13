package com.example.safescreen.domain

/** Result of one warning check: which threshold (in minutes) to announce now, if any, plus the updated memory. */
data class WarningStep(val threshold: Int?, val announced: Set<Int>)

/**
 * Decides when to say "N minutes left". Thresholds are 10, 5 and 1 minutes, and
 * only those strictly below the limit are used (a 10 minute limit never says
 * "10 minutes left" at the very start).
 */
object UsageWarnings {
    val THRESHOLD_MINUTES = listOf(10, 5, 1)

    fun next(remainingMillis: Long, limitMinutes: Int, announced: Set<Int>): WarningStep {
        val eligible = THRESHOLD_MINUTES.filter { it < limitMinutes }
        // Forget thresholds the remaining time is above again (limit raised, usage reset).
        val remembered = announced.filter { remainingMillis <= it * 60_000L }.toSet()
        if (remainingMillis <= 0L) return WarningStep(null, remembered)

        val due = eligible
            .filter { remainingMillis <= it * 60_000L && it !in remembered }
            .minOrNull()
            ?: return WarningStep(null, remembered)
        // Announcing "1 minute" also silences the bigger ones we jumped over.
        return WarningStep(due, remembered + eligible.filter { it >= due })
    }

    fun describe(minutes: Int): String = if (minutes == 1) "1 minute" else "$minutes minutes"
}
