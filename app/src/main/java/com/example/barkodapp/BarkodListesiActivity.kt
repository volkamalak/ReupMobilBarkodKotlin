package com.example.barkodapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.barkodapp.adapter.BarkodAdapter
import com.example.barkodapp.api.RetrofitClient
import com.example.barkodapp.databinding.ActivityBarkodListesiBinding
import com.example.barkodapp.utils.SessionManager
import kotlinx.coroutines.launch

class BarkodListesiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarkodListesiBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var barkodAdapter: BarkodAdapter

    // Sicil numarasını global değişkende saklıyoruz (requirement 11)
    private var operatorSicilNo: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarkodListesiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        operatorSicilNo = sessionManager.getSicilNo()

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        updateUI()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish() // Ana sayfaya dön
        }
    }

    private fun setupRecyclerView() {
        barkodAdapter = BarkodAdapter(
            barkodList = mutableListOf(),
            onDeleteClick = { barkod, position ->
                showDeleteConfirmDialog(barkod, position)
            }
        )
        binding.recyclerView.adapter = barkodAdapter
    }

    private fun setupListeners() {
        // Enter tuşuna basınca barkod ekle
        binding.etBarkod.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                addBarkod()
                true
            } else {
                false
            }
        }

        // Gönder butonu
        binding.btnGonder.setOnClickListener {
            sendBarkodList()
        }
    }

    private fun addBarkod() {
        val barkod = binding.etBarkod.text.toString().trim()

        if (barkod.isEmpty()) {
            Toast.makeText(this, "Lütfen barkod numarası giriniz", Toast.LENGTH_SHORT).show()
            return
        }

        // Barkod doğrulama servisi çağır
        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.validateBarcode(barkod)

                showLoading(false)

                if (response.isSuccessful && response.body() == true) {
                    // Barkod geçerli, listeye ekle
                    barkodAdapter.addBarkod(barkod)
                    binding.etBarkod.text?.clear()
                    binding.etBarkod.requestFocus()
                    updateUI()
                    Toast.makeText(this@BarkodListesiActivity, "Barkod eklendi", Toast.LENGTH_SHORT).show()
                } else {
                    // Barkod bulunamadı
                    showErrorDialog("Barkod bulunamadı")
                }
            } catch (e: Exception) {
                showLoading(false)
                showErrorDialog("Bağlantı hatası: ${e.message}")
            }
        }
    }

    private fun showDeleteConfirmDialog(barkod: String, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Barkod Sil")
            .setMessage("$barkod numaralı barkodu silmek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                barkodAdapter.removeBarkod(position)
                updateUI()
                Toast.makeText(this, "Barkod silindi", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun sendBarkodList() {
        val barkodList = barkodAdapter.getBarkodList()

        if (barkodList.isEmpty()) {
            Toast.makeText(this, "Liste boş. Lütfen barkod ekleyiniz", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.sendBarcodes(barkodList)

                showLoading(false)

                if (response.isSuccessful && response.body() == true) {
                    // Başarılı, listeyi temizle ve textbox'a fokuslan
                    Toast.makeText(this@BarkodListesiActivity, "Liste başarıyla gönderildi", Toast.LENGTH_SHORT).show()
                    barkodAdapter.clearAll()
                    updateUI()
                    binding.etBarkod.requestFocus()
                } else {
                    showErrorDialog("Gönderme işlemi başarısız oldu")
                }
            } catch (e: Exception) {
                showLoading(false)
                showErrorDialog("Bağlantı hatası: ${e.message}")
            }
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Hata")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.etBarkod.isEnabled = !show
        binding.btnGonder.isEnabled = !show
    }

    private fun updateUI() {
        val count = barkodAdapter.itemCount
        binding.tvBarkodSayisi.text = "($count)"

        // Liste boş ise mesaj göster
        if (count == 0) {
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    // Operatör sicil numarasına her yerden erişebilirsiniz
    fun getOperatorSicilNo(): String? {
        return operatorSicilNo
    }
}
