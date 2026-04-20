package com.example.barkodapp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "BarkodAppSession"
        private const val KEY_SICIL_NO = "sicil_no"
        private const val KEY_PERS_NAME = "pers_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_MACHINE_ID = "machine_id"
        private const val KEY_MACHINE_NAME = "machine_name"
        private const val KEY_MACHINE_FACTORY = "machine_factory"
        private const val KEY_YETKI_BARKOD = "yetki_barkod"
        private const val KEY_YETKI_DEPO = "yetki_depo"
    }

    fun saveSicilNo(sicilNo: String) {
        prefs.edit()
            .putString(KEY_SICIL_NO, sicilNo)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun savePersName(name: String) {
        prefs.edit().putString(KEY_PERS_NAME, name).apply()
    }

    fun getPersName(): String? = prefs.getString(KEY_PERS_NAME, null)

    fun getSicilNo(): String? = prefs.getString(KEY_SICIL_NO, null)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun saveMachine(id: Int, name: String, factory: String? = null) {
        prefs.edit()
            .putInt(KEY_MACHINE_ID, id)
            .putString(KEY_MACHINE_NAME, name)
            .putString(KEY_MACHINE_FACTORY, factory)
            .apply()
    }

    fun getMachineId(): Int? {
        val id = prefs.getInt(KEY_MACHINE_ID, -1)
        return if (id == -1) null else id
    }

    fun getMachineName(): String? = prefs.getString(KEY_MACHINE_NAME, null)

    fun getMachineFactory(): String? = prefs.getString(KEY_MACHINE_FACTORY, null)

    fun saveYetkiler(barkod: Boolean, depo: Boolean) {
        prefs.edit()
            .putBoolean(KEY_YETKI_BARKOD, barkod)
            .putBoolean(KEY_YETKI_DEPO, depo)
            .apply()
    }

    fun hasYetkiBarkodOkutma(): Boolean = prefs.getBoolean(KEY_YETKI_BARKOD, false)
    fun hasYetkiDepoTransfer(): Boolean = prefs.getBoolean(KEY_YETKI_DEPO, false)

    fun logout() {
        // Tüm verileri temizle
        prefs.edit().clear().commit()
    }
}

