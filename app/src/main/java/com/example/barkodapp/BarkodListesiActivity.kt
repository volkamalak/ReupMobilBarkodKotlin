package com.example.barkodapp

import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.barkodapp.adapter.BarkodAdapter
import com.example.barkodapp.api.RetrofitClient
import com.example.barkodapp.databinding.ActivityBarkodListesiBinding
import com.example.barkodapp.utils.SessionManager
import android.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class BarkodListesiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarkodListesiBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var barkodAdapter: BarkodAdapter

    private var operatorSicilNo: String? = null
    private var currentSlot: com.example.barkodapp.model.MachineSlotStatus? = null
    private var lastAddTime = 0L

    private val barkodWeights = mutableMapOf<String, Double>()
    private var isFactory87 = false
    private var agirlikLimiti = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarkodListesiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        operatorSicilNo = sessionManager.getSicilNo()

        // MainActivity'den gönderilen slot objesi
        @Suppress("DEPRECATION")
        currentSlot = intent.getParcelableExtra("SLOT_DATA")

        agirlikLimiti = intent.getIntExtra("AGIRLIK_LIMITI", 0).toDouble()
        isFactory87 = agirlikLimiti > 0

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        setupAgirlikBilgi()
        updateUI()
        //setMachineBarkodStatus(true)
        binding.etBarkod.showSoftInputOnFocus = false
        binding.etBarkod.requestFocus()
    }

    private fun setMachineBarkodStatus(status: Boolean, onComplete: (() -> Unit)? = null) {
        val machineId = intent.getIntExtra("MACHINE_ID", -1)
            .takeIf { it != -1 }
            ?: sessionManager.getMachineId()

        android.util.Log.d("BARKOD_STATUS", "setMachineBarkodStatus: machineId=$machineId, status=$status")

        if (machineId == null) {
            android.util.Log.w("BARKOD_STATUS", "machineId null, statu güncellenemiyor")
            onComplete?.invoke()
            return
        }
        lifecycleScope.launch {
            try {
                RetrofitClient.apiService.updateMachineBarkodStatus(machineId, status)
                android.util.Log.d("BARKOD_STATUS", "Statu güncellendi: machineId=$machineId, status=$status")
            } catch (e: Exception) {
                android.util.Log.e("BARKOD_STATUS", "Statu güncellenemedi: ${e.message}")
            }
            onComplete?.invoke()
        }
    }

    private fun navigateBack() {
        setMachineBarkodStatus(false) {
            finish()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            navigateBack()
        }
    }

    private fun setupRecyclerView() {
        barkodAdapter = BarkodAdapter(
            barkodList = mutableListOf(),
            barkodWeights = barkodWeights,
            isFactory87 = isFactory87,
            onDeleteClick = { barkod, position ->
                showDeleteConfirmDialog(barkod, position)
            },
            onBolClick = { barkod, originalWeight ->
                showBolDialog(barkod, originalWeight)
            }
        )
        binding.recyclerView.adapter = barkodAdapter
    }

    private fun setupAgirlikBilgi() {
        if (!isFactory87) {
            binding.cardAgirlikLimit.visibility = View.GONE
            return
        }
        binding.cardAgirlikLimit.visibility = View.VISIBLE
    }

    private fun setupListeners() {
        binding.tilBarkod.setEndIconOnClickListener { showManualEntryDialog() }

        binding.etBarkod.setOnEditorActionListener { _, actionId, event ->
            // Sadece ACTION_DOWN key event'ini dinle; IME_ACTION_DONE aynı okumada ikinci kez tetikler
            if (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
                addBarkod()
                true
            } else if (actionId == EditorInfo.IME_ACTION_DONE && event == null) {
                addBarkod()
                true
            } else {
                false
            }
        }

        binding.btnGonder.setOnClickListener {
            sendBarkodList()
        }
    }

    private fun addBarkod() {
        val now = System.currentTimeMillis()
        if (now - lastAddTime < 300) return
        lastAddTime = now

        val barkod = binding.etBarkod.text.toString().trim()
        val isEmri = intent.getStringExtra("IS_EMRI") ?: currentSlot?.orderNumber

        if (barkod.isEmpty()) {
            showSnackbar("Lütfen barkod numarası giriniz")
            return
        }

        if (barkod.length != 10) {
            binding.etBarkod.text?.clear()
            showWarningDialog(
                "\"$barkod\"\n\nBu barkod ${barkod.length} karakter içeriyor.\nBarkod uzunluğu tam olarak 10 karakter olmalıdır."
            ) {
                binding.etBarkod.requestFocus()
            }
            return
        }

        if (isEmri.isNullOrBlank()) {
            showErrorDialog("İş emri bilgisi bulunamadı")
            return
        }

        if (barkodAdapter.getBarkodList().contains(barkod)) {
            binding.etBarkod.text?.clear()
            showWarningDialog("\"$barkod\"\n\nBu barkod zaten listede mevcut.") {
                binding.etBarkod.requestFocus()
            }
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                val machineId = intent.getIntExtra("MACHINE_ID", -1)
                    .takeIf { it != -1 }
                    ?: sessionManager.getMachineId()
                    ?: 0

                val request = com.example.barkodapp.model.BarkodRequest(isEmri, barkod, machineId)
                val response = RetrofitClient.apiService.kontrolIsEmriBarkod(request)

                showLoading(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true) {
                        val barkodAgirlik = body.weight ?: 0.0
                        if (barkodAgirlik > 0) {
                            barkodWeights[barkod] = barkodAgirlik
                        }

                        barkodAdapter.addBarkod(barkod)
                        binding.etBarkod.text?.clear()
                        binding.etBarkod.requestFocus()
                        updateUI()
                        showSnackbar("Barkod eklendi")
                    } else {
                        binding.etBarkod.text?.clear()
                        showWarningDialog("\"$barkod\"\n\n${body?.message ?: "Barkod bulunamadı"}") {
                            binding.etBarkod.requestFocus()
                        }
                    }
                } else {
                    showErrorDialog("API hatası: ${response.code()}")
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
                barkodWeights.remove(barkod)
                barkodAdapter.removeBarkod(position)
                updateUI()
                showSnackbar("Barkod silindi")
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun showBolDialog(barkod: String, originalWeight: Double) {
        val etMiktar = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Kullanılacak miktar (max %.1f kg)".format(originalWeight)
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Balya Böl")
            .setMessage("Barkod: $barkod\nOrijinal ağırlık: %.1f kg\n\nKullanılacak miktarı girin:".format(originalWeight))
            .setView(etMiktar)
            .setPositiveButton("Uygula") { _, _ ->
                val girilenStr = etMiktar.text.toString().trim()
                val girilenMiktar = girilenStr.toDoubleOrNull()
                when {
                    girilenMiktar == null || girilenMiktar <= 0 -> {
                        showErrorDialog("Geçerli bir miktar giriniz")
                    }
                    girilenMiktar >= originalWeight -> {
                        showErrorDialog(
                            "Girilen miktar (%.1f kg) orijinal ağırlıktan (%.1f kg) küçük olmalıdır".format(
                                girilenMiktar, originalWeight
                            )
                        )
                    }
                    else -> {
                        barkodWeights[barkod] = girilenMiktar
                        val idx = barkodAdapter.getBarkodList().indexOf(barkod)
                        if (idx != -1) barkodAdapter.notifyItemChanged(idx)
                        updateUI()
                        showSnackbar("Ağırlık %.1f kg olarak güncellendi".format(girilenMiktar))
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun sendBarkodList() {
        val barkodList = barkodAdapter.getBarkodList()

        if (barkodList.isEmpty()) {
            showSnackbar("Liste boş. Lütfen barkod ekleyiniz")
            return
        }

        val orderNumber = currentSlot?.orderNumber
        val machineName = sessionManager.getMachineName()
        val persNo = sessionManager.getSicilNo()
        val lot = currentSlot?.lot
        val machineWorkOrderRunId = currentSlot?.workOrderRunId

        if (orderNumber.isNullOrBlank()) {
            showErrorDialog("İş emri bilgisi bulunamadı")
            return
        }
        if (machineName.isNullOrBlank()) {
            showErrorDialog("Makine bilgisi bulunamadı")
            return
        }
        if (persNo.isNullOrBlank()) {
            showErrorDialog("Personel sicil bilgisi bulunamadı")
            return
        }
        if (machineWorkOrderRunId == null) {
            showErrorDialog("İş emri run ID bilgisi bulunamadı")
            return
        }
        if (lot == null) {
            showErrorDialog("Lot bilgisi bulunamadı")
            return
        }

        if (isFactory87) {
            val toplamAgirlik = barkodWeights.values.sum()
            if (toplamAgirlik > agirlikLimiti) {
                AlertDialog.Builder(this)
                    .setTitle("Ağırlık Limiti Aşıldı")
                    .setMessage(
                        "Toplam ağırlık (%.1f kg) seçilen limiti (${agirlikLimiti.toInt()} kg) aşıyor!\n\nYine de göndermek istiyor musunuz?".format(toplamAgirlik)
                    )
                    .setPositiveButton("Evet, Gönder") { _, _ ->
                        doSendBarkodList(barkodList, orderNumber!!, lot!!, machineName!!, persNo!!, machineWorkOrderRunId!!)
                    }
                    .setNegativeButton("İptal", null)
                    .show()
                return
            }
        }

        doSendBarkodList(barkodList, orderNumber!!, lot!!, machineName!!, persNo!!, machineWorkOrderRunId!!)
    }

    private fun doSendBarkodList(
        barkodList: List<String>,
        orderNumber: String,
        lot: String,
        machineName: String,
        persNo: String,
        machineWorkOrderRunId: Int
    ) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                val barcodeItems = barkodList.map { barkod ->
                    com.example.barkodapp.model.BarcodeItem(
                        barcode = barkod,
                        weight = barkodWeights[barkod] ?: 0.0
                    )
                }
                val request = com.example.barkodapp.model.SendBarcodesRequest(
                    orderNumber = orderNumber,
                    lot = lot,
                    machineName = machineName,
                    movType = "261",
                    persNo = persNo,
                    barcodes = barcodeItems,
                    workOrderRunId = machineWorkOrderRunId
                )
                val response = RetrofitClient.apiService.sendBarcodes(request)

                showLoading(false)

                if (response.isSuccessful && (response.body()?.insertedCount ?: 0) > 0) {
                    android.widget.Toast.makeText(
                        this@BarkodListesiActivity,
                        "Liste başarıyla gönderildi",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    navigateBack()
                } else {
                    showErrorDialog("Gönderme işlemi başarısız oldu")
                }
            } catch (e: Exception) {
                showLoading(false)
                showErrorDialog("Bağlantı hatası: ${e.message}")
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.btnGonder)
            .show()
    }

    private fun showManualEntryDialog() {
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val tilManuel = com.google.android.material.textfield.TextInputLayout(
            this, null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Barkod Numarası"
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val etManuel = com.google.android.material.textfield.TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            imeOptions = EditorInfo.IME_ACTION_DONE
            maxLines = 1
            textSize = 18f
        }
        tilManuel.addView(etManuel)

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(pad, pad, pad, (8 * dp).toInt())
            addView(tilManuel)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Manuel Barkod Girişi")
            .setView(container)
            .setPositiveButton("Tamam") { _, _ ->
                val barkod = etManuel.text.toString().trim()
                if (barkod.isNotEmpty()) {
                    binding.etBarkod.setText(barkod)
                    addBarkod()
                }
            }
            .setNegativeButton("İptal", null)
            .create()

        etManuel.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val barkod = etManuel.text.toString().trim()
                if (barkod.isNotEmpty()) {
                    binding.etBarkod.setText(barkod)
                    dialog.dismiss()
                    addBarkod()
                }
                true
            } else false
        }

        dialog.setOnDismissListener { hideKeyboard() }

        dialog.show()
        etManuel.requestFocus()
        etManuel.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etManuel, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun showWarningDialog(message: String, onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(this)
            .setTitle("Uyarı")
            .setMessage(message)
            .setPositiveButton("Tamam") { _, _ ->
                hideKeyboard()
                onDismiss?.invoke()
            }
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Hata")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.etBarkod.isEnabled = !show
        binding.btnGonder.isEnabled = !show
    }

    private fun updateUI() {
        val count = barkodAdapter.itemCount
        binding.tvBarkodSayisi.text = "($count)"

        if (count == 0) {
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }

        if (isFactory87) {
            val toplamAgirlik = barkodWeights.values.sum()
            binding.tvToplamAgirlik.text = "%.1f / ${agirlikLimiti.toInt()} kg".format(toplamAgirlik)
        }
    }

    fun getOperatorSicilNo(): String? {
        return operatorSicilNo
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        navigateBack()
    }
}
