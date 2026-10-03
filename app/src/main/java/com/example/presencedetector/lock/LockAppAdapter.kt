package com.example.presencedetector.lock

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.databinding.ItemLockAppBinding

data class LockApp(val packageName: String, val label: String, val icon: Drawable)

/** Installed apps with a switch to protect each one behind the PIN. */
class LockAppAdapter(private val onToggle: (LockApp, Boolean) -> Unit) :
  RecyclerView.Adapter<LockAppAdapter.Holder>() {
  private var items: List<LockApp> = emptyList()
  private var protectedApps: Set<String> = emptySet()

  class Holder(val binding: ItemLockAppBinding) : RecyclerView.ViewHolder(binding.root)

  fun submit(apps: List<LockApp>, protected: Set<String>) {
    items = apps
    protectedApps = protected
    notifyDataSetChanged()
  }

  override fun getItemCount() = items.size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
    Holder(ItemLockAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

  override fun onBindViewHolder(holder: Holder, position: Int) {
    val app = items[position]
    val b = holder.binding
    b.ivIcon.setImageDrawable(app.icon)
    b.tvLabel.text = app.label
    b.tvPackage.text = app.packageName
    b.switchApp.setOnCheckedChangeListener(null)
    b.switchApp.isChecked = app.packageName in protectedApps
    b.switchApp.setOnCheckedChangeListener { _, checked -> onToggle(app, checked) }
  }
}
