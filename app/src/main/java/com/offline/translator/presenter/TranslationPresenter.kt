package com.offline.translator.presenter

import com.offline.translator.model.TranslationModel
import com.offline.translator.view.TranslationView
import java.io.File

class TranslationPresenter(
    private val view: TranslationView,
    private val model: TranslationModel
) {
    fun scanAvailableDictionaries(filesDir: File) {
        val binFiles = filesDir.listFiles { _, name -> name.endsWith(".bin") }
        if (binFiles.isNullOrEmpty()) {
            view.updateDictionaryList(emptyList())
            view.showError("Nenhum dicionário offline encontrado (.bin)")
        } else {
            view.updateDictionaryList(binFiles.map { it.absolutePath })
        }
    }

    fun processCapturedText(rawText: String) {
        val dictPath = view.getSelectedDictionaryPath()
        if (dictPath.isEmpty() || dictPath == "Nenhum selecionado") {
            view.showError("Selecione um dicionário binário válido.")
            return
        }
        if (rawText.isBlank()) return

        val translatedResult = model.translateText(dictPath, rawText)
        view.showTranslation(translatedResult)
    }
}