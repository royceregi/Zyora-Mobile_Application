package com.royce.zyora.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.royce.zyora.MainActivity
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.databinding.ActivityGetStartedBinding

class GetStartedActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityGetStartedBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesManager = PreferencesManager(this)
        
        // Apply theme preference
        if (preferencesManager.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        // Check if user is already logged in
        if (preferencesManager.isLoggedIn()) {
            navigateToMain()
            return
        }
        
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnGetStarted.setOnClickListener {
            navigateToRegister()
        }
        
        binding.btnLogin.setOnClickListener {
            navigateToLogin()
        }
    }
    
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
