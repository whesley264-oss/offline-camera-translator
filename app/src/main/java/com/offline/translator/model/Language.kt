package com.offline.translator.model

import com.google.mlkit.nl.translate.TranslateLanguage

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    var isDownloaded: Boolean = false,
    var isDownloading: Boolean = false
) {
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            Language(TranslateLanguage.ENGLISH, "English", "English"),
            Language(TranslateLanguage.PORTUGUESE, "Portuguese", "Português"),
            Language(TranslateLanguage.SPANISH, "Spanish", "Español"),
            Language(TranslateLanguage.FRENCH, "French", "Français"),
            Language(TranslateLanguage.GERMAN, "German", "Deutsch"),
            Language(TranslateLanguage.ITALIAN, "Italian", "Italiano"),
            Language(TranslateLanguage.CHINESE, "Chinese", "中文"),
            Language(TranslateLanguage.JAPANESE, "Japanese", "日本語"),
            Language(TranslateLanguage.KOREAN, "Korean", "한국어"),
            Language(TranslateLanguage.RUSSIAN, "Russian", "Русский"),
            Language(TranslateLanguage.ARABIC, "Arabic", "العربية"),
            Language(TranslateLanguage.HINDI, "Hindi", "हिन्दी")
        )
        
        val DEFAULT_SOURCE = "en"
        val DEFAULT_TARGET = "pt"
        
        fun fromCode(code: String): Language? = SUPPORTED_LANGUAGES.find { it.code == code }
    }
}