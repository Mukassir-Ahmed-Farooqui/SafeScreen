package com.example.safescreen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageWarningsTest {
    private val m = 60_000L

    @Test fun noWarningWhileMoreThanTheBiggestThresholdRemains() {
        assertNull(UsageWarnings.next(30 * m, 60, emptySet()).threshold)
    }

    @Test fun warnsAtTenFiveAndOneMinutes() {
        var step = UsageWarnings.next(10 * m, 60, emptySet())
        assertEquals(10, step.threshold)
        step = UsageWarnings.next(9 * m, 60, step.announced)
        assertNull(step.threshold)
        step = UsageWarnings.next(5 * m, 60, step.announced)
        assertEquals(5, step.threshold)
        step = UsageWarnings.next(1 * m, 60, step.announced)
        assertEquals(1, step.threshold)
        step = UsageWarnings.next(30_000L, 60, step.announced)
        assertNull(step.threshold)
    }

    @Test fun neverWarnsAtOrAboveTheLimit() {
        // limit 10 -> only 5 and 1 apply
        assertNull(UsageWarnings.next(9 * m, 10, emptySet()).threshold)
        assertEquals(5, UsageWarnings.next(5 * m, 10, emptySet()).threshold)
    }

    @Test fun jumpingStraightToOneMinuteSilencesTheBiggerOnes() {
        val step = UsageWarnings.next(30_000L, 60, emptySet())
        assertEquals(1, step.threshold)
        assertEquals(setOf(10, 5, 1), step.announced)
    }

    @Test fun nothingWhenTimeIsUp() {
        assertNull(UsageWarnings.next(0L, 60, emptySet()).threshold)
    }

    @Test fun raisingTheLimitMakesWarningsPossibleAgain() {
        val after = UsageWarnings.next(8 * m, 30, setOf(5, 1))
        assertEquals(10, after.threshold)
    }

    @Test fun oneMinuteLimitHasNoWarnings() {
        assertNull(UsageWarnings.next(30_000L, 1, emptySet()).threshold)
    }

    @Test fun wording() {
        assertEquals("1 minute", UsageWarnings.describe(1))
        assertEquals("5 minutes", UsageWarnings.describe(5))
    }
}
