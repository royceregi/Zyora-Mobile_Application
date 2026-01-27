package com.royce.zyora.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Centralized error handling utility for the app.
 * Provides consistent error logging and user feedback.
 */
object ErrorHandler {

    private const val TAG = "ZyoraError"

    /**
     * Handle and log an error with user feedback
     */
    fun handleError(
        context: Context,
        error: Throwable,
        userMessage: String = "An error occurred. Please try again.",
        showDialog: Boolean = false
    ) {
        // Log the error
        Log.e(TAG, "Error: ${error.message}", error)
        
        // Show user feedback
        if (showDialog) {
            showErrorDialog(context, userMessage, error.message)
        } else {
            Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Show detailed error dialog
     */
    private fun showErrorDialog(context: Context, userMessage: String, technicalMessage: String?) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Error")
            .setMessage(userMessage + if (technicalMessage != null) "\n\nDetails: $technicalMessage" else "")
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Handle network errors specifically
     */
    fun handleNetworkError(context: Context) {
        Toast.makeText(
            context,
            "Network error. Please check your connection.",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Handle data validation errors
     */
    fun handleValidationError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Log info message
     */
    fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Log warning message
     */
    fun logWarning(message: String) {
        Log.w(TAG, message)
    }
}
