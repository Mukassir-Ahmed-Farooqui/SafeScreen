package com.example.safescreen.monitoring

import com.example.safescreen.monitoring.UsageSessions.ACTIVITY_PAUSED
import com.example.safescreen.monitoring.UsageSessions.ACTIVITY_RESUMED
import com.example.safescreen.monitoring.UsageSessions.KEYGUARD_SHOWN
import com.example.safescreen.monitoring.UsageSessions.SCREEN_NON_INTERACTIVE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageSessionsTest {
    private val m = 60_000L
    private fun e(type: Int, pkg: String?, minutes: Long) = RawUsageEvent(type, pkg, minutes * m)

    @Test fun simpleSession() {
        val t = UsageSessions.totals(listOf(e(ACTIVITY_RESUMED, "a", 0), e(ACTIVITY_PAUSED, "a", 10)), 20 * m)
        assertEquals(10 * m, t["a"])
    }

    @Test fun missingPauseNeverOvercounts() {
        val t = UsageSessions.totals(
            listOf(e(ACTIVITY_RESUMED, "a", 0), e(ACTIVITY_RESUMED, "b", 10), e(ACTIVITY_PAUSED, "b", 12)),
            60 * m,
        )
        assertEquals(10 * m, t["a"])
        assertEquals(2 * m, t["b"])
    }

    @Test fun screenOffClosesTheSession() {
        val t = UsageSessions.totals(listOf(e(ACTIVITY_RESUMED, "a", 0), e(SCREEN_NON_INTERACTIVE, null, 5)), 600 * m)
        assertEquals(5 * m, t["a"])
    }

    @Test fun keyguardClosesTheSession() {
        val t = UsageSessions.totals(
            listOf(e(ACTIVITY_RESUMED, "a", 0), e(KEYGUARD_SHOWN, null, 1), e(ACTIVITY_RESUMED, "b", 100)),
            101 * m,
        )
        assertEquals(1 * m, t["a"])
        assertEquals(1 * m, t["b"])
    }

    @Test fun sessionStillOpenCountsUntilNow() {
        assertEquals(5 * m, UsageSessions.totals(listOf(e(ACTIVITY_RESUMED, "a", 50)), 55 * m)["a"])
    }

    @Test fun switchingActivitiesInsideOneAppAddsUp() {
        val t = UsageSessions.totals(
            listOf(
                e(ACTIVITY_RESUMED, "a", 0), e(ACTIVITY_PAUSED, "a", 3),
                e(ACTIVITY_RESUMED, "a", 3), e(ACTIVITY_PAUSED, "a", 6),
            ),
            10 * m,
        )
        assertEquals(6 * m, t["a"])
    }

    @Test fun pauseOfAnotherAppIsIgnored() {
        val t = UsageSessions.totals(
            listOf(e(ACTIVITY_RESUMED, "a", 0), e(ACTIVITY_PAUSED, "b", 2), e(ACTIVITY_PAUSED, "a", 4)),
            10 * m,
        )
        assertEquals(4 * m, t["a"])
        assertNull(t["b"])
    }

    @Test fun noEventsMeansNoUsage() {
        assertTrue(UsageSessions.totals(emptyList(), 10 * m).isEmpty())
    }
}
