package com.example.presencedetector.router.tplink

import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Primitives used by the encrypted web API of TP-Link Archer/Deco style firmwares. */
open class TpLinkCrypto(private val random: SecureRandom = SecureRandom()) {
  /** RSA PKCS#1 v1.5, encrypting in blocks the way the router's JavaScript does; hex output. */
  fun rsaEncryptHex(plain: String, modulusHex: String, exponentHex: String): String {
    val key =
      KeyFactory.getInstance("RSA")
        .generatePublic(RSAPublicKeySpec(BigInteger(modulusHex, 16), BigInteger(exponentHex, 16)))
    val keyBytes = (modulusHex.length + 1) / 2
    val chunk = keyBytes - PKCS1_OVERHEAD
    val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding").apply { init(Cipher.ENCRYPT_MODE, key) }
    return plain.toByteArray(Charsets.UTF_8).toList().chunked(chunk).joinToString("") { block ->
      cipher
        .doFinal(block.toByteArray())
        .joinToString("") { "%02x".format(it) }
        .padStart(keyBytes * 2, '0')
    }
  }

  fun aesEncrypt(plain: String, key: String, iv: String): String =
    Base64.getEncoder()
      .encodeToString(aes(Cipher.ENCRYPT_MODE, key, iv).doFinal(plain.toByteArray()))

  fun aesDecrypt(base64: String, key: String, iv: String): String =
    String(aes(Cipher.DECRYPT_MODE, key, iv).doFinal(Base64.getDecoder().decode(base64.trim())))

  fun md5Hex(text: String): String =
    MessageDigest.getInstance("MD5").digest(text.toByteArray()).joinToString("") {
      "%02x".format(it)
    }

  /** The router expects the AES key and IV as 16 numeric characters. */
  open fun newDigits(): String = buildString { repeat(DIGITS) { append(random.nextInt(10)) } }

  private fun aes(mode: Int, key: String, iv: String): Cipher =
    Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
      init(mode, SecretKeySpec(key.toByteArray(), "AES"), IvParameterSpec(iv.toByteArray()))
    }

  private companion object {
    const val PKCS1_OVERHEAD = 11
    const val DIGITS = 16
  }
}
