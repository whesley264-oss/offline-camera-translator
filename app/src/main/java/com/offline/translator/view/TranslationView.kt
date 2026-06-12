package com.offline.translator.view

import com.offline.translator.model.Language

interface TranslationView {
    fun showTranslation(translatedText: String)
    fun showError(message: String)
    fun getSelectedSourceLanguage(): String
    fun getSelectedTargetLanguage(): String
    fun updateLanguageLists(sourceLanguages: List<Language>, targetLanguages: List<Language>)
    fun updateDownloadProgress(language: Language, isDownloading: Boolean)
    fun showDownloadSuccess(language: Language)
    fun showDownloadError(language: Language, error: String)
    fun showLoading(isLoading: Boolean)
}