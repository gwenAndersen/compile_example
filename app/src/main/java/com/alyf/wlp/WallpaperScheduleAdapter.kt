package com.alyf.wlp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WallpaperScheduleAdapter(
    private val schedules: MutableList<WallpaperSchedule>,
    private val onDeleteClickListener: (Int) -> Unit,
    private val onEditClickListener: (Int) -> Unit
) : RecyclerView.Adapter<WallpaperScheduleAdapter.ViewHolder>(), ItemMoveCallback.ItemMoveAdapter {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallpaper_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.scheduleImageTextView.text = schedule.uri.toString()
        holder.scheduleTimeTextView.text = String.format("%02d:%02d", schedule.hour, schedule.minute)
        holder.deleteButton.setOnClickListener { onDeleteClickListener(position) }
        holder.editButton.setOnClickListener { onEditClickListener(position) }
    }

    override fun getItemCount(): Int {
        return schedules.size
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        val fromSchedule = schedules[fromPosition]
        schedules.removeAt(fromPosition)
        schedules.add(toPosition, fromSchedule)
        notifyItemMoved(fromPosition, toPosition)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scheduleImageTextView: TextView = itemView.findViewById(R.id.schedule_image_textview)
        val scheduleTimeTextView: TextView = itemView.findViewById(R.id.schedule_time_textview)
        val deleteButton: Button = itemView.findViewById(R.id.delete_schedule_button)
        val editButton: Button = itemView.findViewById(R.id.edit_schedule_button)
    }
}
