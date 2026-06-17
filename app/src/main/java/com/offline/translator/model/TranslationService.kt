package com.offline.translator.model

import android.content.Context
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TranslationService(private val context: Context) {
    
    private val translators = mutableMapOf<Pair<String, String>, Translator>()
    private val modelManager = RemoteModelManager.getInstance()
    
    // Base languages (en, pt)
    val baseLanguages = setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)
    
    suspend fun downloadLanguage(sourceLang: String, targetLang: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val conditions = DownloadConditions.Builder().build()
                getOrCreateTranslator(sourceLang, targetLang).downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        Log.d(TAG, "Downloaded language model: $sourceLang -> $targetLang")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { e: Exception ->
                        Log.e(TAG, "Failed to download model: ${e.message}")
                        continuation.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception downloading model: ${e.message}")
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        if (text.isBlank()) return Result.success("")
        
        // Validate languages
        if (sourceLang.isBlank() || targetLang.isBlank()) {
            return Result.failure(Exception("Idioma inválido selecionado"))
        }
        
        if (sourceLang == targetLang) {
            return Result.success(text) // Same language, no translation needed
        }
        
        return suspendCancellableCoroutine { continuation ->
            try {
                val translator = getOrCreateTranslator(sourceLang, targetLang)
                val conditions = DownloadConditions.Builder().build()
                
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener {
                        translator.translate(text)
                            .addOnSuccessListener { translatedText ->
                                continuation.resume(Result.success(translatedText))
                            }
                            .addOnFailureListener { e: Exception ->
                                Log.e(TAG, "Translation failed: ${e.message}")
                                continuation.resume(Result.failure(Exception("Tradução falhou: ${e.message}")))
                            }
                    }
                    .addOnFailureListener { e: Exception ->
                        Log.e(TAG, "Model download failed: ${e.message}")
                        continuation.resume(Result.failure(Exception("Modelo não baixado: baixe o idioma na Biblioteca")))
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Translation exception: ${e.message}")
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    suspend fun getDownloadedModels(): Set<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                    .addOnSuccessListener { models ->
                        val languages = models.map { it.language }.toSet()
                        Log.d(TAG, "ML Kit downloaded models: $languages")
                        continuation.resume(languages)
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Failed to get downloaded models")
                        continuation.resume(emptySet())
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting models: ${e.message}")
                continuation.resume(emptySet())
            }
        }
    }
    
    suspend fun deleteLanguage(langCode: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                    .addOnSuccessListener { models ->
                        val model = models.find { it.language == langCode }
                        if (model != null) {
                            modelManager.deleteDownloadedModel(model)
                                .addOnSuccessListener {
                                    continuation.resume(Result.success(Unit))
                                }
                                .addOnFailureListener { e: Exception ->
                                    continuation.resume(Result.failure(e))
                                }
                        } else {
                            continuation.resume(Result.failure(Exception("Modelo não encontrado")))
                        }
                    }
                    .addOnFailureListener { e: Exception ->
                        continuation.resume(Result.failure(e))
                    }
            } catch (e: Exception) {
                continuation.resume(Result.failure(e))
            }
        }
    }
    
    fun isModelDownloaded(langCode: String, callback: (Boolean) -> Unit) {
        try {
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    callback(models.any { it.language == langCode })
                }
                .addOnFailureListener { callback(false) }
        } catch (e: Exception) {
            callback(false)
        }
    }
    
    private fun getOrCreateTranslator(sourceLang: String, targetLang: String): Translator {
        val key = Pair(sourceLang, targetLang)
        return translators.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }
    
    fun closeTranslators() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
    
    companion object {
        private const val TAG = "TranslationService"
    }
}
