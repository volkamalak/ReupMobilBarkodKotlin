package com.example.barkodapp

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.barkodapp.databinding.ActivityMainBinding
import com.example.barkodapp.utils.SessionManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar
        setSupportActionBar(binding.toolbar)

        sessionManager = SessionManager(this)

        // Navigation Drawer
        toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // Load user and work order info
        loadDashboardData()
    }

    private fun loadDashboardData() {
        // Show loading state initially
        binding.tvSicilNo.text = "Sicil: Yükleniyor..."
        binding.tvIsEmri.text = "İş Emri No: Yükleniyor..."
        binding.tvOkutulanBalya.text = "Okutulan Balya: Yükleniyor..."
        binding.tvSonBalya.text = "Son Balya No: Yükleniyor..."

        // Simulate a network call to fetch data
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500) // Simulate network delay

            // Populate with real data from session
            val sicilNo = sessionManager.getSicilNo()
            binding.tvSicilNo.text = "Sicil: ${sicilNo ?: "N/A"}"

            // Populate with dummy data for now
            binding.tvIsEmri.text = "İş Emri No: X-12345-A"
            binding.tvOkutulanBalya.text = "Okutulan Balya: 125"
            binding.tvSonBalya.text = "Son Balya No: B-789-C"
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_balya_okuma -> {
                val intent = Intent(this, BarkodListesiActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_guvenli_cikis -> {
                showLogoutDialog()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Güvenli Çıkış")
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // Geri tuşunu devre dışı bırak (orijinal davranış)
        }
    }
}
