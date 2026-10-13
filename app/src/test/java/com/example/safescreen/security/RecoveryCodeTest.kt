package com.example.safescreen.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeTest {
    @Test fun generatedCodeHasRightLengthAndAlphabet() {
        val code = RecoveryCode.generate()
        assertEquals(RecoveryCode.LENGTH, code.length)
        assertTrue(code.all { it in RecoveryCode.ALPHABET })
    }

    @Test fun twoCodesDiffer() {
        assertFalse(RecoveryCode.generate() == RecoveryCode.generate())
    }

    @Test fun formatsInGroupsOfFour() {
        assertEquals("ABCD-EFGH-JKLM", RecoveryCode.format("ABCDEFGHJKLM"))
    }

    @Test fun normalizeIgnoresCaseDashesAndSpaces() {
        assertEquals("ABCDEFGHJKLM", RecoveryCode.normalize("abcd-efgh jklm"))
    }

    @Test fun validatesLength() {
        assertTrue(RecoveryCode.isValid("abcd-efgh-jklm"))
        assertFalse(RecoveryCode.isValid("abcd-efgh"))
        assertFalse(RecoveryCode.isValid(""))
    }
}
