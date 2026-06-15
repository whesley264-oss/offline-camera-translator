package com.offline.translator.model

import android.content.Context
import android.content.SharedPreferences

class LanguageDownloadManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun saveDownloadedLanguage(langCode: String) {
        val downloaded = getDownloadedLanguagesInternal().toMutableSet()
        downloaded.add(langCode)
        prefs.edit().putStringSet(KEY_DOWNLOADED, downloaded).apply()
    }
    
    fun removeDownloadedLanguage(langCode: String) {
        val downloaded = getDownloadedLanguagesInternal().toMutableSet()
        downloaded.remove(langCode)
        prefs.edit().putStringSet(KEY_DOWNLOADED, downloaded).apply()
    }
    
    fun getDownloadedLanguages(): Set<String> {
        val baseLanguages = setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)
        val saved = prefs.getStringSet(KEY_DOWNLOADED, emptySet()) ?: emptySet()
        return baseLanguages + saved
    }
    
    private fun getDownloadedLanguagesInternal(): Set<String> {
        return prefs.getStringSet(KEY_DOWNLOADED, emptySet()) ?: emptySet()
    }
    
    fun clearAll() {
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "language_downloads"
        private const val KEY_DOWNLOADED = "downloaded_languages"
    }
}
