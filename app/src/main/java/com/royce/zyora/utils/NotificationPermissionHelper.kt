package com.royce.zyora.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Helper class for managing notification permissions across different Android versions.
 * Handles runtime permission requests for POST_NOTIFICATIONS (Android 13+)
 * and SCHEDULE_EXACT_ALARM permissions.
 */
object NotificationPermissionHelper {

    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    const val EXACT_ALARM_PERMISSION_REQUEST_CODE = 1002

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are allowed by default on older versions
            true
        }
    }

    /**
     * Check if exact alarm permission is granted (Android 12+)
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Request notification permission (Android 13+)
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    /**
     * Request exact alarm permission (Android 12+)
     */
    fun requestExactAlarmPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!canScheduleExactAlarms(activity)) {
                showExactAlarmPermissionDialog(activity)
            }
        }
    }

    /**
     * Show dialog explaining why exact alarm permission is needed
     */
    private fun showExactAlarmPermissionDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Enable Exact Alarms")
            .setMessage("To receive timely reminders for habits and hydration, please enable 'Alarms & reminders' permission in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                openExactAlarmSettings(activity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Open system settings for exact alarm permission
     */
    private fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    /**
     * Show rationale dialog for notification permission
     */
    fun showNotificationPermissionRationale(activity: Activity, onAccept: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Enable Notifications")
            .setMessage("Zyora needs notification permission to send you reminders for habits and hydration goals. This helps you stay on track with your wellness journey.")
            .setPositiveButton("Enable") { _, _ ->
                onAccept()
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    /**
     * Handle permission request result
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }

    /**
     * Check all required permissions for notifications
     */
    fun checkAndRequestAllPermissions(activity: Activity, onAllGranted: () -> Unit) {
        val permissionsNeeded = mutableListOf<String>()

        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission(activity)) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isEmpty()) {
            // All permissions granted, check exact alarm
            if (!canScheduleExactAlarms(activity)) {
                requestExactAlarmPermission(activity)
            } else {
                onAllGranted()
            }
        } else {
            // Request missing permissions
            ActivityCompat.requestPermissions(
                activity,
                permissionsNeeded.toTypedArray(),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }
}
