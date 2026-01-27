package com.royce.zyora.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.databinding.FragmentSettingsBinding
import com.royce.zyora.ui.auth.GetStartedActivity
import com.royce.zyora.ui.auth.ProfileActivity

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        preferencesManager = PreferencesManager(requireContext())
        
        setupUI()
        loadUserInfo()
    }
    
    private fun setupUI() {
        // Profile section
        binding.cardProfile.setOnClickListener {
            navigateToProfile()
        }
        
        // Theme toggle
        binding.switchDarkMode.isChecked = preferencesManager.isDarkMode()
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            toggleTheme(isChecked)
        }
        
        binding.cardTheme.setOnClickListener {
            binding.switchDarkMode.toggle()
        }
        
        // Notifications toggle
        binding.switchNotifications.isChecked = preferencesManager.isNotificationsEnabled()
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setNotificationsEnabled(isChecked)
            Toast.makeText(requireContext(), 
                if (isChecked) "Notifications enabled" else "Notifications disabled", 
                Toast.LENGTH_SHORT).show()
        }
        
        binding.cardNotifications.setOnClickListener {
            binding.switchNotifications.toggle()
        }
        
        // Export data
        binding.cardExportData.setOnClickListener {
            exportAllData()
        }
        
        // Clear data
        binding.cardClearData.setOnClickListener {
            showClearDataDialog()
        }
        
        // About
        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }
        
        // Logout
        binding.cardLogout.setOnClickListener {
            showLogoutDialog()
        }
    }
    
    private fun loadUserInfo() {
        val user = preferencesManager.getCurrentUser()
        user?.let {
            binding.tvUserName.text = it.username
            binding.tvUserEmail.text = it.email
        }
    }
    
    private fun navigateToProfile() {
        val intent = Intent(requireContext(), ProfileActivity::class.java)
        startActivity(intent)
    }
    
    private fun toggleTheme(isDarkMode: Boolean) {
        preferencesManager.setDarkMode(isDarkMode)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        Toast.makeText(requireContext(), 
            if (isDarkMode) "Dark mode enabled" else "Light mode enabled", 
            Toast.LENGTH_SHORT).show()
    }
    
    private fun exportAllData() {
        val habits = preferencesManager.getHabits()
        val habitProgress = preferencesManager.getHabitProgress()
        val moods = preferencesManager.getMoodEntries()
        val hydration = preferencesManager.getHydrationEntries()
        
        val csvContent = buildString {
            append("=== ZYORA DATA EXPORT ===\n\n")
            
            // Habits
            append("HABITS:\n")
            append("Name,Description,Target,Unit,Category\n")
            habits.forEach { habit ->
                append("\"${habit.name}\",\"${habit.description}\",${habit.targetCount},\"${habit.unit}\",\"${habit.category}\"\n")
            }
            append("\n")
            
            // Habit Progress
            append("HABIT PROGRESS:\n")
            append("Date,Habit Name,Current Count,Target Count,Completed\n")
            habitProgress.forEach { progress ->
                val habit = habits.find { it.id == progress.habitId }
                append("${progress.date},\"${habit?.name ?: "Unknown"}\",${progress.currentCount},${progress.targetCount},${progress.isCompleted}\n")
            }
            append("\n")
            
            // Moods
            append("MOOD ENTRIES:\n")
            append("Date,Time,Mood,Intensity,Note\n")
            moods.forEach { mood ->
                append("${mood.date},${mood.time},\"${mood.mood.displayName}\",${mood.intensity},\"${mood.note}\"\n")
            }
            append("\n")
            
            // Hydration
            append("HYDRATION ENTRIES:\n")
            append("Date,Time,Amount,Container Type\n")
            hydration.forEach { entry ->
                val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(entry.timestamp))
                append("${entry.date},$time,${entry.amount},\"${entry.containerType.displayName}\"\n")
            }
        }
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/csv"
            putExtra(Intent.EXTRA_TEXT, csvContent)
            putExtra(Intent.EXTRA_SUBJECT, "Zyora Complete Data Export")
        }
        
        startActivity(Intent.createChooser(shareIntent, "Export All Data"))
    }
    
    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Data")
            .setMessage("This will permanently delete all your habits, mood entries, and hydration data. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearAllData() {
        // Clear all data except user info and preferences
        preferencesManager.saveHabits(emptyList())
        preferencesManager.saveHabitProgress(emptyList())
        preferencesManager.saveMoodEntries(emptyList())
        preferencesManager.saveHydrationEntries(emptyList())
        
        Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show()
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About Zyora")
            .setMessage("""
                Zyora - Your Personal Wellness Companion
                
                Version: 1.0.0
                
                Features:
                • Habit Tracking
                • Mood Journaling
                • Hydration Reminders
                • Progress Analytics
                • Data Export
                
                Designed with Material Design principles and wellness psychology research to help you build healthier habits and track your emotional well-being.
                
                © 2024 Zyora Team
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? Your data will be saved locally.")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun logout() {
        preferencesManager.logout()
        
        val intent = Intent(requireContext(), GetStartedActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        requireActivity().finish()
    }
    
    override fun onResume() {
        super.onResume()
        loadUserInfo() // Refresh user info when returning from profile
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
