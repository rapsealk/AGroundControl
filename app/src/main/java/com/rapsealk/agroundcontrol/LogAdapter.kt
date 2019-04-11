package com.rapsealk.agroundcontrol

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.rapsealk.agroundcontrol.data.LogMessage

public class LogAdapter(private val mItems: List<LogMessage> = ArrayList()) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    override fun getItemCount(): Int = mItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = mItems[position]
        holder.timestamp.text = String.format("%d", log.timestamp.toLong() % 10000)
        holder.message.text = "[${log.tag}] ${log.message}"
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val timestamp = itemView.findViewById<TextView>(R.id.tv_timestamp)
        val message = itemView.findViewById<TextView>(R.id.tv_message)
    }
}