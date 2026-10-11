package com.example.safescreen.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test fun acceptsOnlyFourDigits() {
        assertTrue(PinHasher.isValidPin("0123"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("12345"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertFalse(PinHasher.isValidPin(""))
    }

    @Test fun correctPinMatches() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)
        assertTrue(PinHasher.matches("4821", salt, hash))
    }

    @Test fun wrongPinDoesNotMatch() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)
        assertFalse(PinHasher.matches("4822", salt, hash))
    }

    @Test fun hashIsNotThePin() {
        val hash = PinHasher.hash("4821", PinHasher.newSalt())
        assertNotEquals("4821", String(hash))
        assertTrue(hash.size == 32)
    }

    @Test fun sameHashForSameInputs() {
        val salt = PinHasher.newSalt()
        assertArrayEquals(PinHasher.hash("1111", salt), PinHasher.hash("1111", salt))
    }

    @Test fun differentSaltGivesDifferentHash() {
        val a = PinHasher.hash("1111", PinHasher.newSalt())
        val b = PinHasher.hash("1111", PinHasher.newSalt())
        assertFalse(a.contentEquals(b))
    }
}
