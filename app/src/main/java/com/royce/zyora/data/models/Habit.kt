package com.royce.zyora.data.models

data class Habit(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val targetCount: Int = 1, // Daily target (e.g., 8 glasses of water, 10000 steps)
    val unit: String = "", // e.g., "glasses", "steps", "minutes"
    val iconName: String = "", // Icon identifier
    val color: String = "#66B2D6", // Hex color
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val streak: Int = 0, // Current streak in days
    val bestStreak: Int = 0, // Best streak achieved
    val category: HabitCategory = HabitCategory.HEALTH,
    val reminderEnabled: Boolean = false, // Whether daily reminders are enabled
    val reminderHour: Int = 9, // Hour for daily reminder (24-hour format)
    val reminderMinute: Int = 0 // Minute for daily reminder
)

enum class HabitCategory {
    HEALTH, FITNESS, MINDFULNESS, PRODUCTIVITY, PERSONAL
}

data class HabitProgress(
    val id: String = "",
    val habitId: String = "",
    val date: String = "", // Format: yyyy-MM-dd
    val currentCount: Int = 0,
    val targetCount: Int = 1,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = ""
)

data class HabitWithProgress(
    val habit: Habit,
    val todayProgress: HabitProgress?,
    val completionPercentage: Float = 0f
)
