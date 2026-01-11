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
            val dates = preferences.getDetectionHistory(bssid)
            dates.forEach { date ->
                allLogs.add(HistoryItem(bssid, nickname, date))
            }
        }
        
        val sortedLogs = allLogs.sortedByDescending { it.date }
        adapter.setItems(sortedLogs)
        tvHistoryTitle.text = "Full History (${sortedLogs.size} records)"
    }

    private fun filterHistory(query: String) {
        val bssids = preferences.getAllTrackedBssids()
        val filteredLogs = mutableListOf<HistoryItem>()
        
        bssids.forEach { bssid ->
            val nickname = preferences.getNickname(bssid) ?: ""
            // Filter by BSSID or Nickname
            if (bssid.lowercase().contains(query) || nickname.lowercase().contains(query)) {
                val dates = preferences.getDetectionHistory(bssid)
                dates.forEach { date ->
                    filteredLogs.add(HistoryItem(bssid, nickname.ifEmpty { "Unknown" }, date))
                }
            }
        }
        
        val sortedLogs = filteredLogs.sortedByDescending { it.date }
        adapter.setItems(sortedLogs)
        tvHistoryTitle.text = "Filtered Results (${sortedLogs.size})"
    }

    data class HistoryItem(val bssid: String, val nickname: String, val date: String)

    class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        private var items: List<HistoryItem> = emptyList()

        fun setItems(newItems: List<HistoryItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.text1.text = "${item.date} - ${item.nickname}"
            holder.text1.setTextColor(holder.itemView.context.getColor(R.color.dark_text))
            holder.text2.text = "BSSID: ${item.bssid}"
            holder.text2.setTextColor(holder.itemView.context.getColor(R.color.light_text))
            
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, WifiRadarActivity::class.java)
                // Pass BSSID to highlight or auto-open if we want
                holder.itemView.context.startActivity(intent)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
