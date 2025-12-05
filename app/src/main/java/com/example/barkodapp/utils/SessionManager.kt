package com.example.barkodapp.utils

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "BarkodAppSession"
        private const val KEY_SICIL_NO = "sicil_no"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    // Sicil numarasını kaydet ve giriş durumunu true yap
    fun saveSicilNo(sicilNo: String) {
        prefs.edit().apply {
            putString(KEY_SICIL_NO, sicilNo)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    // Kayıtlı sicil numarasını al
    fun getSicilNo(): String? {
        return prefs.getString(KEY_SICIL_NO, null)
    }

    // Kullanıcının giriş yapıp yapmadığını kontrol et
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // Çıkış yap - Session'ı temizle
    fun logout() {
        prefs.edit().apply {
            clear()
            apply()
        }
    }
}
