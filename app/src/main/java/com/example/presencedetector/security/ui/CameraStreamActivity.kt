package com.example.presencedetector.security.ui

import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.presencedetector.databinding.ActivityCameraStreamBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.util.*

class CameraStreamActivity : AppCompatActivity(), MediaPlayer.EventListener {

    private lateinit var binding: ActivityCameraStreamBinding
    private lateinit var libVLC: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    private var cameraUrl: String? = null
    private var cameraName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraStreamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraUrl = intent.getStringExtra(EXTRA_CAMERA_URL)
        cameraName = intent.getStringExtra(EXTRA_CAMERA_NAME)

        if (cameraUrl == null) {
            showError("URL da câmera não fornecida.")
            return
        }

        // Forçar modo paisagem para melhor visualização
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val args = ArrayList<String>()
        args.add("-vvv") // Verbose logging
        libVLC = LibVLC(this, args)
        mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.setEventListener(this)
    }

    override fun onStart() {
        super.onStart()
        mediaPlayer.attachViews(binding.vlcVideoLayout, null, false, false)

        val media = Media(libVLC, Uri.parse(cameraUrl))
        media.setHWDecoderEnabled(true, false)
        media.addOption(":network-caching=1500")

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()

        showLoading(true)
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.stop()
        mediaPlayer.detachViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        libVLC.release()
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Opcional: Lidar com a mudança de orientação se não for forçada
    }

    override fun onEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Buffering -> {
                if (event.buffering < 100) {
                    showLoading(true)
                } else {
                    showLoading(false)
                }
            }
            MediaPlayer.Event.Playing -> showLoading(false)
            MediaPlayer.Event.EncounteredError -> {
                showError("Erro ao reproduzir o stream. Verifique a URL e a conexão.")
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvErrorMessage.visibility = View.GONE
    }

    private fun showError(message: String) {
        showLoading(false)
        binding.tvErrorMessage.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    companion object {
        const val EXTRA_CAMERA_URL = "extra_camera_url"
        const val EXTRA_CAMERA_NAME = "extra_camera_name"
    }
}