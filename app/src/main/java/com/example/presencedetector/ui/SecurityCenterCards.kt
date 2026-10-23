package com.example.presencedetector.ui

import android.content.Context
import android.content.Intent
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ActivityMainBinding
import com.example.presencedetector.lock.AppLockPreferences
import com.example.presencedetector.lock.AppLockSettingsActivity
import com.example.presencedetector.presence.PresenceActivity
import com.example.presencedetector.presence.PresencePreferences
import com.example.presencedetector.router.RouterActivity
import com.example.presencedetector.router.RouterPreferences

/** Wires the router, presence sensor and app-lock shortcut cards on the home screen. */
object SecurityCenterCards {
  fun bind(context: Context, binding: ActivityMainBinding) {
    binding.cardRouter.setOnClickListener {
      context.startActivity(Intent(context, RouterActivity::class.java))
    }
    binding.cardPresence.setOnClickListener {
      context.startActivity(Intent(context, PresenceActivity::class.java))
    }
    binding.cardAppLock.setOnClickListener {
      context.startActivity(Intent(context, AppLockSettingsActivity::class.java))
    }
  }

  fun refresh(context: Context, binding: ActivityMainBinding) {
    val router = RouterPreferences(context)
    binding.tvRouterSummary.text =
      if (router.isConfigured()) context.getString(R.string.card_router_on, router.host)
      else context.getString(R.string.card_router_off)

    val presence = PresencePreferences(context)
    binding.tvPresenceSummary.text =
      if (presence.isConfigured()) context.getString(R.string.card_presence_on, presence.baseUrl)
      else context.getString(R.string.card_presence_off)

    val lock = AppLockPreferences(context)
    binding.tvAppLockSummary.text =
      if (lock.enabled) context.getString(R.string.card_applock_on, lock.protectedApps.size)
      else context.getString(R.string.card_applock_off)
  }
}
