package com.example.presencedetector

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.presencedetector.utils.PreferencesUtil
import com.google.android.material.appbar.MaterialToolbar

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var etFilter: EditText
    private lateinit var tvHistoryTitle: TextView
    private lateinit var preferences: PreferencesUtil
    private val adapter = HistoryAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        preferences = PreferencesUtil(this)

        etFilter = findViewById(R.id.etFilterBssid)
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle)
        recyclerView = findViewById(R.id.rvHistory)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        loadAllHistory()

        etFilter.addTextChangedListener(object : TextWatcher {
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
        })
    }

    private fun loadAllHistory() {
        val bssids = preferences.getAllTrackedBssids()
        val allLogs = mutableListOf<HistoryItem>()

        bssids.forEach { bssid ->
            val nickname = preferences.getNickname(bssid) ?: "Unknown"
            val eventLogs = preferences.getEventLogs(bssid)
            eventLogs.forEach { logLine ->
                // Parse log line to extract event type and timestamp
                val isArrival = logLine.contains("Arrived")
                val isDeparture = logLine.contains("Left")
                allLogs.add(HistoryItem(bssid, nickname, logLine, isArrival, isDeparture))
            }
        }

        val sortedLogs = allLogs.sortedByDescending { it.logDetail }
        adapter.setItems(sortedLogs)
        tvHistoryTitle.text = "Full History (${sortedLogs.size} events)"
    }

    private fun filterHistory(query: String) {
        val bssids = preferences.getAllTrackedBssids()
        val filteredLogs = mutableListOf<HistoryItem>()

        bssids.forEach { bssid ->
            val nickname = preferences.getNickname(bssid) ?: ""
            if (bssid.lowercase().contains(query) || nickname.lowercase().contains(query)) {
                val eventLogs = preferences.getEventLogs(bssid)
                eventLogs.forEach { logLine ->
                    val isArrival = logLine.contains("Arrived")
                    val isDeparture = logLine.contains("Left")
                    filteredLogs.add(HistoryItem(bssid, nickname.ifEmpty { "Unknown" }, logLine, isArrival, isDeparture))
                }
            }
        }

        val sortedLogs = filteredLogs.sortedByDescending { it.logDetail }
        adapter.setItems(sortedLogs)
        tvHistoryTitle.text = "Filtered Results (${sortedLogs.size})"
    }

    data class HistoryItem(val bssid: String, val nickname: String, val logDetail: String, val isArrival: Boolean = false, val isDeparture: Boolean = false)

    class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        private var items: List<HistoryItem> = emptyList()

        fun setItems(newItems: List<HistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history_event, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            // Set nickname
            holder.tvNickname.text = item.nickname

            // Parse timestamp from log detail
            val timestamp = extractTime(item.logDetail)
            holder.tvTimestamp.text = timestamp

            // Set BSSID (show last 8 chars for brevity)
            holder.tvBssid.text = item.bssid.takeLast(8)

            // Determine event type and set UI accordingly
            if (item.isArrival) {
                holder.tvEventIcon.text = "🟢"
                holder.chipEventType.text = "Arrived"
                holder.chipEventType.setChipBackgroundColorResource(R.color.success_bright)
            } else if (item.isDeparture) {
                holder.tvEventIcon.text = "🔴"
                holder.chipEventType.text = "Left"
                holder.chipEventType.setChipBackgroundColorResource(R.color.danger_color)
            }

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, WifiRadarActivity::class.java)
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = items.size

        private fun extractTime(logDetail: String): String {
            // Log format: "Arrived 01:49" or "Left 01:49"
            val parts = logDetail.split(" ")
            return if (parts.size >= 2) parts[1] else "N/A"
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvEventIcon: TextView = view.findViewById(R.id.tvEventIcon)
            val tvNickname: TextView = view.findViewById(R.id.tvNickname)
            val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
            val tvBssid: TextView = view.findViewById(R.id.tvBssid)
            val chipEventType: com.google.android.material.chip.Chip = view.findViewById(R.id.chipEventType)
        }
    }
}
