package com.offline.translator.presenter

import android.graphics.Bitmap
import com.offline.translator.model.Language
import com.offline.translator.model.TranslationService
import com.offline.translator.model.TextRecognitionService
import com.offline.translator.view.TranslationView
import kotlinx.coroutines.*

class TranslationPresenter(
    private val view: TranslationView,
    private val translationService: TranslationService,
    private val textRecognitionService: TextRecognitionService
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var downloadedLanguages = mutableSetOf<String>()
    
    init {
        scope.launch {
            loadDownloadedLanguages()
        }
    }
    
    private suspend fun loadDownloadedLanguages() {
        // Get downloaded models from ML Kit
        val models = translationService.getDownloadedModels()
        downloadedLanguages.clear()
        downloadedLanguages.addAll(models)
        
        // Bundled languages (en, pt) are always available
        downloadedLanguages.addAll(translationService.bundledLanguages)
        
        updateLanguageLists()
    }
    
    fun updateLanguageLists() {
        val sourceLangs = Language.SUPPORTED_LANGUAGES.map { lang ->
            lang.copy(isDownloaded = downloadedLanguages.contains(lang.code))
        }
        val targetLangs = sourceLangs.filter { it.code != view.getSelectedSourceLanguage() }
        
        view.updateLanguageLists(sourceLangs, targetLangs)
    }
    
    fun downloadLanguage(langCode: String) {
        val language = Language.fromCode(langCode) ?: return
        
        // Cannot download bundled languages
        if (translationService.bundledLanguages.contains(langCode)) {
            view.showError("${language.name} já está disponível no app")
            return
        }
        
        scope.launch {
            language.isDownloading = true
            view.updateDownloadProgress(language, true)
            view.showLoading(true)
            
            val source = view.getSelectedSourceLanguage()
            val target = view.getSelectedTargetLanguage()
            
            val result = translationService.downloadLanguage(source, target)
            
            language.isDownloading = false
            view.updateDownloadProgress(language, false)
            view.showLoading(false)
            
            result.fold(
                onSuccess = {
                    downloadedLanguages.add(langCode)
                    view.showDownloadSuccess(language)
                    updateLanguageLists()
                },
                onFailure = { e ->
                    view.showDownloadError(language, e.message ?: "Download failed")
                }
            )
        }
    }
    
    fun processFrame(bitmap: Bitmap) {
        scope.launch {
            view.showLoading(true)
            
            // 1. Recognize text from image
            val recognitionResult = textRecognitionService.recognizeText(bitmap)
            
            recognitionResult.fold(
                onSuccess = { recognizedText ->
                    if (recognizedText.isNotBlank()) {
                        translateText(recognizedText)
                    } else {
                        view.showLoading(false)
                    }
                },
                onFailure = { e ->
                    view.showError("OCR Error: ${e.message}")
                    view.showLoading(false)
                }
            )
        }
    }
    
    private suspend fun translateText(text: String) {
        val sourceLang = view.getSelectedSourceLanguage()
        val targetLang = view.getSelectedTargetLanguage()
        
        val result = translationService.translate(text, sourceLang, targetLang)
        
        view.showLoading(false)
        
        result.fold(
            onSuccess = { translatedText ->
                view.showTranslation(translatedText)
            },
            onFailure = { e ->
                view.showError("Translation Error: ${e.message}")
            }
        )
    }
    
    fun onSourceLanguageChanged(newSourceLang: String) {
        updateLanguageLists()
    }
    
    fun onTargetLanguageChanged(newTargetLang: String) {
        updateLanguageLists()
    }
    
    fun cleanup() {
        scope.cancel()
        translationService.closeTranslators()
        textRecognitionService.close()
    }
}