package com.offline.translator.model

class NativeTranslationEngine {
    companion object {
        init {
            System.loadLibrary("translation_engine")
        }
    }
    external fun translateWord(dictPath: String, word: String): String
}