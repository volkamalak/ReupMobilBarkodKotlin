package com.example.barkodapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.barkodapp.databinding.ActivityMainBinding
import com.example.barkodapp.utils.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Sicil numarasını göster
        val sicilNo = sessionManager.getSicilNo()
        binding.tvSicilNo.text = "Sicil: $sicilNo"

        setupListeners()
    }

    private fun setupListeners() {
        // Barkod Listesi butonuna tıklanınca
        binding.btnBarkodListesi.setOnClickListener {
            val intent = Intent(this, BarkodListesiActivity::class.java)
            startActivity(intent)
        }

        // Çıkış butonuna tıklanınca
        binding.btnCikis.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Çıkış")
            .setMessage("Çıkış yapmak istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun performLogout() {
        // Session'ı temizle
        sessionManager.logout()

        // Login ekranına yönlendir
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Geri tuşuna basıldığında uygulamadan çıkmasın
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Geri tuşunu devre dışı bırak
    }
}
