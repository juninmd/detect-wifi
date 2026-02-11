package com.example.presencedetector

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.databinding.ActivityHistoryBinding
import com.example.presencedetector.databinding.ItemHistoryEventBinding
import com.example.presencedetector.utils.PreferencesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryActivity : AppCompatActivity() {

  private lateinit var binding: ActivityHistoryBinding
  private lateinit var preferences: PreferencesUtil
  private val adapter = HistoryAdapter()
  private var loadJob: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    binding.toolbar.setNavigationOnClickListener { finish() }

    preferences = PreferencesUtil(this)

    binding.rvHistory.layoutManager = LinearLayoutManager(this)
    binding.rvHistory.adapter = adapter

    loadAllHistory()

    binding.etFilterBssid.addTextChangedListener(
      object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
          val query = s.toString().lowercase()
          if (query.length >= 2) {
            filterHistory(query)
          } else if (query.isEmpty()) {
            loadAllHistory()
          }
        }

        override fun afterTextChanged(s: Editable?) {}
      }
    )
  }

  private fun loadAllHistory() {
    loadJob?.cancel()
    loadJob =
      lifecycleScope.launch(Dispatchers.IO) {
        val bssids = preferences.getAllTrackedBssids()
        val allLogs = mutableListOf<HistoryItem>()

        bssids.forEach { bssid ->
          val nickname = preferences.getNickname(bssid) ?: getString(R.string.text_unknown)
          val eventLogs = preferences.getEventLogs(bssid)
          eventLogs.forEach { logLine ->
            // Parse log line to extract event type and timestamp
            val isArrival = logLine.contains("Arrived")
            val isDeparture = logLine.contains("Left")
            allLogs.add(HistoryItem(bssid, nickname, logLine, isArrival, isDeparture))
          }
        }

        // Add System Logs
        val systemLogs = preferences.getSystemLogs()
        systemLogs.forEach { logLine ->
          allLogs.add(HistoryItem("SYSTEM", "Security System", logLine))
        }

        val sortedLogs = allLogs.sortedByDescending { it.logDetail }

        withContext(Dispatchers.Main) {
          adapter.setItems(sortedLogs)
          binding.tvHistoryTitle.text = getString(R.string.title_history_full, sortedLogs.size)
        }
      }
  }

  private fun filterHistory(query: String) {
    loadJob?.cancel()
    loadJob =
      lifecycleScope.launch(Dispatchers.IO) {
        val bssids = preferences.getAllTrackedBssids()
        val filteredLogs = mutableListOf<HistoryItem>()

        bssids.forEach { bssid ->
          val nickname = preferences.getNickname(bssid) ?: ""
          if (bssid.lowercase().contains(query) || nickname.lowercase().contains(query)) {
            val eventLogs = preferences.getEventLogs(bssid)
            eventLogs.forEach { logLine ->
              val isArrival = logLine.contains("Arrived")
              val isDeparture = logLine.contains("Left")
              filteredLogs.add(
                HistoryItem(
                  bssid,
                  nickname.ifEmpty { getString(R.string.text_unknown) },
                  logLine,
                  isArrival,
                  isDeparture
                )
              )
            }
          }
        }

        val sortedLogs = filteredLogs.sortedByDescending { it.logDetail }

        withContext(Dispatchers.Main) {
          adapter.setItems(sortedLogs)
          binding.tvHistoryTitle.text = getString(R.string.title_history_filtered, sortedLogs.size)
        }
      }
  }

  data class HistoryItem(
    val bssid: String,
    val nickname: String,
    val logDetail: String,
    val isArrival: Boolean = false,
    val isDeparture: Boolean = false
  )

  inner class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
    private var items: List<HistoryItem> = emptyList()

    fun setItems(newItems: List<HistoryItem>) {
      items = newItems
      notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      val binding =
        ItemHistoryEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
      return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
      val item = items[position]

      // Set nickname
      holder.binding.tvNickname.text = item.nickname

      // Parse timestamp from log detail
      val timestamp = extractTime(item.logDetail)
      holder.binding.tvTimestamp.text = timestamp

      // Set BSSID (show last 8 chars for brevity)
      holder.binding.tvBssid.text = item.bssid.takeLast(8)

      // Determine event type and set UI accordingly
      if (item.bssid == "SYSTEM") {
        holder.binding.tvEventIcon.text = "🛡️"
        holder.binding.chipEventType.text = getString(R.string.label_security)
        holder.binding.tvBssid.text = getString(R.string.label_system_log)
        // Parse message from logDetail "[timestamp] Message"
        val parts = item.logDetail.split("] ")
        if (parts.size > 1) {
          holder.binding.tvNickname.text = parts[1] // Show the message as the main text
        }
      } else if (item.isArrival) {
        holder.binding.tvEventIcon.text = "🟢"
        holder.binding.chipEventType.text = getString(R.string.label_arrived)
        holder.binding.chipEventType.setChipBackgroundColorResource(R.color.success_bright)
      } else if (item.isDeparture) {
        holder.binding.tvEventIcon.text = "🔴"
        holder.binding.chipEventType.text = getString(R.string.label_left)
        holder.binding.chipEventType.setChipBackgroundColorResource(R.color.danger_color)
      }

      if (item.bssid == "SYSTEM") {
        holder.itemView.setOnClickListener(null)
      } else {
        holder.itemView.setOnClickListener {
          // Open device detail to show full history for this specific device
          val intent = Intent(holder.itemView.context, DeviceDetailActivity::class.java)
          intent.putExtra("bssid", item.bssid)
          intent.putExtra("from_history", true)
          holder.itemView.context.startActivity(intent)
        }
      }
    }

    override fun getItemCount(): Int = items.size

    private fun extractTime(logDetail: String): String {
      // Log format: "Arrived 01:49" or "Left 01:49"
      val parts = logDetail.split(" ")
      return if (parts.size >= 2) parts[1] else getString(R.string.text_na)
    }

    inner class ViewHolder(val binding: ItemHistoryEventBinding) :
      RecyclerView.ViewHolder(binding.root)
  }
}
