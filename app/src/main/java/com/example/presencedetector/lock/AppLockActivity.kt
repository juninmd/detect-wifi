package com.example.presencedetector.lock

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ActivityAppLockBinding
import com.example.presencedetector.intruder.IntruderAlertSender
import com.example.presencedetector.lock.AppLockController.Result
import com.google.android.material.button.MaterialButton

/** Full-screen PIN pad shown on top of a protected app. Leaving without the PIN goes Home. */
class AppLockActivity : AppCompatActivity() {
  companion object {
    const val EXTRA_PACKAGE = "extra_package"
  }

  private lateinit var binding: ActivityAppLockBinding
  private lateinit var controller: AppLockController
  private var target = ""
  private var label = ""
  private var unlocked = false
  private var countdown: CountDownTimer? = null
  private val entered = StringBuilder()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    binding = ActivityAppLockBinding.inflate(layoutInflater)
    setContentView(binding.root)
    controller = AppLockModule.controller(this)
    target = intent.getStringExtra(EXTRA_PACKAGE).orEmpty()

    bindTarget()
    setupKeypad()
    renderDots()
    binding.btnCancel.setOnClickListener { goHome() }
    onBackPressedDispatcher.addCallback(
      this,
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = goHome()
      },
    )
    controller.lockoutRemainingMs().takeIf { it > 0 }?.let(::startCountdown)
  }

  override fun onStop() {
    super.onStop()
    if (!unlocked && !isChangingConfigurations) goHome()
  }

  override fun onDestroy() {
    countdown?.cancel()
    super.onDestroy()
  }

  private fun bindTarget() {
    val info = runCatching { packageManager.getApplicationInfo(target, 0) }.getOrNull()
    label = info?.let { packageManager.getApplicationLabel(it).toString() } ?: target
    info?.let { binding.ivAppIcon.setImageDrawable(packageManager.getApplicationIcon(it)) }
    binding.tvPrompt.text = getString(R.string.applock_prompt, label)
  }

  private fun setupKeypad() {
    binding.keypad.children.filterIsInstance<MaterialButton>().forEach { key ->
      key.tag?.let { digit -> key.setOnClickListener { append(digit.toString()) } }
    }
    binding.keyDelete.setOnClickListener {
      if (entered.isNotEmpty()) entered.deleteCharAt(entered.length - 1)
      renderDots()
    }
    binding.keyOk.setOnClickListener { submit() }
  }

  private fun append(digit: String) {
    if (entered.length >= AppLockController.MAX_PIN) return
    entered.append(digit)
    binding.tvMessage.text = ""
    renderDots()
  }

  private fun submit() {
    val pin = entered.toString()
    entered.clear()
    renderDots()
    when (val result = controller.submit(target, pin)) {
      Result.Unlocked,
      Result.NoPin -> {
        unlocked = true
        finish()
      }
      is Result.Wrong -> {
        binding.tvMessage.text = getString(R.string.applock_wrong, result.remaining)
        if (result.report) report()
      }
      is Result.Locked -> {
        startCountdown(result.remainingMs)
        if (result.report) report()
      }
    }
  }

  private fun report() =
    IntruderAlertSender.report(this, getString(R.string.applock_intruder_reason, label))

  private fun startCountdown(ms: Long) {
    setKeysEnabled(false)
    countdown?.cancel()
    countdown =
      object : CountDownTimer(ms, 1_000) {
          override fun onTick(remaining: Long) {
            binding.tvMessage.text =
              getString(R.string.applock_locked, (remaining / 1_000).toInt() + 1)
          }

          override fun onFinish() {
            binding.tvMessage.text = ""
            setKeysEnabled(true)
          }
        }
        .start()
  }

  private fun setKeysEnabled(enabled: Boolean) =
    binding.keypad.children.forEach { it.isEnabled = enabled }

  private fun renderDots() {
    binding.dots.removeAllViews()
    val size = (AppLockController.MIN_PIN).coerceAtLeast(entered.length)
    val margin = (6 * resources.displayMetrics.density).toInt()
    repeat(size) { index ->
      val dot = ImageView(this)
      dot.setImageResource(
        if (index < entered.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
      )
      dot.layoutParams =
        LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
          it.setMargins(margin, 0, margin, 0)
        }
      binding.dots.addView(dot)
    }
  }

  private fun goHome() {
    startActivity(
      Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_HOME)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    finish()
  }
}
