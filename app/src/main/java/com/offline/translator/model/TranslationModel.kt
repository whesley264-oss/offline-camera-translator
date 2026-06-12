package com.offline.translator.model

import java.util.Locale

interface TranslationModel {
    fun translateText(dictPath: String, text: String): String
}

class TranslationModelImpl(private val engine: NativeTranslationEngine) : TranslationModel {
    override fun translateText(dictPath: String, text: String): String {
        if (text.isBlank()) return ""
        val tokens = text.split(Regex("(\\s+|(?<=\\b)|(?=\\b))"))
        val resultBuilder = StringBuilder()

        for (token in tokens) {
            val cleanWord = token.replace(Regex("[^a-zA-Z]"), "").lowercase(Locale.ROOT)
            if (cleanWord.isNotEmpty()) {
                val translation = engine.translateWord(dictPath, cleanWord)
                when {
                    token.all { it.isUpperCase() } -> resultBuilder.append(translation.uppercase(Locale.ROOT))
                    token.isNotBlank() && token.first().isUpperCase() -> resultBuilder.append(translation.replaceFirstChar { it.uppercase(Locale.ROOT) })
                    else -> resultBuilder.append(translation)
                }
            } else {
                resultBuilder.append(token)
            }
        }
        return resultBuilder.toString()
    }
}