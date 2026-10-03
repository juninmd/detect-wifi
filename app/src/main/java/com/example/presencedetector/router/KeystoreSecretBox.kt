package com.example.presencedetector.router

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts small secrets (the router password) with a non-exportable Android Keystore key. */
object KeystoreSecretBox {
  private const val ALIAS = "detect_wifi_secret_box"
  private const val PROVIDER = "AndroidKeyStore"
  private const val TRANSFORM = "AES/GCM/NoPadding"
  private const val IV_BYTES = 12
  private const val TAG_BITS = 128

  fun encrypt(plain: String): String {
    val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
    return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray()), Base64.NO_WRAP)
  }

  /** Returns null when the value cannot be read (for example after a backup restore). */
  fun decrypt(token: String): String? = runCatching {
    val bytes = Base64.decode(token, Base64.NO_WRAP)
    val spec = GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES)
    val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), spec) }
    String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES))
  }
    .getOrNull()

  private fun key(): SecretKey {
    val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
    (store.getKey(ALIAS, null) as? SecretKey)?.let {
      return it
    }
    val spec =
      KeyGenParameterSpec.Builder(
          ALIAS,
          KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
      .apply { init(spec) }
      .generateKey()
  }
}
