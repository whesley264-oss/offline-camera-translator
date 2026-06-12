package com.offline.translator.model

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TextRecognitionService {
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun recognizeText(bitmap: Bitmap): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val recognizedText = visionText.text.trim()
                    continuation.resume(Result.success(recognizedText))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }
    
    fun close() {
        recognizer.close()
    }
}