package com.offline.translator.model

import android.content.Context
import android.content.SharedPreferences

class LanguageDownloadManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    init {
        // Initialize base languages if not already set
        initializeBaseLanguages()
    }
    
    private fun initializeBaseLanguages() {
        val existing = prefs.getStringSet(KEY_DOWNLOADED, null)
        if (existing == null) {
            // First run - initialize with base languages
            val baseLanguages = setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)
            prefs.edit().putStringSet(KEY_DOWNLOADED, baseLanguages).apply()
        }
    }
    
    fun saveDownloadedLanguage(langCode: String) {
        val downloaded = getDownloadedLanguagesInternal().toMutableSet()
        downloaded.add(langCode)
        prefs.edit().putStringSet(KEY_DOWNLOADED, downloaded).apply()
    }
    
    fun removeDownloadedLanguage(langCode: String) {
        val downloaded = getDownloadedLanguagesInternal().toMutableSet()
        // Don't allow removing base languages
        if (langCode != Language.DEFAULT_SOURCE && langCode != Language.DEFAULT_TARGET) {
            downloaded.remove(langCode)
            prefs.edit().putStringSet(KEY_DOWNLOADED, downloaded).apply()
        }
    }
    
    fun getDownloadedLanguages(): Set<String> {
        val saved = getDownloadedLanguagesInternal()
        // Always include base languages
        return saved + setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)
    }
    
    private fun getDownloadedLanguagesInternal(): Set<String> {
        return prefs.getStringSet(KEY_DOWNLOADED, setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)) ?: setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
        // Re-initialize base languages
        initializeBaseLanguages()
    }
    
    companion object {
        private const val PREFS_NAME = "language_downloads"
        private const val KEY_DOWNLOADED = "downloaded_languages"
    }
}
