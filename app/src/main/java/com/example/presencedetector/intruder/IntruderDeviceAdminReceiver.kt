package com.example.presencedetector.intruder

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Receives failed screen-unlock attempts (requires the user to enable device admin). */
class IntruderDeviceAdminReceiver : DeviceAdminReceiver() {
  override fun onPasswordFailed(context: Context, intent: Intent) {
    IntruderAlertSender.report(context, "Tentativa de desbloqueio do celular com senha incorreta")
  }
}
