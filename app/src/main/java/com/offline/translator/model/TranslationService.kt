package com.offline.translator.model

import android.content.Context
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
    
    // Base languages (en, pt) - will be auto-downloaded on first use
    val baseLanguages = setOf(Language.DEFAULT_SOURCE, Language.DEFAULT_TARGET)
    
    suspend fun downloadLanguage(sourceLang: String, targetLang: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val conditions = DownloadConditions.Builder().build()
            
            getOrCreateTranslator(sourceLang, targetLang).download(conditions)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }
    
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        if (text.isBlank()) return Result.success("")
        
        return suspendCancellableCoroutine { continuation ->
            val translator = getOrCreateTranslator(sourceLang, targetLang)
            
            // First ensure model is downloaded
            val conditions = DownloadConditions.Builder().build()
            translator.download(conditions)
                .addOnSuccessListener {
                    // Then translate
                    translator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            continuation.resume(Result.success(translatedText))
                        }
                        .addOnFailureListener { e ->
                            continuation.resume(Result.failure(e))
                        }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }
    
    suspend fun getDownloadedModels(): Set<String> {
        return suspendCancellableCoroutine { continuation ->
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    continuation.resume(models.map { it.language }.toSet())
                }
                .addOnFailureListener {
                    continuation.resume(emptySet())
                }
        }
    }
    
    suspend fun deleteLanguage(langCode: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    val model = models.find { it.language == langCode }
                    if (model != null) {
                        modelManager.deleteDownloadedModel(model)
                            .addOnSuccessListener {
                                continuation.resume(Result.success(Unit))
                            }
                            .addOnFailureListener { e ->
                                continuation.resume(Result.failure(e))
                            }
                    } else {
                        continuation.resume(Result.failure(Exception("Model not found")))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }
    
    fun isModelDownloaded(langCode: String, callback: (Boolean) -> Unit) {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                callback(models.any { it.language == langCode })
            }
            .addOnFailureListener { callback(false) }
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
}
