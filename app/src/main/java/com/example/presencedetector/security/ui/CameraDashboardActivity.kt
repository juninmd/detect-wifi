package com.example.presencedetector.security.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.R
import com.example.presencedetector.databinding.ActivityCameraDashboardBinding
import com.example.presencedetector.security.model.CameraChannel
import com.example.presencedetector.security.model.DetectionSettings

class CameraDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraDashboardBinding
    private lateinit var settings: DetectionSettings
    private val adapter = CameraAdapter()
    private var libVLC: org.videolan.libvlc.LibVLC? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Inicializa LibVLC (Instância global para a activity)
        val vlcOptions = arrayListOf(
            "--rtsp-tcp",
            "--network-caching=300",
            "--no-audio",
            "--no-drop-late-frames",
            "--no-skip-frames"
        )
        libVLC = org.videolan.libvlc.LibVLC(this, vlcOptions)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Security Cameras"

        setupRecyclerView()
        setupListeners()
    }
    
    override fun onResume() {
        super.onResume()
        loadCameras()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        libVLC?.release()
        libVLC = null
    }

    private fun setupRecyclerView() {
        binding.rvCameraList.layoutManager = GridLayoutManager(this, 2)
        binding.rvCameraList.adapter = adapter
    }
    
    private fun setupListeners() {
        binding.fabAddCamera.setOnClickListener {
            startActivity(Intent(this, SecuritySettingsActivity::class.java))
        }

        adapter.setOnItemClickListener { channel ->
            val intent = Intent(this, CameraStreamActivity::class.java).apply {
                putExtra(CameraStreamActivity.EXTRA_CAMERA_URL, channel.rtspUrl)
                putExtra(CameraStreamActivity.EXTRA_CAMERA_NAME, channel.name)
            }
            startActivity(intent)
        }
    }

    private fun loadCameras() {
        settings = DetectionSettings.load(this)
        val channels = settings.channels
        
        // Filtra câmeras ocultas
        val visibleChannels = channels.filterNot { it.isHidden }

        if (visibleChannels.isEmpty()) {
            binding.rvCameraList.visibility = View.GONE
        } else {
            binding.rvCameraList.visibility = View.VISIBLE
            adapter.submitList(visibleChannels)
        }
    }
    
    private fun toggleCameraVisibility(channel: CameraChannel) {
        // Atualiza o canal
        val updatedChannels = settings.channels.map {
            if (it.id == channel.id) it.copy(isHidden = !it.isHidden) else it
        }
        
        settings = settings.copy(channels = updatedChannels)
        settings.save(this)
        
        // Recarrega lista
        loadCameras()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class CameraAdapter : RecyclerView.Adapter<CameraAdapter.CameraViewHolder>() {
        private var items: List<CameraChannel> = emptyList()
        private var onItemClick: ((CameraChannel) -> Unit)? = null

        fun submitList(newItems: List<CameraChannel>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun setOnItemClickListener(listener: (CameraChannel) -> Unit) {
            onItemClick = listener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_camera_dashboard, parent, false)
            return CameraViewHolder(view)
        }

        override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
            holder.bind(items[position])
        }
        
        override fun onViewRecycled(holder: CameraViewHolder) {
            super.onViewRecycled(holder)
            holder.releasePlayer()
        }

        override fun getItemCount() = items.size

        inner class CameraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), 
            org.videolan.libvlc.interfaces.IVLCVout.Callback {
            
            private val tvName: TextView = itemView.findViewById(R.id.tvCameraName)
            private val tvStatus: TextView = itemView.findViewById(R.id.tvCameraStatus)
            private val videoSurface: android.view.SurfaceView = itemView.findViewById(R.id.videoSurface)
            private val ivLoading: View = itemView.findViewById(R.id.ivLoading)
            private val btnToggleVisibility: View = itemView.findViewById(R.id.btnToggleVisibility)
            
            private var mediaPlayer: org.videolan.libvlc.MediaPlayer? = null

            fun bind(channel: CameraChannel) {
                tvName.text = channel.name
                tvStatus.text = "Canal ${channel.channel}"
                
                startPreview(channel)
                
                itemView.setOnClickListener {
                    onItemClick?.invoke(channel)
                }
                
                btnToggleVisibility.setOnClickListener {
                    toggleCameraVisibility(channel)
                }
            }
            
            private fun startPreview(channel: CameraChannel) {
                releasePlayer()
                
                val vlc = libVLC ?: return
                val player = org.videolan.libvlc.MediaPlayer(vlc)
                mediaPlayer = player
                
                val vlcOut = player.vlcVout
                vlcOut.setVideoView(videoSurface)
                vlcOut.addCallback(this)
                vlcOut.attachViews()
                
                val url = channel.rtspUrlSubstream
                val media = org.videolan.libvlc.Media(vlc, android.net.Uri.parse(url)).apply {
                    setHWDecoderEnabled(true, false)
                    addOption(":network-caching=300")
                    addOption(":rtsp-tcp")
                }
                
                player.media = media
                media.release()
                
                player.play()
                ivLoading.visibility = View.VISIBLE
                
                player.setEventListener { event ->
                     if (event.type == org.videolan.libvlc.MediaPlayer.Event.Playing) {
                         ivLoading.post { ivLoading.visibility = View.GONE }
                     }
                }
            }
            
            fun releasePlayer() {
                mediaPlayer?.let { player ->
                    player.stop()
                    player.vlcVout.detachViews()
                    player.release()
                }
                mediaPlayer = null
            }
            
            override fun onSurfacesCreated(vlcVout: org.videolan.libvlc.interfaces.IVLCVout?) {}
            override fun onSurfacesDestroyed(vlcVout: org.videolan.libvlc.interfaces.IVLCVout?) {}
        }
    }
}