package com.example.presencedetector.security.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CameraConfig(
  val id: String = "cam_${System.currentTimeMillis()}",
  val name: String,
  val rtspUrl: String
) : Parcelable
