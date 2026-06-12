package com.offline.translator.model

import android.graphics.Bitmap
import android.graphics.RectF
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
    
    suspend fun recognizeTextWithBoxes(bitmap: Bitmap): Result<Pair<String, List<RectF>>> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text.trim()
                    val boxes = visionText.textBlocks.mapNotNull { block ->
                        block.boundingBox?.let { rect ->
                            RectF(
                                rect.left.toFloat(),
                                rect.top.toFloat(),
                                rect.right.toFloat(),
                                rect.bottom.toFloat()
                            )
                        }
                    }
                    continuation.resume(Result.success(Pair(text, boxes)))
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