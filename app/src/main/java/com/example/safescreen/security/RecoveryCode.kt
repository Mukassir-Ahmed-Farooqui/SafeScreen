package com.example.safescreen.security

import java.security.SecureRandom

/**
 * One-time recovery code for a forgotten PIN: 12 characters from a 32-letter
 * alphabet without look-alikes (no 0/O/1/I), about 60 bits of randomness,
 * shown as ABCD-EFGH-JKLM. Only a salted hash of it is stored.
 */
object RecoveryCode {
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 12

    fun generate(random: SecureRandom = SecureRandom()): String =
        String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

    fun format(raw: String): String = raw.chunked(4).joinToString("-")

    /** Upper-cases and drops dashes, spaces and anything outside the alphabet. */
    fun normalize(input: String): String = input.uppercase().filter { it in ALPHABET }

    fun isValid(input: String): Boolean = normalize(input).length == LENGTH
}
