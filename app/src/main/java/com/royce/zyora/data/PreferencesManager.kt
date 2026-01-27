package com.royce.zyora.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.royce.zyora.data.models.*

/**
 * Manager class for handling all SharedPreferences operations.
 * Uses Gson to serialize/deserialize complex objects for storage.
 * Stores user data, habits, mood entries, and hydration information.
 */
class PreferencesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "zyora_prefs"
        
        // User keys
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        
        // Habits keys
        private const val KEY_HABITS = "habits"
        private const val KEY_HABIT_PROGRESS = "habit_progress"
        
        // Mood keys
        private const val KEY_MOOD_ENTRIES = "mood_entries"
        
        // Hydration keys
        private const val KEY_HYDRATION_ENTRIES = "hydration_entries"
        private const val KEY_HYDRATION_GOAL = "hydration_goal"
        private const val KEY_DAILY_PORTION_PLAN = "daily_portion_plan"
        
        // App preferences
        private const val KEY_IS_DARK_MODE = "is_dark_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
    
    // User Management
    fun saveUser(user: User) {
        sharedPreferences.edit()
            .putString(KEY_USER_DATA, gson.toJson(user))
            .putString(KEY_CURRENT_USER_ID, user.id)
            .apply()
    }
    
    fun getCurrentUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            gson.fromJson(userJson, User::class.java)
        } else null
    }
    
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .apply()
    }
    
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    
    fun logout() {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_CURRENT_USER_ID, null)
            .apply()
    }
    
    // Habits Management
    /**
     * Saves list of habits to SharedPreferences as JSON.
     * @param habits List of Habit objects to persist
     */
    fun saveHabits(habits: List<Habit>) {
        val habitsJson = gson.toJson(habits)
        sharedPreferences.edit()
            .putString(KEY_HABITS, habitsJson)
            .apply()
    }
    
    fun getHabits(): List<Habit> {
        val habitsJson = sharedPreferences.getString(KEY_HABITS, null)
        return if (habitsJson != null) {
            val type = object : TypeToken<List<Habit>>() {}.type
            gson.fromJson(habitsJson, type)
        } else emptyList()
    }
    
    fun saveHabitProgress(progressList: List<HabitProgress>) {
        val progressJson = gson.toJson(progressList)
        sharedPreferences.edit()
            .putString(KEY_HABIT_PROGRESS, progressJson)
            .apply()
    }
    
    fun getHabitProgress(): List<HabitProgress> {
        val progressJson = sharedPreferences.getString(KEY_HABIT_PROGRESS, null)
        return if (progressJson != null) {
            val type = object : TypeToken<List<HabitProgress>>() {}.type
            gson.fromJson(progressJson, type)
        } else emptyList()
    }
    
    // Mood Management
    /**
     * Saves list of mood entries to SharedPreferences as JSON.
     * @param moodEntries List of MoodEntry objects to persist
     */
    fun saveMoodEntries(moodEntries: List<MoodEntry>) {
        val moodJson = gson.toJson(moodEntries)
        sharedPreferences.edit()
            .putString(KEY_MOOD_ENTRIES, moodJson)
            .apply()
    }
    
    fun getMoodEntries(): List<MoodEntry> {
        val moodJson = sharedPreferences.getString(KEY_MOOD_ENTRIES, null)
        return if (moodJson != null) {
            val type = object : TypeToken<List<MoodEntry>>() {}.type
            gson.fromJson(moodJson, type)
        } else emptyList()
    }
    
    // Hydration Management
    /**
     * Saves list of hydration entries to SharedPreferences as JSON.
     * @param entries List of HydrationEntry objects to persist
     */
    fun saveHydrationEntries(entries: List<HydrationEntry>) {
        val entriesJson = gson.toJson(entries)
        sharedPreferences.edit()
            .putString(KEY_HYDRATION_ENTRIES, entriesJson)
            .apply()
    }
    
    fun getHydrationEntries(): List<HydrationEntry> {
        val entriesJson = sharedPreferences.getString(KEY_HYDRATION_ENTRIES, null)
        return if (entriesJson != null) {
            val type = object : TypeToken<List<HydrationEntry>>() {}.type
            gson.fromJson(entriesJson, type)
        } else emptyList()
    }
    
    fun saveHydrationGoal(goal: HydrationGoal) {
        val goalJson = gson.toJson(goal)
        sharedPreferences.edit()
            .putString(KEY_HYDRATION_GOAL, goalJson)
            .apply()
    }
    
    fun getHydrationGoal(): HydrationGoal {
        val goalJson = sharedPreferences.getString(KEY_HYDRATION_GOAL, null)
        return if (goalJson != null) {
            gson.fromJson(goalJson, HydrationGoal::class.java)
        } else HydrationGoal()
    }
    
    // Portion Schedule Management
    fun saveDailyPortionPlan(plan: DailyPortionPlan) {
        val planJson = gson.toJson(plan)
        sharedPreferences.edit()
            .putString(KEY_DAILY_PORTION_PLAN, planJson)
            .apply()
    }
    
    fun getDailyPortionPlan(): DailyPortionPlan? {
        val planJson = sharedPreferences.getString(KEY_DAILY_PORTION_PLAN, null)
        return if (planJson != null) {
            gson.fromJson(planJson, DailyPortionPlan::class.java)
        } else null
    }
    
    fun updatePortionCompletion(portionIndex: Int, isCompleted: Boolean) {
        val plan = getDailyPortionPlan() ?: return
        val updatedPortions = plan.scheduledPortions.map { portion ->
            if (portion.portionIndex == portionIndex) {
                portion.copy(
                    isCompleted = isCompleted,
                    completedAt = if (isCompleted) System.currentTimeMillis() else null
                )
            } else {
                portion
            }
        }
        val updatedPlan = plan.copy(scheduledPortions = updatedPortions)
        saveDailyPortionPlan(updatedPlan)
    }
    
    // App Preferences
    fun setDarkMode(isDarkMode: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_DARK_MODE, isDarkMode)
            .apply()
    }
    
    fun isDarkMode(): Boolean = sharedPreferences.getBoolean(KEY_IS_DARK_MODE, false)
    
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
            .apply()
    }
    
    fun isNotificationsEnabled(): Boolean = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    
    fun setFirstLaunch(isFirstLaunch: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_FIRST_LAUNCH, isFirstLaunch)
            .apply()
    }
    
    fun isFirstLaunch(): Boolean = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    
    // Utility methods
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
}
