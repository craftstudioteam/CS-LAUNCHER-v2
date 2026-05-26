package com.craftstudio.launcher.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

class PixelCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Tool { PENCIL, ERASER, EYEDROPPER }

    var gridSize: Int = 32
        private set
    private var drawColor: Int = Color.BLACK
    var currentTool: Tool = Tool.PENCIL
        private set

    private lateinit var canvasBitmap: Bitmap
    private lateinit var drawCanvas: Canvas

    private var cellSize: Float = 0f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    private var lastGridX: Int = -1
    private var lastGridY: Int = -1

    private val undoStack = ArrayDeque<Bitmap>()
    private val redoStack = ArrayDeque<Bitmap>()
    private val maxUndoSteps = 30

    private val gridPaint = Paint().apply {
        color = 0x33FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = false
    }

    private val pixelPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
    }

    private val checkerPaint = Paint().apply {
        isAntiAlias = false
    }

    private var checkerBitmap: Bitmap? = null

    var listener: OnPixelDrawnListener? = null

    interface OnPixelDrawnListener {
        fun onPixelDrawn()
    }

    init {
        initBitmap()
    }

    private fun initBitmap() {
        canvasBitmap = Bitmap.createBitmap(gridSize, gridSize, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap)
        drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        undoStack.clear()
        redoStack.clear()
        checkerBitmap = null
    }

    fun setGridSize(size: Int) {
        if (size == gridSize) return
        gridSize = size
        initBitmap()
        invalidate()
        listener?.onPixelDrawn()
    }

    fun setDrawColor(color: Int) {
        drawColor = color
    }

    fun getDrawColor(): Int = drawColor

    fun setTool(tool: Tool) {
        currentTool = tool
    }

    fun getBitmap(): Bitmap = canvasBitmap.copy(canvasBitmap.config, true)

    fun getScaledBitmap(scale: Int): Bitmap {
        return Bitmap.createScaledBitmap(canvasBitmap, gridSize * scale, gridSize * scale, false)
    }

    fun clear() {
        saveUndo()
        drawCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        invalidate()
        listener?.onPixelDrawn()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(canvasBitmap.copy(canvasBitmap.config, true))
        if (redoStack.size > maxUndoSteps) redoStack.removeFirst()
        canvasBitmap = undoStack.removeLast()
        drawCanvas = Canvas(canvasBitmap)
        invalidate()
        listener?.onPixelDrawn()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(canvasBitmap.copy(canvasBitmap.config, true))
        if (undoStack.size > maxUndoSteps) undoStack.removeFirst()
        canvasBitmap = redoStack.removeLast()
        drawCanvas = Canvas(canvasBitmap)
        invalidate()
        listener?.onPixelDrawn()
    }

    fun hasUndo(): Boolean = undoStack.isNotEmpty()
    fun hasRedo(): Boolean = redoStack.isNotEmpty()

    private fun saveUndo() {
        undoStack.addLast(canvasBitmap.copy(canvasBitmap.config, true))
        if (undoStack.size > maxUndoSteps) undoStack.removeFirst()
        redoStack.clear()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        recalcCellSize(w, h)
        checkerBitmap = null
    }

    private fun recalcCellSize(w: Int, h: Int) {
        val available = min(w, h).toFloat()
        cellSize = available / gridSize
        offsetX = (w - cellSize * gridSize) / 2f
        offsetY = (h - cellSize * gridSize) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        recalcCellSize(w.toInt(), h.toInt())

        // Draw checkerboard background
        drawCheckerboard(canvas, w, h)

        // Draw the pixel bitmap scaled up
        val dstRect = RectF(offsetX, offsetY, offsetX + cellSize * gridSize, offsetY + cellSize * gridSize)
        canvas.drawBitmap(canvasBitmap, null, dstRect, pixelPaint)

        // Draw grid lines
        for (i in 0..gridSize) {
            val x = offsetX + i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + gridSize * cellSize, gridPaint)
        }
        for (i in 0..gridSize) {
            val y = offsetY + i * cellSize
            canvas.drawLine(offsetX, y, offsetX + gridSize * cellSize, y, gridPaint)
        }
    }

    private fun drawCheckerboard(canvas: Canvas, w: Float, h: Float) {
        if (checkerBitmap == null || checkerBitmap!!.width != w.toInt() || checkerBitmap!!.height != h.toInt()) {
            val bmp = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val checkSize = cellSize / 2
            val lightColor = Color.parseColor("#2A2A2A")
            val darkColor = Color.parseColor("#222222")
            var row = 0
            var y = offsetY
            while (y < offsetY + gridSize * cellSize) {
                var col = 0
                var x = offsetX
                while (x < offsetX + gridSize * cellSize) {
                    checkerPaint.color = if ((row + col) % 2 == 0) lightColor else darkColor
                    c.drawRect(x, y, (x + checkSize).coerceAtMost(offsetX + gridSize * cellSize),
                        (y + checkSize).coerceAtMost(offsetY + gridSize * cellSize), checkerPaint)
                    col++
                    x += checkSize
                }
                row++
                y += checkSize
            }
            checkerBitmap = bmp
        }
        checkerBitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val gridX = floor((event.x - offsetX) / cellSize).toInt()
        val gridY = floor((event.y - offsetY) / cellSize).toInt()

        if (gridX < 0 || gridX >= gridSize || gridY < 0 || gridY >= gridSize) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                saveUndo()
                handleToolAction(gridX, gridY)
                lastGridX = gridX
                lastGridY = gridY
            }
            MotionEvent.ACTION_MOVE -> {
                if (lastGridX != gridX || lastGridY != gridY) {
                    drawLine(lastGridX, lastGridY, gridX, gridY)
                    lastGridX = gridX
                    lastGridY = gridY
                }
            }
            MotionEvent.ACTION_UP -> {
                lastGridX = -1
                lastGridY = -1
                listener?.onPixelDrawn()
            }
        }
        return true
    }

    private fun handleToolAction(x: Int, y: Int) {
        when (currentTool) {
            Tool.PENCIL -> {
                drawCanvas.drawPoint(x.toFloat(), y.toFloat(), pixelPaint.apply { color = drawColor })
                invalidate()
            }
            Tool.ERASER -> {
                erasePixel(x, y)
                invalidate()
            }
            Tool.EYEDROPPER -> {
                val pickedColor = canvasBitmap.getPixel(x, y)
                drawColor = pickedColor
                listener?.onPixelDrawn()
            }
        }
    }

    private fun erasePixel(x: Int, y: Int) {
        canvasBitmap.setPixel(x, y, Color.TRANSPARENT)
    }

    private fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int) {
        // Bresenham's line algorithm
        var cx = x0
        var cy = y0
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            handleToolAction(cx, cy)
            if (cx == x1 && cy == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                cx += sx
            }
            if (e2 < dx) {
                err += dx
                cy += sy
            }
        }
    }
}
