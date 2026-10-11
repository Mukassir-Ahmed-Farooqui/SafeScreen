package com.example.safescreen.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    private var now = 0L
    private val session = SessionManager { now }

    @Test fun startsLocked() = assertFalse(session.unlocked.value)

    @Test fun staysUnlockedForShortTrips() {
        session.unlock()
        session.onAppBackgrounded()
        now += SessionManager.AUTO_LOCK_GRACE_MS - 1
        session.onAppForegrounded()
        assertTrue(session.unlocked.value)
    }

    @Test fun relocksAfterLongAbsence() {
        session.unlock()
        session.onAppBackgrounded()
        now += SessionManager.AUTO_LOCK_GRACE_MS + 1
        session.onAppForegrounded()
        assertFalse(session.unlocked.value)
    }

    @Test fun explicitLockWorks() {
        session.unlock()
        session.lock()
        assertFalse(session.unlocked.value)
    }
}
