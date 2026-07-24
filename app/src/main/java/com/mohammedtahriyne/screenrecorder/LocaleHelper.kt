package com.mohammedtahriyne.screenrecorder

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {

    fun applyLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = if (languageCode == ConfigManager.LANG_SYSTEM || languageCode.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageCode)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getLanguageDisplayName(languageCode: String, englishName: String, nativeName: String): String {
        return "$englishName ($nativeName)"
    }

    val supportedLanguages: List<Pair<String, String>> by lazy {
        listOf(
            ConfigManager.LANG_SYSTEM to "System Default",
            ConfigManager.LANG_ENGLISH to "English",
            ConfigManager.LANG_ARABIC to "Arabic",
            ConfigManager.LANG_FRENCH to "French",
            ConfigManager.LANG_SPANISH to "Spanish"
        )
    }
}
