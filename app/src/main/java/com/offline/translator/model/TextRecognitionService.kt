package com.offline.translator.model

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TextRecognitionService {
    
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun recognizeText(bitmap: Bitmap): Result<String> {
        // First attempt with enhanced image
        val enhanced = enhanceForOCR(bitmap)
        val result = recognizeInternal(enhanced)
        
        // If failed or empty, try with original
        if (result.getOrNull()?.isBlank() == true) {
            return recognizeInternal(bitmap)
        }
        
        return result
    }
    
    private suspend fun recognizeInternal(bitmap: Bitmap): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = cleanRecognizedText(visionText.text)
                    continuation.resume(Result.success(text))
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }
    }
    
    /**
     * Enhance image for better OCR results
     */
    private fun enhanceForOCR(bitmap: Bitmap): Bitmap {
        // Scale up for better recognition
        val scaleFactor = 2f
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scaleFactor).toInt(),
            (bitmap.height * scaleFactor).toInt(),
            true
        )
        
        // Apply contrast enhancement
        val enhanced = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint()
        
        // Increase contrast and sharpness
        val colorMatrix = ColorMatrix().apply {
            setScale(1.5f, 1.5f, 1.5f, 1f) // Increase contrast
        }
        
        // Add saturation reduction (grayscale-ish but keeps some color info)
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(1.2f)
        colorMatrix.postConcat(satMatrix)
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        
        if (scaled != bitmap) scaled.recycle()
        return enhanced
    }
    
    /**
     * Clean and normalize recognized text
     */
    private fun cleanRecognizedText(text: String): String {
        return text
            .trim()
            .replaceAll("\\s+".toRegex(), " ") // Multiple spaces to single
            .replaceAll("_[a-zA-Z]".toRegex()) { match -> 
                match.value.first().lowercaseChar().toString() 
            } // Fix underscore artifacts
            .replaceAll("[^\\p{L}\\p{N}\\s.,!?;:'\"-]".toRegex(), "") // Remove weird chars
            .replaceAll("\\n{3,}".toRegex(), "\n\n") // Too many newlines
            .trim()
    }
    
    suspend fun recognizeTextWithBoxes(bitmap: Bitmap): Result<Pair<String, List<RectF>>> {
        val enhanced = enhanceForOCR(bitmap)
        return recognizeWithBoxesInternal(enhanced).also { 
            if (enhanced != bitmap) enhanced.recycle()
        }
    }
    
    private suspend fun recognizeWithBoxesInternal(bitmap: Bitmap): Result<Pair<String, List<RectF>>> {
        return suspendCancellableCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = cleanRecognizedText(visionText.text)
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