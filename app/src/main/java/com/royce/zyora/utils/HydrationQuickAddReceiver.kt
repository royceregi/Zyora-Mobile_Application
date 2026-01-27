package com.royce.zyora.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.data.models.ContainerType
import com.royce.zyora.data.models.HydrationEntry
import java.text.SimpleDateFormat
import java.util.*

class HydrationQuickAddReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val waterAmount = intent.getIntExtra("water_amount", 250)
        
        // Add water entry
        val preferencesManager = PreferencesManager(context)
        val currentEntries = preferencesManager.getHydrationEntries().toMutableList()
        
        val newEntry = HydrationEntry(
            id = UUID.randomUUID().toString(),
            amount = waterAmount,
            containerType = ContainerType.GLASS, // Default to glass for quick add
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        )
        
        currentEntries.add(newEntry)
        preferencesManager.saveHydrationEntries(currentEntries)
        
        // Show confirmation toast
        Toast.makeText(
            context, 
            "Added ${waterAmount}ml of water! 💧", 
            Toast.LENGTH_SHORT
        ).show()
        
        // Cancel the notification since user took action
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.cancel(HydrationNotificationManager.HYDRATION_NOTIFICATION_ID)
        
        // Check if goal is achieved and show celebration
        val hydrationGoal = preferencesManager.getHydrationGoal()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayIntake = currentEntries
            .filter { it.date == today }
            .sumOf { it.amount }
            
        if (todayIntake >= hydrationGoal.dailyGoalMl) {
            // Show achievement notification
            HydrationNotificationManager.showGoalAchievedNotification(
                context,
                hydrationGoal.dailyGoalMl,
                todayIntake
            )
        }
    }
}
