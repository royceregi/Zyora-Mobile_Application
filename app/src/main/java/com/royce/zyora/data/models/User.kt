package com.royce.zyora.data.models

data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "", // In production, this should be hashed
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val preferences: UserPreferences = UserPreferences()
)

data class UserPreferences(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val hydrationReminderInterval: Int = 120, // minutes
    val dailyHydrationGoal: Int = 8, // glasses
    val reminderStartTime: String = "08:00",
    val reminderEndTime: String = "22:00"
)
