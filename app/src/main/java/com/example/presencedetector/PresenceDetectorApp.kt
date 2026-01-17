package com.example.presencedetector

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.presencedetector.utils.NotificationUtil
import com.example.presencedetector.utils.PreferencesUtil
import com.google.firebase.FirebaseApp

/**
 * Application class for initializing global configurations.
 */
class PresenceDetectorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        applyTheme()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase initialization failed - app can still work without it
        }

        // Create notification channels
        NotificationUtil.createNotificationChannels(this)
    }

    private fun applyTheme() {
        val preferences = PreferencesUtil(this)
        when (preferences.getAppTheme()) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}
