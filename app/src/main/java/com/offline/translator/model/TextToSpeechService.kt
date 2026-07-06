package com.offline.translator.model

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.*

class TextToSpeechService(context: Context) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private var onComplete: (() -> Unit)? = null
    private var selectedVoice: Voice? = null
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            
            // Try to find a high-quality voice (not from the embedded TTS engine)
            selectBestVoiceForLocale(Locale.US)
            
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onComplete?.invoke()
                }
                override fun onError(utteranceId: String?) {}
            })
        }
    }
    
    /**
     * Select the best available voice for the given locale, preferring network/enhanced voices
     */
    private fun selectBestVoiceForLocale(locale: Locale) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = tts.getVoices()
            val localeVoices = voices.filter { voice ->
                voice.locale.language == locale.language &&
                (voice.locale.country == locale.country || voice.locale.country.isEmpty())
            }
            
            // Prefer enhanced/network voices for more natural speech
            selectedVoice = localeVoices
                .filter { !it.name.contains(" Built-In", ignoreCase = true) }
                .maxByOrNull { voice ->
                    // Score based on quality indicators
                    var score = 0
                    if (voice.name.contains("enhanced", ignoreCase = true)) score += 10
                    if (voice.name.contains("premium", ignoreCase = true)) score += 10
                    if (voice.name.contains("network", ignoreCase = true)) score += 8
                    if (voice.name.contains("high", ignoreCase = true)) score += 5
                    if (!voice.name.contains("zombie", ignoreCase = true) && 
                        !voice.name.contains("robot", ignoreCase = true)) score += 3
                    score
                } ?: localeVoices.firstOrNull()
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
        
        // Try to use selected voice on Android 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && selectedVoice != null) {
            try {
                tts.setVoice(selectedVoice!!)
            } catch (e: Exception) {
                // Voice selection failed, continue with default
            }
        }
        
        // Set speech rate slightly slower for clearer pronunciation
        tts.setSpeechRate(0.95f)
        tts.setPitch(1.0f)
        
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.getVoices().forEach { voice ->
                languages.add(voice.locale.language)
            }
        } else {
            tts.availableLanguages.forEach { locale ->
                languages.add(locale.language)
            }
        }
        return languages.distinct()
    }
    
    /**
     * Get list of available voices for UI selection
     */
    fun getAvailableVoices(languageCode: String): List<VoiceInfo> {
        if (!isInitialized || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return emptyList()
        
        val locale = when (languageCode) {
            "en" -> Locale.US
            "pt" -> Locale("pt", "BR")
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            else -> Locale.US
        }
        
        return tts.getVoices()
            .filter { it.locale.language == locale.language }
            .map { voice ->
                val quality = when {
                    voice.name.contains("enhanced", ignoreCase = true) -> "Enhanced"
                    voice.name.contains("premium", ignoreCase = true) -> "Premium"
                    voice.name.contains("network", ignoreCase = true) -> "Network"
                    voice.name.contains("high", ignoreCase = true) -> "High Quality"
                    else -> "Standard"
                }
                VoiceInfo(voice.name, quality, voice)
            }
            .sortedByDescending { it.quality != "Standard" }
    }
    
    /**
     * Select a specific voice by name
     */
    fun selectVoice(voiceName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            selectedVoice = tts.getVoices().find { it.name == voiceName }
        }
    }
    
    data class VoiceInfo(val name: String, val quality: String, val voice: Voice)
}