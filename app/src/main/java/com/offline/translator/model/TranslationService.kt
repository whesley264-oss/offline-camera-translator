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
import kotlin.coroutines.resumeWithException

class TranslationService(private val context: Context) {
    
    private val translators = mutableMapOf<Pair<String, String>, Translator>()
    private val modelManager = RemoteModelManager.getInstance()
    
    suspend fun downloadLanguage(sourceLang: String, targetLang: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()
            
            getOrCreateTranslator(sourceLang, targetLang).downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
            
            continuation.invokeOnCancellation {
                // Model download is cancelled
            }
        }
    }
    
    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> {
        if (text.isBlank()) return Result.success("")
        
        return suspendCancellableCoroutine { continuation ->
            val translator = getOrCreateTranslator(sourceLang, targetLang)
            
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    continuation.resume(Result.success(translatedText))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
            
            continuation.invokeOnCancellation {
                // Translation is cancelled
            }
        }
    }
    
    suspend fun getDownloadedModels(): Set<String> {
        return suspendCancellableCoroutine { continuation ->
            modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
                .addOnSuccessListener { models ->
                    val codes = models.map { it.language }.toSet()
                    continuation.resume(codes)
                }
                .addOnFailureListener {
                    continuation.resume(emptySet())
                }
        }
    }
    
    suspend fun deleteLanguage(langCode: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val model = TranslateRemoteModel.newBuilder(langCode).build()
            modelManager.deleteDownloadedModel(model)
                .addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }
    
    fun isModelDownloaded(langCode: String, callback: (Boolean) -> Unit) {
        val model = TranslateRemoteModel.newBuilder(langCode).build()
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { isDownloaded -> callback(isDownloaded) }
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