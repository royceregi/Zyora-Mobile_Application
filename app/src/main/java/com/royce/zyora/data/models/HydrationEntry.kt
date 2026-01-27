package com.royce.zyora.data.models

data class HydrationEntry(
    val id: String = "",
    val amount: Int = 250, // ml
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = "", // Format: yyyy-MM-dd
    val time: String = "", // Format: HH:mm
    val containerType: ContainerType = ContainerType.GLASS
)

enum class ContainerType(val displayName: String, val defaultAmount: Int) {
    GLASS("Glass", 250),
    BOTTLE("Bottle", 500),
    CUP("Cup", 200),
    LARGE_BOTTLE("Large Bottle", 750),
    CUSTOM("Custom", 250) // Default to 250ml for custom entries
}

data class HydrationGoal(
    val dailyGoalMl: Int = 2000, // Total daily goal (e.g., 3000ml)
    val portionSizeMl: Int = 250, // Amount per reminder (e.g., 300ml)
    val portionsPerDay: Int = 8, // Auto-calculated: dailyGoal ÷ portionSize
    val reminderIntervalMinutes: Int = 120, // Minutes between reminders (e.g., 45)
    val startTime: String = "08:00", // When reminders start
    val endTime: String = "22:00", // When reminders end
    val isReminderEnabled: Boolean = true,
    val autoCalculatePortions: Boolean = true // If true, portionsPerDay = dailyGoal ÷ portionSize
)

data class HydrationStats(
    val todayIntake: Int = 0, // ml
    val goalProgress: Float = 0f, // 0.0 to 1.0
    val streakDays: Int = 0,
    val averageDaily: Int = 0, // ml over last 7 days
    val weeklyIntake: List<Int> = emptyList() // Last 7 days
)
