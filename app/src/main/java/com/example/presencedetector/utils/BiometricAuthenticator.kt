package com.example.presencedetector.utils

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthenticator(private val activity: FragmentActivity) {

  fun authenticate(onSuccess: () -> Unit, onFail: (() -> Unit)? = null) {
    val biometricManager = BiometricManager.from(activity)
    val authenticators =
      BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    when (biometricManager.canAuthenticate(authenticators)) {
      BiometricManager.BIOMETRIC_SUCCESS -> {
        showPrompt(onSuccess, onFail)
      }
      BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
        Toast.makeText(activity, "No biometric hardware found", Toast.LENGTH_SHORT).show()
        onFail?.invoke()
      }
      BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
        Toast.makeText(activity, "Biometric hardware unavailable", Toast.LENGTH_SHORT).show()
        onFail?.invoke()
      }
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
        // Prompt user to set up device lock
        Toast.makeText(activity, "Please set up a lock screen or fingerprint", Toast.LENGTH_LONG)
          .show()
        onFail?.invoke()
      }
      else -> {
        Toast.makeText(activity, "Biometric authentication unavailable", Toast.LENGTH_SHORT).show()
        onFail?.invoke()
      }
    }
  }

  private fun showPrompt(onSuccess: () -> Unit, onFail: (() -> Unit)?) {
    val executor = ContextCompat.getMainExecutor(activity)

    val callback =
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          // Error 13 is usually "Canceled by user" (touching outside)
          if (
            errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
              errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
          ) {
            Toast.makeText(activity, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
          }
          onFail?.invoke()
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          // Biometric is valid but not recognized (wrong finger)
        }
      }

    val promptInfo =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authentication Required")
        .setSubtitle("Confirm it's you to proceed")
        .setAllowedAuthenticators(
          BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    biometricPrompt.authenticate(promptInfo)
  }

  companion object {
    fun isAvailable(context: Context): Boolean {
      val biometricManager = BiometricManager.from(context)
      val authenticators =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
          BiometricManager.Authenticators.DEVICE_CREDENTIAL
      return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }
  }
}
