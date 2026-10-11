package com.example.safescreen.domain

import com.example.safescreen.data.AppRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestrictionPolicyTest {

    private val minute = 60_000L

    private fun rule(blocked: Boolean = false, limit: Int = 0) =
        AppRule(packageName = "com.example.app", appName = "Example", isBlocked = blocked, dailyLimitMinutes = limit)

    @Test fun appWithoutRuleIsAllowed() {
        val decision = RestrictionPolicy.evaluate(null, 500 * minute)
        assertEquals(Verdict.ALLOW, decision.verdict)
        assertFalse(decision.isRestricted)
    }

    @Test fun explicitlyBlockedAppIsRestricted() {
        assertEquals(Verdict.BLOCKED_BY_RULE, RestrictionPolicy.evaluate(rule(blocked = true), 0).verdict)
    }

    @Test fun blockWinsOverLimit() {
        assertEquals(Verdict.BLOCKED_BY_RULE, RestrictionPolicy.evaluate(rule(blocked = true, limit = 60), 0).verdict)
    }

    @Test fun usageExactlyAtLimitIsRestricted() {
        assertEquals(Verdict.DAILY_LIMIT_REACHED, RestrictionPolicy.evaluate(rule(limit = 30), 30 * minute).verdict)
    }

    @Test fun usageAboveLimitIsRestricted() {
        assertEquals(Verdict.DAILY_LIMIT_REACHED, RestrictionPolicy.evaluate(rule(limit = 30), 45 * minute).verdict)
    }

    @Test fun usageBelowLimitIsAllowed() {
        assertEquals(Verdict.ALLOW, RestrictionPolicy.evaluate(rule(limit = 30), 29 * minute + 59_000).verdict)
    }

    @Test fun zeroLimitMeansNoLimit() {
        val decision = RestrictionPolicy.evaluate(rule(limit = 0), 10_000 * minute)
        assertEquals(Verdict.ALLOW, decision.verdict)
        assertNull(decision.remainingMillis)
        assertNull(decision.usageFraction)
    }

    @Test fun negativeLimitIsTreatedAsNoLimit() {
        assertEquals(Verdict.ALLOW, RestrictionPolicy.evaluate(rule(limit = -5), 100 * minute).verdict)
    }

    @Test fun oversizedLimitIsCappedToOneDay() {
        assertEquals(1440, RestrictionPolicy.evaluate(rule(limit = 5000), 0).limitMinutes)
    }

    @Test fun negativeUsageIsTreatedAsZero() {
        assertEquals(0L, RestrictionPolicy.evaluate(rule(limit = 30), -1000).usedMillis)
    }

    @Test fun remainingTimeNeverGoesNegative() {
        val decision = RestrictionPolicy.evaluate(rule(limit = 10), 20 * minute)
        assertEquals(0L, decision.remainingMillis)
        assertTrue(decision.isRestricted)
    }

    @Test fun usageFractionIsClampedToOne() {
        assertEquals(1f, RestrictionPolicy.evaluate(rule(limit = 10), 20 * minute).usageFraction)
    }
}
