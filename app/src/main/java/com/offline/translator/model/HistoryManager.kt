package com.offline.translator.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class HistoryManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun saveTranslation(
        originalText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String,
        type: String // "text" or "image"
    ) {
        val timestamp = System.currentTimeMillis()
        val id = "t_${timestamp}"
        
        val entry = TranslationHistoryEntry(
            id = id,
            originalText = originalText,
            translatedText = translatedText,
            sourceLang = sourceLang,
            targetLang = targetLang,
            type = type,
            timestamp = timestamp
        )
        
        // Save to history
        val history = getHistory().toMutableList()
        history.add(0, entry) // Add to beginning
        
        // Keep only last 100 entries
        val trimmed = history.take(MAX_HISTORY_SIZE)
        saveHistory(trimmed)
    }
    
    fun getHistory(): List<TranslationHistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            HistorySerializer.deserialize(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun clearHistory() {
        prefs.edit { remove(KEY_HISTORY) }
    }
    
    fun deleteEntry(id: String) {
        val history = getHistory().toMutableList()
        history.removeAll { it.id == id }
        saveHistory(history)
    }
    
    fun searchHistory(query: String): List<TranslationHistoryEntry> {
        return getHistory().filter {
            it.originalText.contains(query, ignoreCase = true) ||
            it.translatedText.contains(query, ignoreCase = true)
        }
    }
    
    private fun saveHistory(history: List<TranslationHistoryEntry>) {
        val json = HistorySerializer.serialize(history)
        prefs.edit { putString(KEY_HISTORY, json) }
    }
    
    companion object {
        private const val PREFS_NAME = "translation_history"
        private const val KEY_HISTORY = "history"
        private const val MAX_HISTORY_SIZE = 100
    }
}

data class TranslationHistoryEntry(
    val id: String,
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val type: String,
    val timestamp: Long
)

private object HistorySerializer {
    fun serialize(list: List<TranslationHistoryEntry>): String {
        return list.joinToString(SEPARATOR) { entry ->
            listOf(
                entry.id,
                entry.originalText.replace(SEPARATOR, " "),
                entry.translatedText.replace(SEPARATOR, " "),
                entry.sourceLang,
                entry.targetLang,
                entry.type,
                entry.timestamp.toString()
            ).joinToString(FIELD_SEPARATOR)
        }
    }
    
    fun deserialize(data: String): List<TranslationHistoryEntry> {
        if (data.isBlank()) return emptyList()
        
        return data.split(SEPARATOR).mapNotNull { entryStr ->
            val fields = entryStr.split(FIELD_SEPARATOR)
            if (fields.size >= 7) {
                TranslationHistoryEntry(
                    id = fields[0],
                    originalText = fields[1],
                    translatedText = fields[2],
                    sourceLang = fields[3],
                    targetLang = fields[4],
                    type = fields[5],
                    timestamp = fields[6].toLongOrNull() ?: 0L
                )
            } else null
        }
    }
    
    private const val SEPARATOR = "||"
    private const val FIELD_SEPARATOR = "|"
}