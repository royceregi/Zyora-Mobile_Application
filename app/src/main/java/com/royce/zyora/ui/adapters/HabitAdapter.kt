package com.royce.zyora.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.royce.zyora.data.models.Habit
import com.royce.zyora.data.models.HabitWithProgress
import com.royce.zyora.databinding.ItemHabitBinding

class HabitAdapter(
    private val habits: List<HabitWithProgress>,
    private val onHabitClick: (Habit) -> Unit,
    private val onProgressUpdate: (Habit, Int) -> Unit,
    private val onEditHabit: (Habit) -> Unit,
    private val onDeleteHabit: (Habit) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val binding = ItemHabitBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HabitViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        holder.bind(habits[position])
    }

    override fun getItemCount(): Int = habits.size

    inner class HabitViewHolder(
        private val binding: ItemHabitBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(habitWithProgress: HabitWithProgress) {
            val habit = habitWithProgress.habit
            val progress = habitWithProgress.todayProgress
            
            binding.apply {
                // Set habit info
                tvHabitName.text = habit.name
                tvHabitDescription.text = habit.description
                
                // Set progress info
                val currentCount = progress?.currentCount ?: 0
                val targetCount = habit.targetCount
                
                tvProgress.text = "$currentCount / $targetCount ${habit.unit}"
                
                // Set progress bar
                progressBar.max = targetCount
                progressBar.progress = currentCount
                
                // Set completion percentage
                val percentage = ((currentCount.toFloat() / targetCount.toFloat()) * 100).toInt()
                tvPercentage.text = "$percentage%"
                
                // Set habit color
                try {
                    val color = Color.parseColor(habit.color)
                    progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
                    ivHabitIcon.imageTintList = android.content.res.ColorStateList.valueOf(color)
                } catch (e: Exception) {
                    // Use default color if parsing fails
                }
                
                // Set icon based on category or name
                val iconRes = getHabitIcon(habit.iconName, habit.name)
                ivHabitIcon.setImageResource(iconRes)
                
                // Set completion status
                val isCompleted = progress?.isCompleted == true
                cardHabit.alpha = if (isCompleted) 0.7f else 1.0f
                
                // Handle increment button
                btnIncrement.setOnClickListener {
                    if (currentCount < targetCount) {
                        onProgressUpdate(habit, currentCount + 1)
                    }
                }
                
                // Handle decrement button
                btnDecrement.setOnClickListener {
                    if (currentCount > 0) {
                        onProgressUpdate(habit, currentCount - 1)
                    }
                }
                
                // Handle card click
                cardHabit.setOnClickListener {
                    onHabitClick(habit)
                }
                
                // Handle edit button
                btnEdit.setOnClickListener {
                    onEditHabit(habit)
                }
                
                // Handle delete button
                btnDelete.setOnClickListener {
                    onDeleteHabit(habit)
                }
            }
        }
        
        private fun getHabitIcon(iconName: String, habitName: String): Int {
            return when {
                iconName.contains("water") || habitName.lowercase().contains("water") -> 
                    com.royce.zyora.R.drawable.ic_water_drop
                iconName.contains("fitness") || habitName.lowercase().contains("exercise") -> 
                    com.royce.zyora.R.drawable.ic_fitness
                iconName.contains("meditation") || habitName.lowercase().contains("meditat") -> 
                    com.royce.zyora.R.drawable.ic_meditation
                iconName.contains("book") || habitName.lowercase().contains("read") -> 
                    com.royce.zyora.R.drawable.ic_book
                iconName.contains("walk") || habitName.lowercase().contains("step") -> 
                    com.royce.zyora.R.drawable.ic_walk
                else -> com.royce.zyora.R.drawable.ic_habits
            }
        }
    }
}
