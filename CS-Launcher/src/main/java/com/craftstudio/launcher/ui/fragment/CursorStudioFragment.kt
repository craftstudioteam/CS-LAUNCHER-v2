package com.craftstudio.launcher.ui.fragment

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.craftstudio.launcher.R
import com.craftstudio.launcher.colorselector.ColorSelector
import com.craftstudio.launcher.colorselector.ColorSelectionListener
import com.craftstudio.launcher.databinding.FragmentCursorStudioBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.view.PixelCanvasView
import com.craftstudio.launcher.utils.file.FileTools.Companion.mkdirs
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.anim.AnimPlayer
import com.craftstudio.launcher.anim.animations.Animations
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CursorStudioFragment : FragmentWithAnim() {

    companion object {
        const val TAG = "CursorStudioFragment"

        private val PRESET_COLORS = listOf(
            Color.BLACK,
            Color.WHITE,
            Color.parseColor("#FF5252"),
            Color.parseColor("#FF6B00"),
            Color.parseColor("#FFB74D"),
            Color.parseColor("#FFEB3B"),
            Color.parseColor("#CDDC39"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#009688"),
            Color.parseColor("#00BCD4"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#3F51B5"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#795548"),
            Color.parseColor("#9E9E9E"),
            Color.parseColor("#424242"),
            Color.parseColor("#03A9F4"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#673AB7")
        )
    }

    private lateinit var binding: FragmentCursorStudioBinding
    private var currentColor = Color.BLACK
    private var currentGridSize = 32
    private var onCursorSavedListener: (() -> Unit)? = null

    fun setOnCursorSavedListener(listener: () -> Unit) {
        onCursorSavedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCursorStudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCanvas()
        setupToolPanel()
        setupColorPalette()
        setupGridToggle()
        setupSaveButton()
        setupBackButton()
        updatePreview()
        updateCurrentColorDisplay()
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer
            .apply(AnimPlayer.Entry(binding.toolPanel, Animations.BounceInLeft))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.toolPanel, Animations.FadeOutLeft))
    }

    private fun setupCanvas() {
        binding.pixelCanvas.listener = object : PixelCanvasView.OnPixelDrawnListener {
            override fun onPixelDrawn() {
                updatePreview()
            }
        }
    }

    private fun setupToolPanel() {
        selectTool(PixelCanvasView.Tool.PENCIL)

        binding.toolPencil.setOnClickListener { selectTool(PixelCanvasView.Tool.PENCIL) }
        binding.toolEraser.setOnClickListener { selectTool(PixelCanvasView.Tool.ERASER) }
        binding.toolEyedropper.setOnClickListener { selectTool(PixelCanvasView.Tool.EYEDROPPER) }

        binding.toolUndo.setOnClickListener {
            binding.pixelCanvas.undo()
            updatePreview()
        }
        binding.toolRedo.setOnClickListener {
            binding.pixelCanvas.redo()
            updatePreview()
        }
        binding.toolClear.setOnClickListener { clearCanvas() }
    }

    private fun selectTool(tool: PixelCanvasView.Tool) {
        binding.pixelCanvas.setTool(tool)

        // If eyedropper, we'll switch back to pencil after picking
        if (tool == PixelCanvasView.Tool.EYEDROPPER) {
            // Eyedropper picks color via the canvas touch handler
            // The color will be updated through the listener
        }

        // Update tool button backgrounds
        val activeBg = R.drawable.bg_cursor_studio_tool_active
        val inactiveBg = R.drawable.bg_cursor_studio_tool_inactive

        binding.toolPencil.setBackgroundResource(if (tool == PixelCanvasView.Tool.PENCIL) activeBg else inactiveBg)
        binding.toolEraser.setBackgroundResource(if (tool == PixelCanvasView.Tool.ERASER) activeBg else inactiveBg)
        binding.toolEyedropper.setBackgroundResource(if (tool == PixelCanvasView.Tool.EYEDROPPER) activeBg else inactiveBg)

        // Update icon tints
        val activeTint = Color.WHITE
        val inactiveTint = Color.parseColor("#AAAAAA")

        (binding.toolPencil.getChildAt(0) as? ImageView)?.setColorFilter(if (tool == PixelCanvasView.Tool.PENCIL) activeTint else inactiveTint)
        (binding.toolEraser.getChildAt(0) as? ImageView)?.setColorFilter(if (tool == PixelCanvasView.Tool.ERASER) activeTint else inactiveTint)
        (binding.toolEyedropper.getChildAt(0) as? ImageView)?.setColorFilter(if (tool == PixelCanvasView.Tool.EYEDROPPER) activeTint else inactiveTint)
    }

    private fun setupColorPalette() {
        val paletteLayout = binding.colorPalette
        paletteLayout.removeAllViews()

        PRESET_COLORS.forEach { color ->
            val swatch = View(requireContext()).apply {
                val size = (28 * resources.displayMetrics.density).toInt()
                layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                    marginEnd = (4 * resources.displayMetrics.density).toInt()
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)
                    setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#333333"))
                }
                setOnClickListener { selectColor(color) }
            }
            paletteLayout.addView(swatch)
        }

        // Custom color picker button
        val customBtn = TextView(requireContext()).apply {
            val size = (28 * resources.displayMetrics.density).toInt()
            layoutParams = ViewGroup.MarginLayoutParams(size, size).apply {
                marginEnd = (4 * resources.displayMetrics.density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#333333"))
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#FF6B00"))
            }
            text = "+"
            setTextColor(Color.parseColor("#FF6B00"))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
            setOnClickListener { showColorPickerDialog() }
        }
        paletteLayout.addView(customBtn)
    }

    private fun selectColor(color: Int) {
        currentColor = color
        binding.pixelCanvas.setDrawColor(color)
        // Switch to pencil when selecting a color
        selectTool(PixelCanvasView.Tool.PENCIL)
        updateCurrentColorDisplay()
    }

    private fun updateCurrentColorDisplay() {
        binding.currentColorDisplay.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 4 * resources.displayMetrics.density
            setColor(currentColor)
            setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#555555"))
        }
    }

    private fun showColorPickerDialog() {
        val container = FrameLayout(requireContext())
        val colorSelector = ColorSelector(requireContext(), container, object : ColorSelectionListener {
            override fun onColorSelected(color: Int) {
                selectColor(color)
            }
        })
        colorSelector.setAlphaEnabled(false)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.cursor_studio_custom_color)
            .setView(container)
            .setPositiveButton(R.string.generic_confirm, null)
            .setNegativeButton(R.string.generic_cancel, null)
            .show()
    }

    private fun setupGridToggle() {
        updateGridToggleUI()

        binding.grid16.setOnClickListener {
            if (currentGridSize != 16) {
                confirmGridChange(16)
            }
        }
        binding.grid32.setOnClickListener {
            if (currentGridSize != 32) {
                confirmGridChange(32)
            }
        }
    }

    private fun confirmGridChange(newSize: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.generic_warning)
            .setMessage(R.string.cursor_studio_grid_change_confirm)
            .setPositiveButton(R.string.generic_confirm) { _, _ ->
                currentGridSize = newSize
                binding.pixelCanvas.setGridSize(newSize)
                updateGridToggleUI()
                updatePreview()
            }
            .setNegativeButton(R.string.generic_cancel, null)
            .show()
    }

    private fun updateGridToggleUI() {
        val activeBg = R.drawable.bg_cursor_studio_grid_toggle_active
        val inactiveBg = R.drawable.bg_cursor_studio_grid_toggle_inactive

        binding.grid16.setBackgroundResource(if (currentGridSize == 16) activeBg else inactiveBg)
        binding.grid16.setTextColor(if (currentGridSize == 16) Color.WHITE else Color.parseColor("#808080"))
        if (currentGridSize == 16) binding.grid16.setTypeface(null, android.graphics.Typeface.BOLD)
        else binding.grid16.setTypeface(null, android.graphics.Typeface.NORMAL)

        binding.grid32.setBackgroundResource(if (currentGridSize == 32) activeBg else inactiveBg)
        binding.grid32.setTextColor(if (currentGridSize == 32) Color.WHITE else Color.parseColor("#808080"))
        if (currentGridSize == 32) binding.grid32.setTypeface(null, android.graphics.Typeface.BOLD)
        else binding.grid32.setTypeface(null, android.graphics.Typeface.NORMAL)
    }

    private fun updatePreview() {
        val bitmap = binding.pixelCanvas.getBitmap()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width * 4, bitmap.height * 4, false)

        binding.previewLight.setImageBitmap(scaledBitmap)
        binding.previewDark.setImageBitmap(scaledBitmap)

        // Switch back from eyedropper to pencil after color pick
        if (binding.pixelCanvas.currentTool == PixelCanvasView.Tool.EYEDROPPER) {
            currentColor = binding.pixelCanvas.getDrawColor()
            updateCurrentColorDisplay()
            selectTool(PixelCanvasView.Tool.PENCIL)
        }
    }

    private fun clearCanvas() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.generic_warning)
            .setMessage(R.string.cursor_studio_clear_confirm)
            .setPositiveButton(R.string.generic_confirm) { _, _ ->
                binding.pixelCanvas.clear()
                updatePreview()
            }
            .setNegativeButton(R.string.generic_cancel, null)
            .show()
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener { saveAndApply() }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun saveAndApply() {
        val bitmap = binding.pixelCanvas.getScaledBitmap(4)

        Task.runTask {
            val mouseDir = File(PathManager.DIR_CUSTOM_MOUSE).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "cursor_$timestamp.png"
            val file = File(mouseDir, fileName)

            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                bitmap.recycle()
                file.absolutePath
            } catch (e: Exception) {
                null
            }
        }.ended(TaskExecutors.getAndroidUI()) { filePath ->
            if (filePath != null) {
                val fileName = File(filePath).name
                AllSettings.customMouse.put(fileName).save()
                Toast.makeText(requireContext(), R.string.custom_mouse_saved, Toast.LENGTH_SHORT).show()
                onCursorSavedListener?.invoke()
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), R.string.custom_mouse_save_failed, Toast.LENGTH_SHORT).show()
            }
        }.execute()
    }
}
