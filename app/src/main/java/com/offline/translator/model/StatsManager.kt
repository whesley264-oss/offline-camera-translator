package com.offline.translator.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class StatsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTranslation(
        sourceText: String,
        translatedText: String,
        sourceLang: String,
        targetLang: String,
        type: TranslationType
    ): Long {
        val record = TranslationRecord(
            sourceText = sourceText,
            translatedText = translatedText,
            sourceLang = sourceLang,
            targetLang = targetLang,
            type = type
        )
        
        val records = getRecords().toMutableList()
        records.add(0, record) // Add to beginning
        
        // Keep only last 1000 records
        val trimmed = if (records.size > 1000) records.take(1000) else records
        
        saveRecords(trimmed)
        updateStats()
        
        return record.id
    }

    fun rateTranslation(recordId: Long, rating: TranslationRating) {
        val records = getRecords().toMutableList()
        val index = records.indexOfFirst { it.id == recordId }
        if (index >= 0) {
            records[index] = records[index].copy(rating = rating)
            saveRecords(records)
            updateStats()
        }
    }

    fun getStats(): TranslationStats {
        return TranslationStats(
            totalTranslations = prefs.getInt(KEY_TOTAL, 0),
            textTranslations = prefs.getInt(KEY_TEXT, 0),
            imageTranslations = prefs.getInt(KEY_IMAGE, 0),
            excellentCount = prefs.getInt(KEY_EXCELLENT, 0),
            goodCount = prefs.getInt(KEY_GOOD, 0),
            averageCount = prefs.getInt(KEY_AVERAGE, 0),
            poorCount = prefs.getInt(KEY_POOR, 0),
            badCount = prefs.getInt(KEY_BAD, 0),
            notRatedCount = prefs.getInt(KEY_NOT_RATED, 0)
        )
    }

    fun getRecords(): List<TranslationRecord> {
        val json = prefs.getString(KEY_RECORDS, "[]") ?: "[]"
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                TranslationRecord(
                    id = obj.getLong("id"),
                    sourceText = obj.getString("source"),
                    translatedText = obj.getString("translated"),
                    sourceLang = obj.getString("sourceLang"),
                    targetLang = obj.getString("targetLang"),
                    type = TranslationType.valueOf(obj.getString("type")),
                    rating = if (obj.has("rating") && !obj.isNull("rating")) 
                        TranslationRating.valueOf(obj.getString("rating")) else null,
                    timestamp = obj.getLong("timestamp")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getRecentRecords(limit: Int = 20): List<TranslationRecord> {
        return getRecords().take(limit)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun saveRecords(records: List<TranslationRecord>) {
        val array = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("source", record.sourceText)
                put("translated", record.translatedText)
                put("sourceLang", record.sourceLang)
                put("targetLang", record.targetLang)
                put("type", record.type.name)
                put("rating", record.rating?.name ?: JSONObject.NULL)
                put("timestamp", record.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    private fun updateStats() {
        val records = getRecords()
        val rated = records.filter { it.rating != null }
        
        prefs.edit()
            .putInt(KEY_TOTAL, records.size)
            .putInt(KEY_TEXT, records.count { it.type == TranslationType.TEXT })
            .putInt(KEY_IMAGE, records.count { it.type == TranslationType.IMAGE })
            .putInt(KEY_EXCELLENT, rated.count { it.rating == TranslationRating.EXCELLENT })
            .putInt(KEY_GOOD, rated.count { it.rating == TranslationRating.GOOD })
            .putInt(KEY_AVERAGE, rated.count { it.rating == TranslationRating.AVERAGE })
            .putInt(KEY_POOR, rated.count { it.rating == TranslationRating.POOR })
            .putInt(KEY_BAD, rated.count { it.rating == TranslationRating.BAD })
            .putInt(KEY_NOT_RATED, records.count { it.rating == null })
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "translation_stats"
        private const val KEY_RECORDS = "records"
        private const val KEY_TOTAL = "total"
        private const val KEY_TEXT = "text"
        private const val KEY_IMAGE = "image"
        private const val KEY_EXCELLENT = "excellent"
        private const val KEY_GOOD = "good"
        private const val KEY_AVERAGE = "average"
        private const val KEY_POOR = "poor"
        private const val KEY_BAD = "bad"
        private const val KEY_NOT_RATED = "not_rated"
    }
}