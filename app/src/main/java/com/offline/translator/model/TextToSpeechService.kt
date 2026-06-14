package com.offline.translator.model

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TextToSpeechService(context: Context) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private var onComplete: (() -> Unit)? = null
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && 
                          result != TextToSpeech.LANG_NOT_SUPPORTED
            
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onComplete?.invoke()
                }
                override fun onError(utteranceId: String?) {}
            })
        }
    }
    
    fun speak(text: String, languageCode: String = "en", onComplete: (() -> Unit)? = null) {
        if (!isInitialized) return
        
        this.onComplete = onComplete
        
        val locale = when (languageCode) {
            "en" -> Locale.US
            "pt" -> Locale("pt", "BR")
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            else -> Locale.US
        }
        
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default
            tts.setLanguage(Locale.US)
        }
        
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translation_utterance")
    }
    
    fun speakSource(text: String, sourceLang: String) {
        speak(text, sourceLang)
    }
    
    fun speakTranslation(text: String, targetLang: String) {
        speak(text, targetLang)
    }
    
    fun stop() {
        tts.stop()
    }
    
    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
    
    fun isAvailable(): Boolean = isInitialized
    
    fun getAvailableLanguages(): List<String> {
        if (!isInitialized) return emptyList()
        
        val languages = mutableListOf<String>()
        tts.availableLanguages.forEach { locale ->
            languages.add(locale.language)
        }
        return languages.distinct()
    }
}