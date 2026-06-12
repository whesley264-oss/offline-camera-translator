package com.offline.translator.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.text.Text

class CameraPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PreviewView(context, attrs, defStyleAttr) {
    
    private var textBlocks: List<Text.TextBlock> = emptyList()
    
    // Image enhancement settings
    var brightness: Float = 1.0f  // 0.5 to 2.0
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            invalidate()
        }
    
    var contrast: Float = 1.0f  // 0.5 to 2.0
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            invalidate()
        }
    
    var sharpness: Float = 1.0f  // 0.0 to 2.0
        set(value) {
            field = value.coerceIn(0f, 2f)
            invalidate()
        }
    
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#FFFF00")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#00FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#30FFFF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    fun setTextBlocks(blocks: List<Text.TextBlock>) {
        textBlocks = blocks
        invalidate()
    }
    
    fun clearTextBlocks() {
        textBlocks = emptyList()
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Apply brightness/contrast to canvas
        if (brightness != 1.0f || contrast != 1.0f) {
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        }
        
        // Draw text bounding boxes
        for (block in textBlocks) {
            val rect = block.boundingBox ?: continue
            
            // Scale to view coordinates
            val scaleX = width.toFloat() / bitmapWidth
            val scaleY = height.toFloat() / bitmapHeight
            
            val scaledRect = RectF(
                rect.left * scaleX,
                rect.top * scaleY,
                rect.right * scaleX,
                rect.bottom * scaleY
            )
            
            // Draw fill
            canvas.drawRect(scaledRect, fillPaint)
            
            // Draw border
            canvas.drawRect(scaledRect, overlayPaint)
            
            // Draw corners
            drawCorners(canvas, scaledRect)
        }
        
        if (brightness != 1.0f || contrast != 1.0f) {
            canvas.restore()
        }
    }
    
    private fun drawCorners(canvas: Canvas, rect: RectF) {
        val cornerLength = 20f
        
        // Top-left
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, overlayPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLength, overlayPaint)
        
        // Top-right
        canvas.drawLine(rect.right, rect.top, rect.right - cornerLength, rect.top, overlayPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, overlayPaint)
        
        // Bottom-left
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, overlayPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLength, overlayPaint)
        
        // Bottom-right
        canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLength, rect.bottom, overlayPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, overlayPaint)
    }
    
    private var bitmapWidth = 1
    private var bitmapHeight = 1
    
    fun setBitmapSize(width: Int, height: Int) {
        bitmapWidth = width
        bitmapHeight = height
    }
}