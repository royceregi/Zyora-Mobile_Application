package com.royce.zyora.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra("habit_id")
        val habitName = intent.getStringExtra("habit_name") ?: "Your Habit"
        val habitDescription = intent.getStringExtra("habit_description") ?: ""

        // Show the notification
        HabitNotificationManager.showHabitNotification(context, habitName, habitDescription)
    }
}
