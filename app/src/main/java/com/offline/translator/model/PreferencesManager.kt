package com.offline.translator.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)
    fun setDarkMode(enabled: Boolean) = prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }

    fun getFontSize(): Int = prefs.getInt(KEY_FONT_SIZE, 1)
    fun setFontSize(size: Int) = prefs.edit { putInt(KEY_FONT_SIZE, size) }

    fun isAutoDetect(): Boolean = prefs.getBoolean(KEY_AUTO_DETECT, true)
    fun setAutoDetect(enabled: Boolean) = prefs.edit { putBoolean(KEY_AUTO_DETECT, enabled) }

    fun isHapticEnabled(): Boolean = prefs.getBoolean(KEY_HAPTIC, true)
    fun setHaptic(enabled: Boolean) = prefs.edit { putBoolean(KEY_HAPTIC, enabled) }

    fun isAnimationsEnabled(): Boolean = prefs.getBoolean(KEY_ANIMATIONS, true)
    fun setAnimations(enabled: Boolean) = prefs.edit { putBoolean(KEY_ANIMATIONS, enabled) }

    fun getSourceLanguage(): String = prefs.getString(KEY_SOURCE_LANG, "en") ?: "en"
    fun setSourceLanguage(code: String) = prefs.edit { putString(KEY_SOURCE_LANG, code) }

    fun getTargetLanguage(): String = prefs.getString(KEY_TARGET_LANG, "pt") ?: "pt"
    fun setTargetLanguage(code: String) = prefs.edit { putString(KEY_TARGET_LANG, code) }

    fun isFirstLaunch(): Boolean = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    fun setFirstLaunchDone() = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, false) }

    companion object {
        private const val PREFS_NAME = "translator_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_AUTO_DETECT = "auto_detect"
        private const val KEY_HAPTIC = "haptic"
        private const val KEY_ANIMATIONS = "animations"
        private const val KEY_SOURCE_LANG = "source_lang"
        private const val KEY_TARGET_LANG = "target_lang"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
}