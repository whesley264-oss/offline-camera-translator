package com.offline.translator.model

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class GitHubStatsSync(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Config - você precisará configurar seu token e repo
    private val owner = "whesley264-oss"
    private val repo = "offline-camera-translator"
    private val statsFilePath = "stats_data.json"
    
    // Token pode ser passado via BuildConfig ou preferences
    private var githubToken: String? = null

    fun setToken(token: String) {
        githubToken = token
    }

    suspend fun syncWithGitHub(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = githubToken ?: return@withContext Result.failure(Exception("Token não configurado"))
            
            // 1. Buscar stats atuais do repositório
            val currentStats = getRemoteStats(token)
            
            // 2. Mesclar com stats locais
            val mergedStats = mergeStats(currentStats)
            
            // 3. Atualizar arquivo no repositório
            updateRemoteStats(token, mergedStats)
            
            // 4. Limpar stats locais após sync
            clearLocalStats()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getRemoteStats(token: String): JSONObject {
        val url = URL("https://api.github.com/repos/$owner/$repo/contents/$statsFilePath")
        val conn = url.openConnection() as HttpsURLConnection
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        
        return if (conn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val response = reader.readText()
            reader.close()
            
            // Decodificar content do base64
            val json = JSONObject(response)
            val content = json.getString("content")
            val decoded = String(android.util.Base64.decode(content.replace("\n", ""), android.util.Base64.DEFAULT))
            JSONObject(decoded)
        } else {
            // Arquivo não existe ainda, criar novo
            JSONObject()
        }.also { conn.disconnect() }
    }

    private fun mergeStats(remote: JSONObject): JSONObject {
        val localRecords = getLocalRecords()
        
        // Stats acumuladas
        val totalTranslations = remote.optInt("totalTranslations", 0) + localRecords.size
        val totalRatings = remote.optInt("totalRatings", 0)
        
        // Distribuição de ratings
        val ratingDist = JSONObject().apply {
            put("5", remote.optInt("rating5", 0) + localRecords.count { it.rating?.stars == 5 })
            put("4", remote.optInt("rating4", 0) + localRecords.count { it.rating?.stars == 4 })
            put("3", remote.optInt("rating3", 0) + localRecords.count { it.rating?.stars == 3 })
            put("2", remote.optInt("rating2", 0) + localRecords.count { it.rating?.stars == 2 })
            put("1", remote.optInt("rating1", 0) + localRecords.count { it.rating?.stars == 1 })
        }
        
        // Contadores por tipo
        val textCount = remote.optInt("textTranslations", 0) + localRecords.count { it.type == TranslationType.TEXT }
        val imageCount = remote.optInt("imageTranslations", 0) + localRecords.count { it.type == TranslationType.IMAGE }
        
        // Calcular taxas
        val ratedCount = ratingDist.optInt("5") + ratingDist.optInt("4") + 
                        ratingDist.optInt("3") + ratingDist.optInt("2") + ratingDist.optInt("1")
        
        val successRate = if (ratedCount > 0) {
            ((ratingDist.optInt("5") + ratingDist.optInt("4")).toFloat() / ratedCount) * 100
        } else 0f
        
        val avgRating = if (ratedCount > 0) {
            ((ratingDist.optInt("5") * 5) + (ratingDist.optInt("4") * 4) + 
             (ratingDist.optInt("3") * 3) + (ratingDist.optInt("2") * 2) + ratingDist.optInt("1")) / ratedCount.toFloat()
        } else 0f

        return JSONObject().apply {
            put("lastUpdated", System.currentTimeMillis())
            put("totalTranslations", totalTranslations)
            put("totalRatings", ratedCount)
            put("textTranslations", textCount)
            put("imageTranslations", imageCount)
            put("ratingDistribution", ratingDist)
            put("successRate", "%.1f".format(successRate))
            put("averageRating", "%.2f".format(avgRating))
            put("excellentCount", ratingDist.optInt("5"))
            put("goodCount", ratingDist.optInt("4"))
            put("averageCount", ratingDist.optInt("3"))
            put("poorCount", ratingDist.optInt("2"))
            put("badCount", ratingDist.optInt("1"))
        }
    }

    private fun updateRemoteStats(token: String, stats: JSONObject) {
        val url = URL("https://api.github.com/repos/$owner/$repo/contents/$statsFilePath")
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "PUT"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        
        // Primeiro buscar o SHA do arquivo existente
        val getConn = URL("https://api.github.com/repos/$owner/$repo/contents/$statsFilePath")
            .openConnection() as HttpsURLConnection
        getConn.setRequestProperty("Authorization", "Bearer $token")
        getConn.setRequestProperty("Accept", "application/vnd.github.v3+json")
        
        var sha: String? = null
        if (getConn.responseCode == 200) {
            val reader = BufferedReader(InputStreamReader(getConn.inputStream))
            val response = reader.readText()
            reader.close()
            sha = JSONObject(response).optString("sha")
        }
        getConn.disconnect()
        
        // Preparar body
        val content = stats.toString(2)
        val body = JSONObject().apply {
            put("message", "Update stats from app - ${System.currentTimeMillis()}")
            put("content", android.util.Base64.encodeToString(content.toByteArray(), android.util.Base64.NO_WRAP))
            sha?.let { put("sha", it) }
        }
        
        conn.outputStream.write(body.toString().toByteArray())
        conn.outputStream.flush()
        conn.outputStream.close()
        
        if (conn.responseCode !in 200..299) {
            throw Exception("GitHub API error: ${conn.responseCode}")
        }
        
        conn.disconnect()
    }

    fun getLocalRecords(): List<TranslationRecord> {
        val json = prefs.getString(KEY_PENDING_SYNC, "[]") ?: "[]"
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

    fun addPendingSync(record: TranslationRecord) {
        val records = getLocalRecords().toMutableList()
        records.add(record)
        savePendingSync(records)
    }

    private fun savePendingSync(records: List<TranslationRecord>) {
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
        prefs.edit().putString(KEY_PENDING_SYNC, array.toString()).apply()
    }

    fun clearLocalStats() {
        prefs.edit().putString(KEY_PENDING_SYNC, "[]").apply()
    }

    fun getPendingCount(): Int = getLocalRecords().size

    companion object {
        private const val PREFS_NAME = "github_sync"
        private const val KEY_PENDING_SYNC = "pending_sync"
    }
}