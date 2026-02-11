package com.example.presencedetector.security.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.example.presencedetector.utils.CameraHelper

class HiddenCameraActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // No UI, just logic
    Log.i("HiddenCamera", "Launching hidden camera capture...")

    val helper = CameraHelper(this)
    helper.captureSelfie {
      Log.i("HiddenCamera", "Capture complete. Finishing.")
      finish()
    }
  }
}
