package com.offline.translator.view

interface TranslationView {
    fun showTranslation(translatedText: String)
    fun showError(message: String)
    fun getSelectedDictionaryPath(): String
    fun updateDictionaryList(files: List<String>)
}