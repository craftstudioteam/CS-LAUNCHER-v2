package com.craftstudio.launcher.ui.fragment

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.FragmentMouseDrawBinding
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.utils.file.FileTools.Companion.mkdirs
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MouseDrawFragment : Fragment() {

    private var _binding: FragmentMouseDrawBinding? = null
    private val binding get() = _binding!!

    private var currentColor = Color.BLACK
    private var onCursorSavedListener: (() -> Unit)? = null

    fun setOnCursorSavedListener(listener: () -> Unit) {
        onCursorSavedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMouseDrawBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews()
        setupDrawingCanvas()
    }

    private fun initViews() {
        binding.colorPreview.setBackgroundColor(currentColor)

        binding.btnPencil.setOnClickListener { selectPencil() }
        binding.btnEraser.setOnClickListener { selectEraser() }
        binding.btnUndo.setOnClickListener { undo() }
        binding.btnRedo.setOnClickListener { redo() }
        binding.btnClear.setOnClickListener { clearCanvas() }
        binding.saveButton.setOnClickListener { saveAndApply() }

        binding.btnColorPicker.setOnClickListener { showColorPickerDialog() }

        setupColorPresets()
        setupGridSizeSpinner()
    }

    private fun setupDrawingCanvas() {
        binding.drawingCanvas.setOnDrawChangedListener {
            updateUndoRedoButtons()
        }
        updateUndoRedoButtons()
    }

    private fun selectPencil() {
        binding.drawingCanvas.setEraserMode(false)
        binding.drawingCanvas.setColor(currentColor)
        updateToolSelection(true)
    }

    private fun selectEraser() {
        binding.drawingCanvas.setEraserMode(true)
        updateToolSelection(false)
    }

    private fun updateToolSelection(isPencil: Boolean) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.tab_selected)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        binding.btnPencil.setColorFilter(if (isPencil) activeColor else inactiveColor)
        binding.btnEraser.setColorFilter(if (!isPencil) activeColor else inactiveColor)
    }

    private fun undo() {
        binding.drawingCanvas.undo()
    }

    private fun redo() {
        binding.drawingCanvas.redo()
    }

    private fun updateUndoRedoButtons() {
        val enabledColor = ContextCompat.getColor(requireContext(), R.color.primary_text)
        val disabledColor = ContextCompat.getColor(requireContext(), R.color.secondary_text)

        binding.btnUndo.isEnabled = binding.drawingCanvas.canUndo()
        binding.btnUndo.setColorFilter(if (binding.drawingCanvas.canUndo()) enabledColor else disabledColor)

        binding.btnRedo.isEnabled = binding.drawingCanvas.canRedo()
        binding.btnRedo.setColorFilter(if (binding.drawingCanvas.canRedo()) enabledColor else disabledColor)
    }

    private fun clearCanvas() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.generic_warning)
            .setMessage("Clear the canvas?")
            .setPositiveButton(R.string.generic_confirm) { _, _ ->
                binding.drawingCanvas.clear()
            }
            .setNegativeButton(R.string.generic_cancel, null)
            .show()
    }

    private fun setupColorPresets() {
        val colorViews = listOf(
            binding.colorBlack to Color.BLACK,
            binding.colorWhite to Color.WHITE,
            binding.colorRed to Color.parseColor("#FF5252"),
            binding.colorOrange to Color.parseColor("#FF9800"),
            binding.colorYellow to Color.parseColor("#FFEB3B"),
            binding.colorGreen to Color.parseColor("#4CAF50"),
            binding.colorBlue to Color.parseColor("#2196F3"),
            binding.colorPurple to Color.parseColor("#9C27B0"),
            binding.colorPink to Color.parseColor("#E91E63")
        )

        colorViews.forEach { (view, color) ->
            view.setOnClickListener {
                selectColor(color)
            }
        }
    }

    private fun selectColor(color: Int) {
        currentColor = color
        binding.colorPreview.setBackgroundColor(color)
        binding.drawingCanvas.setColor(color)
        binding.drawingCanvas.setEraserMode(false)
        updateToolSelection(true)
    }

    private fun showColorPickerDialog() {
        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "#FFB74D"
            filters = arrayOf(InputFilter.LengthFilter(7))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val container = FrameLayout(requireContext()).apply {
            setPadding(48, 16, 48, 16)
            addView(editText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.custom_mouse_color_picker)
            .setView(container)
            .setPositiveButton(R.string.generic_confirm) { _, _ ->
                val hexColor = editText.text.toString()
                try {
                    val color = Color.parseColor(hexColor)
                    selectColor(color)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.custom_mouse_invalid_color, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.generic_cancel, null)
            .show()
    }

    private fun setupGridSizeSpinner() {
        binding.spinnerGridSize.setSelection(2)
        binding.spinnerGridSize.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val gridSizes = listOf(16, 24, 32, 48)
                binding.drawingCanvas.setGridSize(gridSizes[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun saveAndApply() {
        val bitmap = binding.drawingCanvas.getScaledBitmap(4) ?: run {
            Toast.makeText(requireContext(), R.string.custom_mouse_save_failed, Toast.LENGTH_SHORT).show()
            return
        }

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
            } else {
                Toast.makeText(requireContext(), R.string.custom_mouse_save_failed, Toast.LENGTH_SHORT).show()
            }
        }.execute()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}