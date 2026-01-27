package com.royce.zyora.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.royce.zyora.MainActivity
import com.royce.zyora.R
import com.royce.zyora.data.PreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home screen widget displaying daily habit completion progress.
 * Shows percentage of habits completed today and quick stats.
 */
class HabitProgressWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
    }

    companion object {
        /**
         * Updates a single widget instance with current habit data
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val preferencesManager = PreferencesManager(context)
            
            // Get today's data
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val habits = preferencesManager.getHabits()
            val habitProgress = preferencesManager.getHabitProgress()
            val todayProgress = habitProgress.filter { it.date == today }
            
            val completedCount = todayProgress.count { it.isCompleted }
            val totalCount = habits.size
            val progressPercentage = if (totalCount > 0) {
                (completedCount.toFloat() / totalCount.toFloat() * 100).toInt()
            } else 0
            
            // Get hydration data
            val hydrationEntries = preferencesManager.getHydrationEntries()
            val hydrationGoal = preferencesManager.getHydrationGoal()
            val todayIntake = hydrationEntries
                .filter { it.date == today }
                .sumOf { it.amount }
            
            // Get mood data
            val moodEntries = preferencesManager.getMoodEntries()
            val todayMood = moodEntries.find { it.date == today }
            
            // Create RemoteViews
            val views = RemoteViews(context.packageName, R.layout.widget_habit_progress)
            
            // Update widget content
            views.setTextViewText(R.id.tvWidgetDate, getFormattedDate())
            views.setTextViewText(R.id.tvWidgetProgress, "$progressPercentage%")
            views.setTextViewText(
                R.id.tvWidgetHabits,
                "$completedCount/$totalCount habits"
            )
            views.setTextViewText(
                R.id.tvWidgetHydration,
                "💧 ${todayIntake}ml / ${hydrationGoal.dailyGoalMl}ml"
            )
            views.setTextViewText(
                R.id.tvWidgetMood,
                if (todayMood != null) "${todayMood.mood.emoji} ${todayMood.mood.displayName}" else "😐 No mood logged"
            )
            
            // Set progress bar
            views.setProgressBar(R.id.progressBarWidget, 100, progressPercentage, false)
            
            // Create intent to open app when widget is clicked
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetContainer, pendingIntent)
            
            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        
        /**
         * Formats current date for widget display
         */
        private fun getFormattedDate(): String {
            val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
            return dateFormat.format(Date())
        }
        
        /**
         * Manually trigger widget update from app
         */
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, HabitProgressWidget::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
