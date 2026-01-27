package com.royce.zyora

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.databinding.ActivityMainBinding
import com.royce.zyora.ui.auth.GetStartedActivity
import com.royce.zyora.ui.fragments.HomeFragment
import com.royce.zyora.ui.fragments.HabitTrackerFragment
import com.royce.zyora.ui.fragments.HydrationReminderFragment
import com.royce.zyora.ui.fragments.MoodJournalFragment
import com.royce.zyora.ui.fragments.SettingsFragment
import com.royce.zyora.utils.NotificationPermissionHelper

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // Request notification permissions on first launch
        if (preferencesManager.isFirstLaunch()) {
            NotificationPermissionHelper.checkAndRequestAllPermissions(this) {
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
            }
            preferencesManager.setFirstLaunch(false)
        }
        
        // Apply theme preference
        if (preferencesManager.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        // Check if user is logged in
        if (!preferencesManager.isLoggedIn()) {
            navigateToGetStarted()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupBottomNavigation()
        
        // Load default fragment or handle intent navigation
        if (savedInstanceState == null) {
            // Check if we should open hydration tab from notification
            if (intent.getBooleanExtra("open_hydration_tab", false)) {
                loadFragment(HydrationReminderFragment())
                binding.bottomNavigation.selectedItemId = R.id.nav_hydration
            } else {
                loadFragment(HomeFragment())
            }
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_habits -> {
                    loadFragment(HabitTrackerFragment())
                    true
                }
                R.id.nav_mood -> {
                    loadFragment(MoodJournalFragment())
                    true
                }
                R.id.nav_hydration -> {
                    loadFragment(HydrationReminderFragment())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
    
    private fun navigateToGetStarted() {
        val intent = Intent(this, GetStartedActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NotificationPermissionHelper.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
                // Check exact alarm permission
                NotificationPermissionHelper.requestExactAlarmPermission(this)
            },
            onDenied = {
                Toast.makeText(
                    this,
                    "Notifications disabled. You won't receive reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }
}