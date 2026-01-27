package com.royce.zyora.data.models

data class MoodEntry(
    val id: String = "",
    val mood: MoodType = MoodType.NEUTRAL,
    val intensity: Int = 3, // 1-5 scale
    val note: String = "",
    val date: String = "", // Format: yyyy-MM-dd
    val time: String = "", // Format: HH:mm
    val timestamp: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(), // e.g., "work", "family", "exercise"
    val activities: List<String> = emptyList() // What user was doing
)

enum class MoodType(val emoji: String, val displayName: String, val colorHex: String) {
    VERY_HAPPY("😄", "Very Happy", "#4CAF50"),
    HAPPY("😊", "Happy", "#8BC34A"),
    NEUTRAL("😐", "Neutral", "#607D8B"),
    SAD("😢", "Sad", "#2196F3"),
    VERY_SAD("😭", "Very Sad", "#3F51B5"),
    ANGRY("😠", "Angry", "#F44336"),
    ANXIOUS("😰", "Anxious", "#9C27B0"),
    EXCITED("🤩", "Excited", "#E91E63"),
    CALM("😌", "Calm", "#00BCD4"),
    TIRED("😴", "Tired", "#795548")
}

data class MoodStats(
    val averageMood: Float = 0f,
    val mostFrequentMood: MoodType = MoodType.NEUTRAL,
    val totalEntries: Int = 0,
    val weeklyTrend: List<Float> = emptyList(), // Last 7 days average
    val monthlyTrend: List<Float> = emptyList() // Last 30 days average
)
