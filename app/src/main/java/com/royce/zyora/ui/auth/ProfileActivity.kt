package com.royce.zyora.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.data.models.User
import com.royce.zyora.databinding.ActivityProfileBinding
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    private lateinit var preferencesManager: PreferencesManager
    private var isEditMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupUI()
        loadUserData()
    }
    
    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.btnEdit.setOnClickListener {
            toggleEditMode()
        }
        
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
        
        binding.btnCancel.setOnClickListener {
            cancelEdit()
        }
    }
    
    private fun loadUserData() {
        val user = preferencesManager.getCurrentUser()
        user?.let {
            binding.etUsername.setText(it.username)
            binding.etEmail.setText(it.email)
            
            // Format join date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.tvJoinDate.text = "Member since ${dateFormat.format(Date(it.createdAt))}"
            
            // Load statistics
            loadUserStats()
        }
        
        setEditMode(false)
    }
    
    private fun loadUserStats() {
        val habits = preferencesManager.getHabits()
        val moodEntries = preferencesManager.getMoodEntries()
        val hydrationEntries = preferencesManager.getHydrationEntries()
        val habitProgress = preferencesManager.getHabitProgress()
        
        // Calculate stats
        val totalHabits = habits.size
        val totalMoods = moodEntries.size
        val totalHydrationLogs = hydrationEntries.size
        val completedHabits = habitProgress.count { it.isCompleted }
        
        // Calculate streak (simplified - consecutive days with completed habits)
        val streak = calculateStreak()
        
        binding.apply {
            tvTotalHabits.text = totalHabits.toString()
            tvCompletedHabits.text = completedHabits.toString()
            tvMoodEntries.text = totalMoods.toString()
            tvHydrationLogs.text = totalHydrationLogs.toString()
            tvCurrentStreak.text = "$streak days"
        }
    }
    
    private fun calculateStreak(): Int {
        val habitProgress = preferencesManager.getHabitProgress()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())
        
        var streak = 0
        val calendar = Calendar.getInstance()
        
        // Check backwards from today
        for (i in 0..30) { // Check last 30 days max
            val checkDate = dateFormat.format(calendar.time)
            val dayProgress = habitProgress.filter { it.date == checkDate }
            
            if (dayProgress.isNotEmpty() && dayProgress.any { it.isCompleted }) {
                streak++
            } else if (checkDate != today) { // Don't break streak on today if no habits completed yet
                break
            }
            
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        
        return streak
    }
    
    private fun toggleEditMode() {
        setEditMode(!isEditMode)
    }
    
    private fun setEditMode(editMode: Boolean) {
        isEditMode = editMode
        
        binding.apply {
            etUsername.isEnabled = editMode
            etEmail.isEnabled = editMode
            
            btnEdit.visibility = if (editMode) android.view.View.GONE else android.view.View.VISIBLE
            btnSave.visibility = if (editMode) android.view.View.VISIBLE else android.view.View.GONE
            btnCancel.visibility = if (editMode) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        
        // Validation
        if (username.isEmpty()) {
            binding.etUsername.error = "Username is required"
            return
        }
        
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Invalid email format"
            return
        }
        
        // Update user
        val currentUser = preferencesManager.getCurrentUser()
        currentUser?.let { user ->
            val updatedUser = user.copy(
                username = username,
                email = email
            )
            
            preferencesManager.saveUser(updatedUser)
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            setEditMode(false)
        }
    }
    
    private fun cancelEdit() {
        loadUserData() // Reload original data
        setEditMode(false)
    }
}
