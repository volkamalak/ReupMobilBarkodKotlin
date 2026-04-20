package com.example.barkodapp

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.barkodapp.api.RetrofitClient
import com.example.barkodapp.databinding.ActivityMainBinding
import com.example.barkodapp.model.Machine
import com.example.barkodapp.model.MachineSlotStatus
import com.example.barkodapp.utils.AppUpdater
import com.example.barkodapp.utils.SessionManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var toggle: ActionBarDrawerToggle
    private var autoRefreshJob: Job? = null

    companion object {
        private const val FACTORY_87 = "87"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn() || sessionManager.getSicilNo().isNullOrBlank()) {
            navigateToLoginAndClearTask()
            return
        }

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        // İkon renginin temayla uyumlu olması için:
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.colorOnPrimary)

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)
        applyMenuPermissions()

        setupDashboardInfo()
        loadMachineSlots()
        startAutoRefresh()

        // GEÇİCİ DEBUG: Mevcut versionCode'u göster
        Toast.makeText(this, "Uygulama versionCode: ${BuildConfig.VERSION_CODE}", Toast.LENGTH_LONG).show()
    }

    private fun applyMenuPermissions() {
        val menu = binding.navView.menu
        menu.findItem(R.id.nav_makine_secimi)?.isVisible = sessionManager.hasYetkiBarkodOkutma()
        menu.findItem(R.id.nav_depo_transfer)?.isVisible = sessionManager.hasYetkiDepoTransfer()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(30_000)
                loadMachineSlots()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        autoRefreshJob?.cancel()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppUpdater.REQUEST_INSTALL_APK) {
            // RESULT_OK birçok cihazda gelmiyor — gerçek versionCode'u kontrol et
            val installedVersionCode = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(packageName, 0).versionCode
                }
            } catch (e: Exception) {
                BuildConfig.VERSION_CODE
            }

            if (installedVersionCode > BuildConfig.VERSION_CODE) {
                // Yeni sürüm gerçekten kuruldu — kaydet ve uygulamayı yeniden başlat
                AppUpdater.confirmInstall(this)
                val restart = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(restart)
                finishAffinity()
            } else {
                AppUpdater.cancelInstall(this)
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Yükleme iptal edildi.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupDashboardInfo() {
        val persName = sessionManager.getPersName()
        val sicilNo = sessionManager.getSicilNo()
        val machineName = sessionManager.getMachineName()

        binding.tvPersName.text = if (!persName.isNullOrBlank()) persName else "Sicil: $sicilNo"
        binding.tvMachineName.text = if (!machineName.isNullOrBlank()) machineName else "Seçilmedi"
    }

    private fun loadMachineSlots() {
        if (!sessionManager.hasYetkiBarkodOkutma()) {
            showSlotsEmpty("Barkod okutma yetkiniz bulunmamaktadır.")
            return
        }

        val machineId = sessionManager.getMachineId()

        android.util.Log.d("SLOTS_DEBUG", "loadMachineSlots çalıştı. machineId=$machineId")

        if (machineId == null) {
            showSlotsEmpty("Makine seçili değil")
            return
        }

        binding.progressBarSlots.visibility = View.VISIBLE
        binding.layoutSlots.visibility = View.GONE
        binding.tvSlotsEmpty.visibility = View.GONE
        binding.layoutSlots.removeAllViews()

        lifecycleScope.launch {
            try {
                android.util.Log.d("SLOTS_DEBUG", "API isteği gönderiliyor: MachineSlotStatus?machineId=$machineId")
                val response = RetrofitClient.apiService.getMachineSlotStatus(machineId)

                binding.progressBarSlots.visibility = View.GONE

                android.util.Log.d("SLOTS_DEBUG", "HTTP Status: ${response.code()}")
                android.util.Log.d("SLOTS_DEBUG", "Response başarılı mı: ${response.isSuccessful}")
                android.util.Log.d("SLOTS_DEBUG", "Body null mu: ${response.body() == null}")
                android.util.Log.d("SLOTS_DEBUG", "Body boş mu: ${response.body()?.isEmpty()}")
                android.util.Log.d("SLOTS_DEBUG", "Body içeriği: ${response.body()}")
                if (!response.isSuccessful) {
                    android.util.Log.e("SLOTS_DEBUG", "Hata Body: ${response.errorBody()?.string()}")
                }

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val slots = response.body()!!
                    android.util.Log.d("SLOTS_DEBUG", "Slot sayısı: ${slots.size}")
                    binding.layoutSlots.visibility = View.VISIBLE
                    renderSlotCards(slots)
                } else {
                    val msg = if (!response.isSuccessful)
                        "API Hatası: HTTP ${response.code()}"
                    else
                        "Makineye ait iş emri bulunamadı (boş liste)"
                    android.util.Log.w("SLOTS_DEBUG", msg)
                    Toast.makeText(this@MainActivity, "DEBUG: $msg", Toast.LENGTH_LONG).show()
                    showSlotsEmpty(msg)
                }
            } catch (e: Exception) {
                binding.progressBarSlots.visibility = View.GONE
                android.util.Log.e("SLOTS_DEBUG", "Exception: ${e.javaClass.simpleName} - ${e.message}")
                val msg = "Bağlantı hatası: ${e.message}"
                Toast.makeText(this@MainActivity, "DEBUG: $msg", Toast.LENGTH_LONG).show()
                showSlotsEmpty(msg)
            }
        }
    }

    private fun showSlotsEmpty(message: String) {
        binding.tvSlotsEmpty.text = message
        binding.tvSlotsEmpty.visibility = View.VISIBLE
        binding.layoutSlots.visibility = View.GONE
    }

    private fun renderSlotCards(slots: List<MachineSlotStatus>) {
        // En fazla 2 kart gösterecek şekilde UI ayarlaması yapılıyor,
        // weight=1 sayesinde yan yana Layout'ta eşit yer kaplayacaklar.
        for (slot in slots.take(2)) {
            val cardView = layoutInflater.inflate(R.layout.item_slot_card, binding.layoutSlots, false) as MaterialCardView
            
            val tvSlotName = cardView.findViewById<TextView>(R.id.tvSlotName)
            val tvStatusIcon = cardView.findViewById<TextView>(R.id.tvStatusIcon)
            val tvStatus = cardView.findViewById<TextView>(R.id.tvStatus)
            val btnAction = cardView.findViewById<MaterialButton>(R.id.btnAction)
            
            val layoutDetails = cardView.findViewById<View>(R.id.layoutDetails)
            val tvIdleMessage = cardView.findViewById<View>(R.id.tvIdleMessage)

            val tvOrderNumber = cardView.findViewById<TextView>(R.id.tvOrderNumber)
            val tvLot = cardView.findViewById<TextView>(R.id.tvLot)
            val tvProduct = cardView.findViewById<TextView>(R.id.tvProduct)
            val tvConsumed = cardView.findViewById<TextView>(R.id.tvConsumed)
            val tvProduced = cardView.findViewById<TextView>(R.id.tvProduced)

            tvSlotName.text = slot.slotName ?: "Bilinmiyor"
            
            val isRunning = slot.isRunning == true

            if (isRunning) {
                // ÇALIŞIYOR (Yeşil Kart)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.reup_success))
                val whiteColor = ContextCompat.getColor(this, R.color.reup_white)
                
                tvStatusIcon.text = "✓"
                tvStatusIcon.setTextColor(whiteColor)
                tvStatus.text = "Çalışıyor"
                tvStatus.setTextColor(whiteColor)
                
                layoutDetails.visibility = View.VISIBLE
                tvIdleMessage.visibility = View.GONE
                
                tvOrderNumber.text = slot.orderNumber ?: "-"
                tvLot.text = slot.lot ?: "-"
                tvProduct.text = slot.product ?: "-"
                tvConsumed.text = slot.consumed ?: "0"
                tvProduced.text = slot.produced ?: "0"
                
                // Details kısmındaki yazı renklerini beyaza çeviriyoruz xml tarafında ayarlanmamışsa
                setAllTextViewColorsToWhite(layoutDetails)

                btnAction.text = "AKTİF"
                btnAction.setTextColor(whiteColor)
                btnAction.strokeColor = ColorStateList.valueOf(whiteColor)

                // Çalışan karta basılınca BarkodListesiActivity'e git
                cardView.setOnClickListener {
                    val machineId = sessionManager.getMachineId() ?: 0
                    val factory = sessionManager.getMachineFactory()
                    if (factory == FACTORY_87) {
                        showAgirlikSecimDialog(slot, machineId)
                    } else {
                        navigateToBarkodListesi(slot, machineId, 0)
                    }
                }

            } else {
                // HAZIR (Gri/Boş Kart)
                cardView.setCardBackgroundColor(ContextCompat.getColor(this, R.color.reup_surface_alt))
                val grayColor = ContextCompat.getColor(this, R.color.textColorSecondary)
                
                tvStatusIcon.text = "■" // Placeholder icon (Kare)
                tvStatusIcon.setTextColor(grayColor)
                tvStatus.text = "Kazan Boş"
                tvStatus.setTextColor(grayColor)
                
                layoutDetails.visibility = View.GONE
                tvIdleMessage.visibility = View.VISIBLE
                
                btnAction.text = "HAZIR"
                btnAction.setTextColor(grayColor)
                btnAction.strokeColor = ColorStateList.valueOf(grayColor)

                // Çalışmayan karta basılınca uyarı
                cardView.setOnClickListener {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Uyarı")
                        .setMessage("İş emri setli değil.")
                        .setPositiveButton("Tamam", null)
                        .show()
                }
            }

            binding.layoutSlots.addView(cardView)
        }
    }

    private fun setAllTextViewColorsToWhite(view: View) {
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                setAllTextViewColorsToWhite(view.getChildAt(i))
            }
        } else if (view is TextView) {
            view.setTextColor(ContextCompat.getColor(this, R.color.reup_white))
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_makine_secimi -> {
                fetchAndShowMachineSelectionDialog()
            }
            R.id.nav_depo_transfer -> {
                startActivity(Intent(this, DepoTransferActivity::class.java))
            }
            R.id.nav_guncelleme -> {
                showUpdateDialog()
            }
            R.id.nav_guvenli_cikis -> {
                showLogoutDialog()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun fetchAndShowMachineSelectionDialog() {
        // Makine yükleniyor dialogu
        val progressDialog = AlertDialog.Builder(this)
            .setMessage("Makineler yükleniyor...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAllMachines()
                progressDialog.dismiss()

                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val machines: List<Machine> = response.body()!!
                    displayMachineDialog(machines)
                } else {
                    Toast.makeText(this@MainActivity, "Makine listesi alınamadı", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayMachineDialog(machines: List<Machine>) {
        val spinner = Spinner(this)
        val machineNames = machines.map { it.machineName ?: "Bilinmiyor" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, machineNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val padding = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 4
        spinner.setPadding(padding, padding, padding, padding)

        // Eğer mevcut seçili makine varsa onu seçili getir
        val currentMachineId = sessionManager.getMachineId()
        val currentIndex = machines.indexOfFirst { it.id == currentMachineId }
        if (currentIndex != -1) {
            spinner.setSelection(currentIndex)
        }

        AlertDialog.Builder(this)
            .setTitle("Makine Değiştir")
            .setMessage("Çalışacağınız makineyi seçin:")
            .setView(spinner)
            .setPositiveButton("Kaydet") { _, _ ->
                val selectedIndex = spinner.selectedItemPosition
                if (selectedIndex >= 0 && selectedIndex < machines.size) {
                    val selected = machines[selectedIndex]
                    sessionManager.saveMachine(selected.id ?: 0, selected.machineName ?: "", selected.factory)
                    
                    // UI ve verileri yenile
                    setupDashboardInfo()
                    loadMachineSlots()
                    Toast.makeText(this, "Makine değiştirildi: ${selected.machineName}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showUpdateDialog() {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Sunucu kontrol ediliyor...")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val result = AppUpdater.checkForUpdate(this@MainActivity)
            loadingDialog.dismiss()

            if (result == null) {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Güncelleme")
                    .setMessage("Sunucuya ulaşılamadı.")
                    .setPositiveButton("Tamam", null)
                    .show()
                return@launch
            }

            val message = buildString {
                append("Kurulu sürüm tarihi:\n${result.installDate}\n\n")
                append("Sunucudaki sürüm tarihi:\n${result.serverDate}\n\n")
                if (result.hasUpdate) append("Yeni sürüm mevcut! Güncellemek ister misiniz?")
                else append("Uygulama güncel.")
            }

            val builder = AlertDialog.Builder(this@MainActivity)
                .setTitle("Güncelleme")
                .setMessage(message)
                .setNegativeButton("Kapat", null)

            if (result.hasUpdate) {
                builder.setPositiveButton("İndir & Yükle") { _, _ ->
                    startUpdate(result.serverLastModified)
                }
            }

            builder.show()
        }
    }

    private fun startUpdate(serverLastModified: Long = 0L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !AppUpdater.canInstallPackages(this)) {
            AlertDialog.Builder(this)
                .setTitle("İzin Gerekli")
                .setMessage("Uygulamayı yüklemek için 'Bilinmeyen kaynaklardan yükleme' iznini açın.")
                .setPositiveButton("Ayarlara Git") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                .setNegativeButton("İptal", null)
                .show()
            return
        }

        val progressDialog = AlertDialog.Builder(this)
            .setTitle("Güncelleme İndiriliyor")
            .setMessage("Lütfen bekleyin... %0")
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            var downloadedKb = 0
            val success = AppUpdater.downloadApk(this@MainActivity) { progress ->
                if (progress >= 0) {
                    progressDialog.setMessage("Lütfen bekleyin... %$progress")
                } else {
                    downloadedKb += 8
                    progressDialog.setMessage("İndiriliyor... $downloadedKb KB")
                }
            }
            progressDialog.dismiss()

            if (success) {
                AppUpdater.installApk(this@MainActivity)
            } else {
                Toast.makeText(this@MainActivity, "İndirme başarısız oldu.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToBarkodListesi(slot: MachineSlotStatus, machineId: Int, agirlikLimiti: Int) {
        val intent = Intent(this@MainActivity, BarkodListesiActivity::class.java).apply {
            putExtra("SLOT_DATA", slot)
            putExtra("IS_EMRI", slot.orderNumber)
            putExtra("MACHINE_ID", machineId)
            if (agirlikLimiti > 0) {
                putExtra("AGIRLIK_LIMITI", agirlikLimiti)
            }
        }
        startActivity(intent)
    }

    private fun showAgirlikSecimDialog(slot: MachineSlotStatus, machineId: Int) {
        val spinner = Spinner(this)
        val secenekler = listOf("3400 kg", "3200 kg", "2100 kg", "3600 kg")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, secenekler)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.setSelection(0)

        val padding = (16 * resources.displayMetrics.density).toInt()
        spinner.setPadding(padding, padding, padding, padding)

        AlertDialog.Builder(this)
            .setTitle("Kazan Ağırlık Limiti")
            .setMessage("Toplam ağırlık limitini seçin:")
            .setView(spinner)
            .setCancelable(false)
            .setPositiveButton("Devam") { _, _ ->
                val agirlikLimiti = when (spinner.selectedItemPosition) {
                    1 -> 3200
                    2 -> 2100
                    3 -> 3600
                    else -> 3400
                }
                navigateToBarkodListesi(slot, machineId, agirlikLimiti)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Güvenli Çıkış")
            .setMessage("Çıkış yapmak istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                try {
                    performLogout()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun performLogout() {
        sessionManager.logout()
        navigateToLoginAndClearTask()
    }

    private fun navigateToLoginAndClearTask() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
