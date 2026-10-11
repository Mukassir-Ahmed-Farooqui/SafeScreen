package com.example.safescreen.domain

enum class Verdict { ALLOW, BLOCKED_BY_RULE, DAILY_LIMIT_REACHED }

/** Result of evaluating one app against its rule, with everything the UI needs to explain it. */
data class RestrictionDecision(
    val verdict: Verdict,
    val usedMillis: Long,
    val limitMinutes: Int,
) {
    val isRestricted: Boolean get() = verdict != Verdict.ALLOW

    private val limitMillis: Long get() = limitMinutes * 60_000L

    /** Time left today, or null when the app has no daily limit. */
    val remainingMillis: Long?
        get() = if (limitMinutes > 0) (limitMillis - usedMillis).coerceAtLeast(0L) else null

    /** 0..1 share of the daily limit used, or null when there is no limit. */
    val usageFraction: Float?
        get() = if (limitMinutes > 0) (usedMillis.toFloat() / limitMillis).coerceIn(0f, 1f) else null
}
