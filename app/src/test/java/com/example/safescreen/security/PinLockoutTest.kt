package com.example.safescreen.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinLockoutTest {
    @Test fun noLockoutBeforeFifthFailure() {
        for (failures in 0..4) assertEquals(0L, PinLockout.durationMs(failures))
    }

    @Test fun lockoutEscalatesEveryFiveFailures() {
        assertEquals(30_000L, PinLockout.durationMs(5))
        assertEquals(60_000L, PinLockout.durationMs(10))
        assertEquals(120_000L, PinLockout.durationMs(15))
    }

    @Test fun lockoutIsCappedAtTenMinutes() {
        assertEquals(600_000L, PinLockout.durationMs(100))
    }

    @Test fun attemptsLeftCountsDownWithinARound() {
        assertEquals(5, PinLockout.attemptsLeft(0))
        assertEquals(4, PinLockout.attemptsLeft(1))
        assertEquals(1, PinLockout.attemptsLeft(4))
        assertEquals(5, PinLockout.attemptsLeft(5))
    }
}
