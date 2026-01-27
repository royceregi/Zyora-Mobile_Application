package com.royce.zyora.data.models

data class PortionSchedule(
    val portionIndex: Int, // 1, 2, 3... (Glass number)
    val scheduledTime: String, // Format: "HH:mm" (when this glass should be consumed)
    val portionSize: Int, // ml per glass
    val isCompleted: Boolean = false,
    val completedAt: Long? = null, // timestamp when user checked the box
    val date: String // Format: "yyyy-MM-dd"
)

data class DailyPortionPlan(
    val date: String, // Today's date
    val totalGlasses: Int, // e.g., 8 glasses
    val glassSize: Int, // e.g., 250ml per glass
    val intervalMinutes: Int, // e.g., 45 minutes between each glass
    val startTime: String, // When reminders start (e.g., "08:00")
    val scheduledPortions: List<PortionSchedule>
)
