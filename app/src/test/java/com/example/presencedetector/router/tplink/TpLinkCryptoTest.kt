package com.example.presencedetector.router.tplink

import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import javax.crypto.Cipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TpLinkCryptoTest {
  private val crypto = TpLinkCrypto()

  private fun rsaPair(bits: Int) =
    KeyPairGenerator.getInstance("RSA").apply { initialize(bits) }.generateKeyPair()

  private fun decrypt(hex: String, key: RSAPrivateKey, keyBytes: Int): String {
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding").apply { init(Cipher.DECRYPT_MODE, key) }
    return hex.chunked(keyBytes * 2).joinToString("") { block ->
      String(cipher.doFinal(block.chunked(2).map { it.toInt(16).toByte() }.toByteArray()))
    }
  }

  @Test
  fun `rsa output decrypts back to the plain text`() {
    val pair = rsaPair(1024)
    val pub = pair.public as RSAPublicKey
    val hex =
      crypto.rsaEncryptHex(
        "p@ssw0rd-ção",
        pub.modulus.toString(16),
        pub.publicExponent.toString(16),
      )
    assertEquals(256, hex.length)
    assertEquals("p@ssw0rd-ção", decrypt(hex, pair.private as RSAPrivateKey, 128))
  }

  @Test
  fun `rsa splits long text into 53 byte blocks for a 512 bit key`() {
    val pair = rsaPair(512)
    val pub = pair.public as RSAPublicKey
    val text = "k=1234567890123456&i=6543210987654321&h=0123456789abcdef0123456789abcdef&s=12345"
    val hex = crypto.rsaEncryptHex(text, pub.modulus.toString(16), pub.publicExponent.toString(16))
    assertEquals(2 * 128, hex.length)
    assertEquals(text, decrypt(hex, pair.private as RSAPrivateKey, 64))
  }

  @Test
  fun `aes round trip`() {
    val encrypted = crypto.aesEncrypt("operation=read", "1234567890123456", "6543210987654321")
    assertEquals(
      "operation=read",
      crypto.aesDecrypt(encrypted, "1234567890123456", "6543210987654321"),
    )
  }

  @Test
  fun `md5 matches the known vector`() {
    assertEquals("5f4dcc3b5aa765d61d8327deb882cf99", crypto.md5Hex("password"))
  }

  @Test
  fun `generated key material is 16 digits`() {
    repeat(20) { assertTrue(crypto.newDigits().matches(Regex("\\d{16}"))) }
  }
}
