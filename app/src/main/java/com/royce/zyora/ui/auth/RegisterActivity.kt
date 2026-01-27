package com.royce.zyora.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.royce.zyora.MainActivity
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.data.models.User
import com.royce.zyora.databinding.ActivityRegisterBinding
import java.util.*

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnRegister.setOnClickListener {
            handleRegister()
        }
        
        binding.btnLoginInstead.setOnClickListener {
            navigateToLogin()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun handleRegister() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()
        
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
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return
        }
        
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return
        }
        
        // Create user
        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            email = email,
            password = password, // In production, hash this
            createdAt = System.currentTimeMillis()
        )
        
        // Save user and login
        preferencesManager.saveUser(user)
        preferencesManager.setLoggedIn(true)
        preferencesManager.setFirstLaunch(false)
        
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
        
        navigateToMain()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
