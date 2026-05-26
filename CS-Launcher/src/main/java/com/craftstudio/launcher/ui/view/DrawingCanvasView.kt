package com.craftstudio.launcher.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private var gridSize = 32
    private var cellSize = 0f
    private var currentColor = Color.BLACK
    private var isEraserMode = false

    private val undoStack = mutableListOf<Bitmap>()
    private val redoStack = mutableListOf<Bitmap>()
    private val maxHistory = 20

    private var lastX = -1
    private var lastY = -1

    private var onDrawChangedListener: (() -> Unit)? = null

    fun setOnDrawChangedListener(listener: () -> Unit) {
        onDrawChangedListener = listener
    }

    fun setGridSize(size: Int) {
        gridSize = size
        initializeBitmap()
        invalidate()
    }

    fun setColor(color: Int) {
        currentColor = color
        isEraserMode = false
        paint.color = color
    }

    fun setEraserMode(enabled: Boolean) {
        isEraserMode = enabled
    }

    fun isEraserMode(): Boolean = isEraserMode

    fun getCurrentColor(): Int = currentColor

    private fun initializeBitmap() {
        bitmap = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
        canvas?.drawColor(Color.TRANSPARENT)
        clearHistory()
    }

    private fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = minOf(w, h).toFloat() / gridSize
        initializeBitmap()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#1A1A1A"))

        bitmap?.let { bmp ->
            if (bmp.isRecycled) return@let
            val scaledBitmap = Bitmap.createScaledBitmap(bmp, (cellSize * gridSize).toInt(), (cellSize * gridSize).toInt(), false)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
            if (scaledBitmap !== bmp) scaledBitmap.recycle()
        }

        for (i in 0..gridSize) {
            canvas.drawLine(i * cellSize, 0f, i * cellSize, gridSize * cellSize, gridPaint)
            canvas.drawLine(0f, i * cellSize, gridSize * cellSize, i * cellSize, gridPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x / cellSize).toInt()
        val y = (event.y / cellSize).toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                saveToUndoStack()
                drawPixel(x, y)
                lastX = x
                lastY = y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (lastX >= 0 && lastY >= 0) {
                    drawLine(lastX, lastY, x, y)
                    lastX = x
                    lastY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                lastX = -1
                lastY = -1
                onDrawChangedListener?.invoke()
            }
        }
        invalidate()
        return true
    }

    private fun drawPixel(x: Int, y: Int) {
        if (x < 0 || x >= gridSize || y < 0 || y >= gridSize) return

        paint.color = if (isEraserMode) Color.TRANSPARENT else currentColor
        canvas?.drawRect(
            x.toFloat(),
            y.toFloat(),
            (x + 1).toFloat(),
            (y + 1).toFloat(),
            paint
        )
    }

    private fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int) {
        val dx = kotlin.math.abs(x1 - x0)
        val dy = kotlin.math.abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        var x = x0
        var y = y0

        while (true) {
            drawPixel(x, y)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
    }

    private fun saveToUndoStack() {
        bitmap?.let { bmp ->
            if (undoStack.size >= maxHistory) {
                undoStack.removeAt(0).recycle()
            }
            undoStack.add(bmp.copy(bmp.config, true))
            redoStack.forEach { it.recycle() }
            redoStack.clear()
        }
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        bitmap?.let { current ->
            if (!current.isRecycled) redoStack.add(current.copy(current.config, true))
        }
        val previous = undoStack.removeAt(undoStack.size - 1)
        if (previous.isRecycled) return false
        bitmap?.recycle()
        bitmap = previous
        canvas = Canvas(bitmap!!)
        invalidate()
        onDrawChangedListener?.invoke()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        bitmap?.let { current ->
            if (!current.isRecycled) undoStack.add(current.copy(current.config, true))
        }
        val next = redoStack.removeAt(redoStack.size - 1)
        if (next.isRecycled) return false
        bitmap?.recycle()
        bitmap = next
        canvas = Canvas(bitmap!!)
        invalidate()
        onDrawChangedListener?.invoke()
        return true
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        saveToUndoStack()
        canvas?.drawColor(Color.TRANSPARENT)
        invalidate()
        onDrawChangedListener?.invoke()
    }

    fun getBitmap(): Bitmap? {
        return bitmap?.copy(Bitmap.Config.ARGB_8888, false)
    }

    fun getScaledBitmap(scale: Int): Bitmap? {
        return bitmap?.let { bmp ->
            Bitmap.createScaledBitmap(bmp, bmp.width * scale, bmp.height * scale, true)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        undoStack.forEach { it.recycle() }
        redoStack.forEach { it.recycle() }
    }
}