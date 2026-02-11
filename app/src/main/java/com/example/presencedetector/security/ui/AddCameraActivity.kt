package com.example.presencedetector.security.ui

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.presencedetector.databinding.ActivityAddCameraBinding
import com.example.presencedetector.security.model.CameraConfig
import com.example.presencedetector.security.repository.CameraRepository
import com.google.android.material.snackbar.Snackbar

class AddCameraActivity : AppCompatActivity() {

  private lateinit var binding: ActivityAddCameraBinding
  private lateinit var repository: CameraRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityAddCameraBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    repository = CameraRepository(this)

    binding.btnSaveCamera.setOnClickListener { saveCamera() }
  }

  private fun saveCamera() {
    val name = binding.etCameraName.text.toString().trim()
    val url = binding.etCameraUrl.text.toString().trim()

    if (name.isEmpty()) {
      binding.tilCameraName.error = "O nome da câmera é obrigatório"
      return
    } else {
      binding.tilCameraName.error = null
    }

    if (url.isEmpty() || !url.startsWith("rtsp://")) {
      binding.tilCameraUrl.error = "A URL RTSP é inválida"
      return
    } else {
      binding.tilCameraUrl.error = null
    }

    val newCamera = CameraConfig(name = name, rtspUrl = url)
    repository.addCamera(newCamera)

    Snackbar.make(binding.root, "Câmera salva com sucesso!", Snackbar.LENGTH_SHORT).show()

    setResult(Activity.RESULT_OK)
    finish()
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return true
  }
}
