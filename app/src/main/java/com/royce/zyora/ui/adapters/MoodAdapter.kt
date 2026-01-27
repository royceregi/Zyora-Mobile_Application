package com.royce.zyora.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.royce.zyora.data.models.MoodEntry
import com.royce.zyora.databinding.ItemMoodBinding

class MoodAdapter(
    private val moods: List<MoodEntry>,
    private val onMoodClick: (MoodEntry) -> Unit,
    private val onMoodDelete: (MoodEntry) -> Unit
) : RecyclerView.Adapter<MoodAdapter.MoodViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodViewHolder {
        val binding = ItemMoodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodViewHolder, position: Int) {
        holder.bind(moods[position])
    }

    override fun getItemCount(): Int = moods.size

    inner class MoodViewHolder(
        private val binding: ItemMoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mood: MoodEntry) {
            binding.apply {
                // Set mood emoji and name
                tvMoodEmoji.text = mood.mood.emoji
                tvMoodName.text = mood.mood.displayName
                
                // Set date and time
                tvDateTime.text = "${mood.date} at ${mood.time}"
                
                // Set note (if available)
                if (mood.note.isNotEmpty()) {
                    tvNote.text = mood.note
                    tvNote.visibility = android.view.View.VISIBLE
                } else {
                    tvNote.visibility = android.view.View.GONE
                }
                
                // Set intensity indicator
                val intensityText = when (mood.intensity) {
                    5 -> "Very High"
                    4 -> "High"
                    3 -> "Medium"
                    2 -> "Low"
                    1 -> "Very Low"
                    else -> "Unknown"
                }
                tvIntensity.text = "Intensity: $intensityText"
                
                // Set mood color
                try {
                    val color = Color.parseColor(mood.mood.colorHex)
                    viewMoodColor.setBackgroundColor(color)
                } catch (e: Exception) {
                    // Use default color if parsing fails
                    viewMoodColor.setBackgroundColor(Color.parseColor("#607D8B"))
                }
                
                // Handle clicks
                cardMood.setOnClickListener {
                    onMoodClick(mood)
                }
                
                btnDelete.setOnClickListener {
                    onMoodDelete(mood)
                }
            }
        }
    }
}
