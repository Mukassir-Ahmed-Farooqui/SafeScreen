package com.example.safescreen.domain

import com.example.safescreen.data.AppRule
import com.example.safescreen.data.effectiveLimitMinutes

/**
 * Pure decision logic: no Android, no Compose, no I/O. That is what makes it
 * trivially unit-testable and safe to call from the accessibility service.
 *
 * Rules, in priority order:
 *  1. No rule                         -> ALLOW
 *  2. Explicitly blocked              -> BLOCKED_BY_RULE (never overridden)
 *  3. Limit > 0 and used >= limit     -> DAILY_LIMIT_REACHED
 *  4. Otherwise                       -> ALLOW
 *
 * A limit of 0 (or a negative value) means "no limit", not "zero minutes".
 */
object RestrictionPolicy {

    fun evaluate(rule: AppRule?, usedMillis: Long): RestrictionDecision {
        val used = usedMillis.coerceAtLeast(0L)
        if (rule == null) return RestrictionDecision(Verdict.ALLOW, used, limitMinutes = 0)

        val limit = rule.effectiveLimitMinutes
        val verdict = when {
            rule.isBlocked -> Verdict.BLOCKED_BY_RULE
            limit > 0 && used >= limit * 60_000L -> Verdict.DAILY_LIMIT_REACHED
            else -> Verdict.ALLOW
        }
        return RestrictionDecision(verdict, used, limit)
    }
}
