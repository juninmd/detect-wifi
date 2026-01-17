package com.example.presencedetector

import android.app.Application
import com.example.presencedetector.utils.NotificationUtil
import com.google.firebase.FirebaseApp

/**
 * Application class for initializing global configurations.
 */
class PresenceDetectorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Firebase initialization failed - app can still work without it
        }

        // Create notification channels
        NotificationUtil.createNotificationChannels(this)
    }
}
