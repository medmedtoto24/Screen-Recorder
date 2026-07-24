package com.mohammedtahriyne.screenrecorder

import android.content.Context
import android.content.SharedPreferences

class ProfileManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NAME = "profile_name"
        private const val KEY_EMAIL = "profile_email"
        private const val KEY_BIO = "profile_bio"
    }

    var profileName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var profileEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    var profileBio: String
        get() = prefs.getString(KEY_BIO, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BIO, value).apply()

    fun isRegistered(): Boolean = profileName.isNotEmpty()

    fun clear() { prefs.edit().clear().apply() }
}
