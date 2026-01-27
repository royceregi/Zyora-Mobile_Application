package com.royce.zyora.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.royce.zyora.MainActivity
import com.royce.zyora.data.PreferencesManager
import com.royce.zyora.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesManager = PreferencesManager(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }
        
        binding.btnRegisterInstead.setOnClickListener {
            navigateToRegister()
        }
        
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        // Validation
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return
        }
        
        // Get stored user
        val currentUser = preferencesManager.getCurrentUser()
        
        if (currentUser == null) {
            Toast.makeText(this, "No account found. Please register first.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Simple authentication (in production, use proper authentication)
        if (currentUser.email == email && currentUser.password == password) {
            preferencesManager.setLoggedIn(true)
            Toast.makeText(this, "Welcome back, ${currentUser.username}!", Toast.LENGTH_SHORT).show()
            navigateToMain()
        } else {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
