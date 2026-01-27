package com.royce.zyora.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.royce.zyora.R
import com.royce.zyora.MainActivity
import com.royce.zyora.data.models.HydrationGoal
import java.util.*

object HydrationNotificationManager {

    const val HYDRATION_NOTIFICATION_CHANNEL_ID = "hydration_reminders"
    const val HYDRATION_NOTIFICATION_ID = 2001

    private fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // On Android 12+ (API 31+), check if we can schedule exact alarms
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            // On older versions, exact alarms are allowed by default
            true
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hydration Reminders"
            val descriptionText = "Regular reminders to drink water"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(HYDRATION_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleHydrationReminders(context: Context, goal: HydrationGoal) {
        if (!goal.isReminderEnabled) {
            cancelHydrationReminders(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Calculate how many portions to schedule
        val portionsToSchedule = if (goal.autoCalculatePortions) {
            if (goal.portionSizeMl > 0) goal.dailyGoalMl / goal.portionSizeMl else 1
        } else {
            goal.portionsPerDay
        }

        // Parse start and end times
        val startParts = goal.startTime.split(":").map { it.toInt() }
        val endParts = goal.endTime.split(":").map { it.toInt() }

        val startHour = startParts[0]
        val startMinute = startParts[1]
        val endHour = endParts[0]
        val endMinute = endParts[1]

        val startTimeInMinutes = startHour * 60 + startMinute
        val endTimeInMinutes = endHour * 60 + endMinute
        val availableMinutes = endTimeInMinutes - startTimeInMinutes

        // Calculate interval between portions
        val intervalMinutes = if (portionsToSchedule > 1) {
            availableMinutes / (portionsToSchedule - 1)
        } else {
            goal.reminderIntervalMinutes
        }

        // Schedule each portion reminder
        for (portionIndex in 0 until portionsToSchedule) {
            val portionTimeInMinutes = startTimeInMinutes + (portionIndex * intervalMinutes)

            val calendar = Calendar.getInstance()
            val portionHour = portionTimeInMinutes / 60
            val portionMinute = portionTimeInMinutes % 60

            calendar.set(Calendar.HOUR_OF_DAY, portionHour)
            calendar.set(Calendar.MINUTE, portionMinute)
            calendar.set(Calendar.SECOND, 0)

            // If this portion time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Create intent for this specific portion
            val intent = Intent(context, HydrationReminderReceiver::class.java).apply {
                putExtra("daily_goal", goal.dailyGoalMl)
                putExtra("portion_size", goal.portionSizeMl)
                putExtra("portion_index", portionIndex + 1)
                putExtra("total_portions", portionsToSchedule)
                putExtra("start_time", goal.startTime)
                putExtra("end_time", goal.endTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                HYDRATION_NOTIFICATION_ID + portionIndex, // Unique ID for each portion
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule this portion reminder
            if (canScheduleExactAlarms(context)) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                android.util.Log.d("HydrationNotification", "Scheduled exact alarm for portion $portionIndex at ${calendar.time}")
            } else {
                // Fall back to inexact alarm if exact alarms are not allowed
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                android.util.Log.d("HydrationNotification", "Scheduled inexact alarm for portion $portionIndex at ${calendar.time} (exact alarms not allowed)")
            }
        }
    }

    fun cancelHydrationReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel all possible portion reminders (up to 20 portions should be enough)
        for (portionIndex in 0 until 20) {
            val intent = Intent(context, HydrationReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                HYDRATION_NOTIFICATION_ID + portionIndex,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    fun showHydrationNotification(
        context: Context,
        dailyGoal: Int,
        currentIntake: Int,
        portionSize: Int,
        portionIndex: Int,
        totalPortions: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Calculate progress
        val progressPercentage = ((currentIntake.toFloat() / dailyGoal.toFloat()) * 100).toInt()
        val remaining = (dailyGoal - currentIntake).coerceAtLeast(0)

        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration_tab", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            portionIndex, // Use portion index as request code for uniqueness
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create portion-specific message
        val title = when (portionIndex) {
            1 -> "🌅 Time for your first hydration!"
            totalPortions -> "🌙 Final hydration reminder for today!"
            else -> "💧 Hydration Reminder ${portionIndex}/${totalPortions}"
        }

        val message = if (portionIndex == totalPortions) {
            // Last portion of the day
            if (remaining <= portionSize) {
                "🎉 Last portion! Drink ${portionSize}ml to complete your daily goal!"
            } else {
                "💧 Drink ${portionSize}ml. ${remaining}ml remaining to reach your goal."
            }
        } else {
            // Regular portion
            "⏰ Drink ${portionSize}ml now. Portion ${portionIndex} of ${totalPortions} today."
        }

        val notification = NotificationCompat.Builder(context, HYDRATION_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hydration)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_add,
                "Add ${portionSize}ml",
                createQuickAddIntent(context, portionSize, portionIndex)
            )
            .build()

        notificationManager.notify(HYDRATION_NOTIFICATION_ID + portionIndex, notification)
    }

    fun showGoalAchievedNotification(context: Context, dailyGoal: Int, currentIntake: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Calculate progress
        val progressPercentage = ((currentIntake.toFloat() / dailyGoal.toFloat()) * 100).toInt()

        // Create intent to open the app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration_tab", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            999, // Unique ID for goal achievement
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎉 Daily Goal Achieved!"
        val message = "Congratulations! You've reached your daily hydration goal of ${dailyGoal}ml!"

        val notification = NotificationCompat.Builder(context, HYDRATION_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hydration)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(HYDRATION_NOTIFICATION_ID + 999, notification)
    }

    private fun createQuickAddIntent(context: Context, amount: Int, portionIndex: Int): PendingIntent {
        val intent = Intent(context, HydrationQuickAddReceiver::class.java).apply {
            putExtra("water_amount", amount)
            putExtra("portion_index", portionIndex)
        }

        return PendingIntent.getBroadcast(
            context,
            amount + portionIndex * 1000, // Make it unique per portion
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showMotivationalNotification(context: Context, streakDays: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (title, message) = when {
            streakDays >= 30 -> "🏆 Hydration Master!" to "Amazing! ${streakDays} days of perfect hydration!"
            streakDays >= 14 -> "💧 Hydration Champion!" to "Fantastic! ${streakDays} days streak! Keep it up!"
            streakDays >= 7 -> "🌊 One Week Strong!" to "Great job! ${streakDays} days of staying hydrated!"
            streakDays >= 3 -> "💦 Building Momentum!" to "Nice! ${streakDays} days in a row! You're doing great!"
            else -> "🚰 Great Start!" to "You're building a healthy hydration habit!"
        }

        val notification = NotificationCompat.Builder(context, HYDRATION_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hydration)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(HYDRATION_NOTIFICATION_ID + 1, notification)
    }
}
