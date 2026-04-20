package com.example.barkodapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.barkodapp.api.RetrofitClient
import com.example.barkodapp.databinding.ActivityLoginBinding
import com.example.barkodapp.model.Machine
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

        // Daha önce giriş yapıldıysa direkt ana sayfaya
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity()
            return
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener { performLogin() }

        binding.etSicilNo.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                performLogin()
                true
            } else false
        }
    }

    private fun performLogin() {
        val sicilNo = binding.etSicilNo.text.toString().trim()

        if (sicilNo.isEmpty()) {
            Toast.makeText(this, "Lütfen sicil numaranızı giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getPersonel(sicilNo)
                showLoading(false)

                if (response.isSuccessful && response.body() != null) {
                    val personel = response.body()!!

                    if (personel.persNo.isNullOrBlank()) {
                        Toast.makeText(this@LoginActivity, "Sicil numarası bulunamadı", Toast.LENGTH_LONG).show()
                    } else {
                        // Personel bilgilerini kaydet
                        sessionManager.saveSicilNo(sicilNo)
                        sessionManager.savePersName(personel.persName ?: "")
                        sessionManager.saveYetkiler(
                            barkod = personel.mobilYetkiBarkodOkutma ?: false,
                            depo   = personel.mobilYetkiDepoTransfer ?: false
                        )
                        // Barkod okutma yetkisi varsa makine seçim dialogu, yoksa direkt ana sayfa
                        if (personel.mobilYetkiBarkodOkutma == true) {
                            showMachineSelectionDialog()
                        } else {
                            navigateToMainActivity()
                        }
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Sicil numarası bulunamadı", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "Bağlantı hatası: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showMachineSelectionDialog() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllMachines()
                showLoading(false)

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val machines: List<Machine> = response.body()!!
                    displayMachineDialog(machines)
                } else {
                    Toast.makeText(this@LoginActivity, "Makine listesi alınamadı", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "Makine listesi hatası: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayMachineDialog(machines: List<Machine>) {
        val spinner = Spinner(this)
        val machineNames = machines.map { it.machineName ?: "Bilinmiyor" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, machineNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Spinner'a padding ekle
        val padding = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
        spinner.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Makine Seçimi")
            .setMessage("Lütfen çalışacağınız makineyi seçin:")
            .setView(spinner)
            .setCancelable(false)
            .setPositiveButton("Devam Et") { _, _ ->
                val selectedIndex = spinner.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < machines.size) {
                    val selected = machines[selectedIndex]
                    sessionManager.saveMachine(selected.id ?: 0, selected.machineName ?: "")
                    navigateToMainActivity()
                }
            }
            .show()
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

