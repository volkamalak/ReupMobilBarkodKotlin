package com.example.barkodapp

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.barkodapp.api.RetrofitClient
import com.example.barkodapp.databinding.ActivityLoginBinding
import com.example.barkodapp.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Eğer kullanıcı daha önce giriş yapmışsa direkt ana sayfaya yönlendir
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        // Giriş butonu tıklanınca
        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        // Enter tuşuna basınca giriş yap
        binding.etSicilNo.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performLogin()
                true
            } else {
                false
            }
        }
    }

    private fun performLogin() {
        val sicilNo = binding.etSicilNo.text.toString().trim()

        if (sicilNo.isEmpty()) {
            Toast.makeText(this, "Lütfen sicil numaranızı giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        // Loading göster
        showLoading(true)

        // API çağrısı yap
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.loginOperator(sicilNo)

                showLoading(false)

                if (response.isSuccessful && response.body() == true) {
                    // Başarılı giriş
                    sessionManager.saveSicilNo(sicilNo)
                    Toast.makeText(this@LoginActivity, "Giriş başarılı", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    // Sicil bulunamadı
                    Toast.makeText(this@LoginActivity, "Sicil numarası bulunamadı", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Bağlantı hatası: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
        binding.etSicilNo.isEnabled = !show
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
