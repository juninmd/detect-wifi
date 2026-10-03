package com.example.presencedetector.lock

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing for the app-lock PIN. Only the salt and hash are ever persisted. */
object PinHasher {
  private const val ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val ITERATIONS = 120_000
  private const val KEY_BITS = 256
  private const val SALT_BYTES = 16

  data class Hashed(val salt: String, val hash: String)

  fun hash(pin: String, salt: ByteArray = newSalt()): Hashed =
    Hashed(encode(salt), encode(derive(pin, salt)))

  fun verify(pin: String, stored: Hashed): Boolean {
    val expected = Base64.getDecoder().decode(stored.hash)
    val actual = derive(pin, Base64.getDecoder().decode(stored.salt))
    return MessageDigest.isEqual(expected, actual)
  }

  private fun newSalt() = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

  private fun derive(pin: String, salt: ByteArray): ByteArray =
    SecretKeyFactory.getInstance(ALGORITHM)
      .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS))
      .encoded

  private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
}
