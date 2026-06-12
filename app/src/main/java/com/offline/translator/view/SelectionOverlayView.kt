package com.offline.translator.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.sqrt

class SelectionOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    // Selection box
    private var selectionRect = RectF()
    private var isSelecting = false
    private var isDragging = false
    private var isCornerDragging = false
    private var activeCorner = Corner.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // Detected text boxes from ML Kit
    private val detectedBoxes = mutableListOf<RectF>()
    private val showDetectedBoxes = true
    
    // Paints
    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#00FF00")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#2000FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#00FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val detectedPaint = Paint().apply {
        color = Color.parseColor("#FFFF00")
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    
    enum class Corner {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }
    
    var onSelectionChanged: ((RectF) -> Unit)? = null
    var onTextAreaSelected: ((String) -> Unit)? = null
    
    private val cornerSize = 40f
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw detected text boxes if available
        if (showDetectedBoxes) {
            for (box in detectedBoxes) {
                canvas.drawRect(box, detectedPaint)
            }
        }
        
        // Draw selection box
        if (selectionRect.width() > 0 && selectionRect.height() > 0) {
            // Fill
            canvas.drawRect(selectionRect, fillPaint)
            // Border
            canvas.drawRect(selectionRect, selectionPaint)
            // Corners
            drawCorners(canvas)
        }
    }
    
    private fun drawCorners(canvas: Canvas) {
        val cs = cornerSize
        
        // Top-left
        canvas.drawRect(
            selectionRect.left - cs/2, selectionRect.top - cs/2,
            selectionRect.left + cs/2, selectionRect.top + cs/2,
            cornerPaint
        )
        
        // Top-right
        canvas.drawRect(
            selectionRect.right - cs/2, selectionRect.top - cs/2,
            selectionRect.right + cs/2, selectionRect.top + cs/2,
            cornerPaint
        )
        
        // Bottom-left
        canvas.drawRect(
            selectionRect.left - cs/2, selectionRect.bottom - cs/2,
            selectionRect.left + cs/2, selectionRect.bottom + cs/2,
            cornerPaint
        )
        
        // Bottom-right
        canvas.drawRect(
            selectionRect.right - cs/2, selectionRect.bottom - cs/2,
            selectionRect.right + cs/2, selectionRect.bottom + cs/2,
            cornerPaint
        )
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                
                // Check if touching a corner
                activeCorner = getCornerAt(event.x, event.y)
                if (activeCorner != Corner.NONE) {
                    isCornerDragging = true
                    return true
                }
                
                // Check if inside selection (for dragging)
                if (selectionRect.contains(event.x, event.y)) {
                    isDragging = true
                    return true
                }
                
                // Start new selection
                isSelecting = true
                selectionRect.set(event.x, event.y, event.x, event.y)
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY
                
                when {
                    isCornerDragging -> {
                        moveCorner(dx, dy)
                    }
                    isDragging -> {
                        selectionRect.offset(dx, dy)
                    }
                    isSelecting -> {
                        selectionRect.right = event.x
                        selectionRect.bottom = event.y
                        normalizeRect()
                    }
                }
                
                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                isSelecting = false
                isDragging = false
                isCornerDragging = false
                activeCorner = Corner.NONE
                
                if (selectionRect.width() > 50 && selectionRect.height() > 50) {
                    onSelectionChanged?.invoke(selectionRect)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun getCornerAt(x: Float, y: Float): Corner {
        val cs = cornerSize * 2
        
        if (distance(x, y, selectionRect.left, selectionRect.top) < cs) 
            return Corner.TOP_LEFT
        if (distance(x, y, selectionRect.right, selectionRect.top) < cs) 
            return Corner.TOP_RIGHT
        if (distance(x, y, selectionRect.left, selectionRect.bottom) < cs) 
            return Corner.BOTTOM_LEFT
        if (distance(x, y, selectionRect.right, selectionRect.bottom) < cs) 
            return Corner.BOTTOM_RIGHT
        
        return Corner.NONE
    }
    
    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }
    
    private fun moveCorner(dx: Float, dy: Float) {
        when (activeCorner) {
            Corner.TOP_LEFT -> {
                selectionRect.left += dx
                selectionRect.top += dy
            }
            Corner.TOP_RIGHT -> {
                selectionRect.right += dx
                selectionRect.top += dy
            }
            Corner.BOTTOM_LEFT -> {
                selectionRect.left += dx
                selectionRect.bottom += dy
            }
            Corner.BOTTOM_RIGHT -> {
                selectionRect.right += dx
                selectionRect.bottom += dy
            }
            Corner.NONE -> {}
        }
        normalizeRect()
    }
    
    private fun normalizeRect() {
        val temp = RectF()
        temp.left = minOf(selectionRect.left, selectionRect.right)
        temp.right = maxOf(selectionRect.left, selectionRect.right)
        temp.top = minOf(selectionRect.top, selectionRect.bottom)
        temp.bottom = maxOf(selectionRect.top, selectionRect.bottom)
        selectionRect = temp
    }
    
    fun setDetectedTextBoxes(boxes: List<RectF>) {
        detectedBoxes.clear()
        detectedBoxes.addAll(boxes)
        invalidate()
    }
    
    fun clearSelection() {
        selectionRect = RectF()
        invalidate()
    }
    
    fun getSelectionRect(): RectF = selectionRect
    
    fun setSelection(rect: RectF) {
        selectionRect = RectF(rect)
        invalidate()
    }
}