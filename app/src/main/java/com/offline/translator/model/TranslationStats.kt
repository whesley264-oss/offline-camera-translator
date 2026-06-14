package com.offline.translator.model

data class TranslationStats(
    val totalTranslations: Int = 0,
    val textTranslations: Int = 0,
    val imageTranslations: Int = 0,
    val excellentCount: Int = 0,    // 5 estrelas
    val goodCount: Int = 0,         // 4 estrelas
    val averageCount: Int = 0,      // 3 estrelas
    val poorCount: Int = 0,         // 2 estrelas
    val badCount: Int = 0,          // 1 estrela
    val notRatedCount: Int = 0
) {
    val averageRating: Float
        get() = if (totalTranslations > 0) {
            ((excellentCount * 5) + (goodCount * 4) + (averageCount * 3) + 
             (poorCount * 2) + (badCount * 1)) / 
            (excellentCount + goodCount + averageCount + poorCount + badCount).toFloat()
        } else 0f

    val successRate: Float
        get() = if (totalTranslations > 0) {
            ((excellentCount + goodCount).toFloat() / totalTranslations) * 100
        } else 0f

    val ratedCount: Int
        get() = excellentCount + goodCount + averageCount + poorCount + badCount
}

enum class TranslationRating(val stars: Int) {
    EXCELLENT(5),
    GOOD(4),
    AVERAGE(3),
    POOR(2),
    BAD(1)
}

data class TranslationRecord(
    val id: Long = System.currentTimeMillis(),
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val type: TranslationType,
    val rating: TranslationRating? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TranslationType {
    TEXT,
    IMAGE
}
