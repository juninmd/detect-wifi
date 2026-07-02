package com.example.presencedetector.security.repository

import android.content.Context
import com.example.presencedetector.security.model.CameraConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CameraRepository(context: Context) {

  private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  private val gson = Gson()

  fun saveCameras(cameras: List<CameraConfig>) {
    val json = gson.toJson(cameras)
    sharedPreferences.edit().putString(KEY_CAMERAS, json).apply()
  }

  fun getCameras(): MutableList<CameraConfig> {
    val json = sharedPreferences.getString(KEY_CAMERAS, null)
    return if (json != null) {
      val type = object : TypeToken<MutableList<CameraConfig>>() {}.type
      gson.fromJson(json, type)
    } else {
      mutableListOf()
    }
  }

  fun addCamera(camera: CameraConfig) {
    val cameras = getCameras()
    cameras.add(camera)
    saveCameras(cameras)
  }

  fun deleteCamera(camera: CameraConfig) {
    val cameras = getCameras()
    cameras.removeAll { it.id == camera.id }
    saveCameras(cameras)
  }

  companion object {
    private const val PREFS_NAME = "security_camera_prefs"
    private const val KEY_CAMERAS = "camera_list"
  }
}
