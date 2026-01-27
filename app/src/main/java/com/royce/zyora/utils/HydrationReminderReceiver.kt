package com.royce.zyora.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.royce.zyora.data.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

class HydrationReminderReceiver : BroadcastReceiver() {

    private fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On Android 12+ (API 31+), check if we can schedule exact alarms
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            // On older versions, exact alarms are allowed by default
            true
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val dailyGoal = intent.getIntExtra("daily_goal", 2000)
        val portionSize = intent.getIntExtra("portion_size", 250)
        val portionIndex = intent.getIntExtra("portion_index", 1)
        val totalPortions = intent.getIntExtra("total_portions", 8)

        // Get current hydration progress
        val preferencesManager = PreferencesManager(context)
        
        // Check if this portion is already completed
        val dailyPlan = preferencesManager.getDailyPortionPlan()
        val portion = dailyPlan?.scheduledPortions?.find { it.portionIndex == portionIndex }
        
        // Only send notification if portion is NOT completed
        if (portion != null && portion.isCompleted) {
            android.util.Log.d("HydrationReminder", "Portion $portionIndex already completed, skipping notification")
            return
        }
        
        val hydrationEntries = preferencesManager.getHydrationEntries()

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayIntake = hydrationEntries
            .filter { it.date == today }
            .sumOf { it.amount }

        // Show portion-specific reminder
        HydrationNotificationManager.showHydrationNotification(
            context,
            dailyGoal,
            todayIntake,
            portionSize,
            portionIndex,
            totalPortions
        )

        // Check for streak achievements (show once per day when goal is reached)
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Show motivational notification only in the evening (after 6 PM) if goal is achieved
        if (hour >= 18 && todayIntake >= dailyGoal) {
            val streak = calculateHydrationStreak(preferencesManager)
            if (streak > 0) {
                HydrationNotificationManager.showMotivationalNotification(context, streak)
            }
        }
    }

    private fun isWithinReminderTime(startTime: String, endTime: String): Boolean {
        try {
            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            val startParts = startTime.split(":").map { it.toInt() }
            val endParts = endTime.split(":").map { it.toInt() }

            val startHour = startParts[0]
            val startMinute = startParts[1]
            val endHour = endParts[0]
            val endMinute = endParts[1]

            val currentTimeInMinutes = currentHour * 60 + currentMinute
            val startTimeInMinutes = startHour * 60 + startMinute
            val endTimeInMinutes = endHour * 60 + endMinute

            return currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
        } catch (e: Exception) {
            // If there's any error parsing times, default to allowing reminders
            return true
        }
    }

    private fun scheduleNextReminderAtStartTime(
        context: Context,
        dailyGoal: Int,
        reminderInterval: Int,
        startTime: String
    ) {
        try {
            val calendar = Calendar.getInstance()
            val startParts = startTime.split(":").map { it.toInt() }
            calendar.set(Calendar.HOUR_OF_DAY, startParts[0])
            calendar.set(Calendar.MINUTE, startParts[1])
            calendar.set(Calendar.SECOND, 0)

            // If start time has passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, HydrationReminderReceiver::class.java).apply {
                putExtra("daily_goal", dailyGoal)
                putExtra("reminder_interval", reminderInterval)
                putExtra("start_time", startTime)
                putExtra("end_time", "22:00") // Default end time
            }

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                HydrationNotificationManager.HYDRATION_NOTIFICATION_ID,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule with exact alarm if possible, otherwise use inexact
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            // If scheduling fails, fall back to regular interval scheduling
            android.util.Log.e("HydrationReminder", "Failed to schedule next reminder", e)
        }
    }

    private fun calculateHydrationStreak(preferencesManager: PreferencesManager): Int {
        val hydrationEntries = preferencesManager.getHydrationEntries()
        val hydrationGoal = preferencesManager.getHydrationGoal()

        if (hydrationGoal.dailyGoalMl <= 0) return 0

        var streak = 0
        val calendar = Calendar.getInstance()

        // Check each day going backwards until we find a day that didn't meet the goal
        while (true) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val dayIntake = hydrationEntries
                .filter { it.date == date }
                .sumOf { it.amount }

            if (dayIntake >= hydrationGoal.dailyGoalMl) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}
