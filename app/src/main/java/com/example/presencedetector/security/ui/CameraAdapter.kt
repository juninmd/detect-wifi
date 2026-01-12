package com.example.presencedetector.security.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.databinding.CardCameraItemBinding
import com.example.presencedetector.security.model.CameraConfig

class CameraAdapter(
    private var cameras: List<CameraConfig>,
    private val onPlayClicked: (CameraConfig) -> Unit
) : RecyclerView.Adapter<CameraAdapter.CameraViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        val binding = CardCameraItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CameraViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val camera = cameras[position]
        holder.bind(camera)
    }

    override fun getItemCount(): Int = cameras.size

    fun updateData(newCameras: List<CameraConfig>) {
        cameras = newCameras
        notifyDataSetChanged()
    }

    inner class CameraViewHolder(private val binding: CardCameraItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(camera: CameraConfig) {
            binding.tvCameraName.text = camera.name
            binding.btnPlayStream.setOnClickListener {
                onPlayClicked(camera)
            }
        }
    }
}
