package com.royce.zyora.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.royce.zyora.R
import com.royce.zyora.data.models.PortionSchedule
import java.text.SimpleDateFormat
import java.util.*

class PortionScheduleAdapter(
    private val portions: List<PortionSchedule>,
    private val onPortionChecked: (Int, Boolean) -> Unit
) : RecyclerView.Adapter<PortionScheduleAdapter.PortionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_portion_schedule, parent, false)
        return PortionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PortionViewHolder, position: Int) {
        holder.bind(portions[position])
    }

    override fun getItemCount(): Int = portions.size

    inner class PortionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkboxPortion)
        private val tvPortionNumber: TextView = itemView.findViewById(R.id.tvPortionNumber)
        private val tvScheduledTime: TextView = itemView.findViewById(R.id.tvScheduledTime)
        private val tvPortionSize: TextView = itemView.findViewById(R.id.tvPortionSize)
        private val tvCompletedTime: TextView = itemView.findViewById(R.id.tvCompletedTime)

        fun bind(portion: PortionSchedule) {
            tvPortionNumber.text = "Glass ${portion.portionIndex}"
            tvScheduledTime.text = "⏰ Reminder at ${portion.scheduledTime}"
            tvPortionSize.text = "${portion.portionSize}ml"
            
            checkBox.isChecked = portion.isCompleted
            
            // Show completed time if available
            if (portion.isCompleted && portion.completedAt != null) {
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val completedTimeStr = timeFormat.format(Date(portion.completedAt))
                tvCompletedTime.text = "✓ Checked at $completedTimeStr"
                tvCompletedTime.visibility = View.VISIBLE
            } else {
                tvCompletedTime.visibility = View.GONE
            }
            
            // Update UI based on completion status
            val alpha = if (portion.isCompleted) 0.5f else 1.0f
            tvPortionNumber.alpha = alpha
            tvScheduledTime.alpha = alpha
            tvPortionSize.alpha = alpha
            
            // Set checkbox listener
            checkBox.setOnCheckedChangeListener(null) // Clear previous listener
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onPortionChecked(portion.portionIndex, isChecked)
            }
        }
    }
}
