package com.example.bmonitoringurl

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UrlAdapter(
    private val urls: MutableList<MonitoredUrl>,
    private val onDeleteClick: (MonitoredUrl) -> Unit
) : RecyclerView.Adapter<UrlAdapter.UrlViewHolder>() {

    class UrlViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrlText: TextView = view.findViewById(R.id.tvUrlText)
        val btnDeleteUrl: ImageButton = view.findViewById(R.id.btnDeleteUrl)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitored_url, parent, false)
        return UrlViewHolder(view)
    }

    override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
        val monitoredUrl = urls[position]
        holder.tvUrlText.text = monitoredUrl.url

        holder.btnDeleteUrl.setOnClickListener {
            onDeleteClick(monitoredUrl)
        }
    }

    override fun getItemCount() = urls.size

    fun updateData(newUrls: List<MonitoredUrl>) {
        urls.clear()
        urls.addAll(newUrls)
        notifyDataSetChanged()
    }
}