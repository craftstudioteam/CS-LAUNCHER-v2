package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.EditText
import androidx.annotation.CheckResult
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.DialogEditTextBinding
import com.craftstudio.launcher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt.Companion.isEmptyOrBlank

class EditTextDialog private constructor(
    private val context: Context,
    private val title: String?,
    private val message: String?,
    private val editText: String?,
    private val hintText: String?,
    private val checkBox: String?,
    private val confirm: String?,
    private val emptyError: String?,
    private val showCheckBox: Boolean,
    private val inputType: Int,
    private val cancelListener: View.OnClickListener?,
    private val confirmListener: ConfirmListener?,
    private val required: Boolean
) : FullScreenDialog(context),
    DialogInitializationListener {
    private val binding = DialogEditTextBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setCancelable(false)
        this.setContentView(binding.root)
        init()
        DraggableDialog.initDialog(this)
    }

    private fun init() {
        title?.let { binding.titleView.text = it }
        message?.let {
            binding.messageView.text = it
            binding.messageView.visibility = View.VISIBLE
        }
        editText?.let { binding.textEdit.setText(it) }
        hintText?.let { binding.textEdit.hint = it } ?: run {
            if (required) binding.textEdit.setHint(R.string.generic_required)
        }

        checkHeight()

        confirm?.let { binding.confirmButton.text = it }
        if (showCheckBox) {
            binding.checkBox.visibility = View.VISIBLE
            binding.checkBox.text = this.checkBox
        }
        if (inputType != -1) binding.textEdit.inputType = inputType

        confirmListener?.let { listener ->
            binding.confirmButton.setOnClickListener {
                if (required) {
                    val text = binding.textEdit.text.toString()
                    if (isEmptyOrBlank(text)) {
                        binding.textEdit.error = emptyError
                            ?: context.getString(R.string.generic_error_field_empty)
                        return@setOnClickListener
                    }
                }
                val dismissDialog = listener.onConfirm(binding.textEdit, binding.checkBox.isChecked)
                if (dismissDialog) dismiss()
            }
        }

        val cListener = cancelListener ?: View.OnClickListener { dismiss() }
        binding.cancelButton.setOnClickListener(cListener)
    }

    private fun checkHeight() {
        checkHeight(binding.root, binding.contentView, binding.scrollView)
    }

    override fun onInit(): Window? = window

    fun interface ConfirmListener {
        fun onConfirm(editText: EditText, checked: Boolean): Boolean
    }

    class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var editText: String? = null
        private var hintText: String? = null
        private var checkBox: String? = null
        private var confirm: String? = null
        private var emptyError: String? = null
        private var showCheckBox = false
        private var inputType = -1
        private var cancelListener: View.OnClickListener? = null
        private var confirmListener: ConfirmListener? = null
        private var required = false

        @CheckResult fun setTitle(title: String): Builder { this.title = title; return this }
        @CheckResult fun setTitle(title: Int): Builder { return setTitle(context.getString(title)) }
        @CheckResult fun setMessage(message: String): Builder { this.message = message; return this }
        @CheckResult fun setMessage(message: Int): Builder { return setMessage(context.getString(message)) }
        @CheckResult fun setEditText(editText: String): Builder { this.editText = editText; return this }
        @CheckResult fun setHintText(hintText: Int): Builder { return setHintText(context.getString(hintText)) }
        @CheckResult fun setHintText(hintText: String): Builder { this.hintText = hintText; return this }
        @CheckResult fun setConfirmText(text: Int): Builder { return setConfirmText(context.getString(text)) }
        @CheckResult fun setConfirmText(text: String): Builder { this.confirm = text; return this }
        @CheckResult fun setEmptyErrorText(text: Int): Builder { return setEmptyErrorText(context.getString(text)) }
        @CheckResult fun setEmptyErrorText(text: String): Builder { this.emptyError = text; return this }
        @CheckResult fun setShowCheckBox(show: Boolean): Builder { this.showCheckBox = show; return this }
        @CheckResult fun setCheckBoxText(text: Int): Builder { return setCheckBoxText(context.getString(text)) }
        @CheckResult fun setCheckBoxText(text: String): Builder { this.checkBox = text; return this }
        @CheckResult fun setInputType(inputType: Int): Builder { this.inputType = inputType; return this }
        @CheckResult fun setCancelListener(cancel: View.OnClickListener): Builder { this.cancelListener = cancel; return this }
        @CheckResult fun setConfirmListener(confirmListener: ConfirmListener): Builder { this.confirmListener = confirmListener; return this }
        @CheckResult fun setAsRequired(): Builder { this.required = true; return this }

        fun buildDialog(): EditTextDialog {
            return EditTextDialog(
                context,
                title, message, editText, hintText, checkBox, confirm, emptyError,
                showCheckBox, inputType,
                cancelListener, confirmListener,
                required
            ).apply { create() }
        }

        fun showDialog() { buildDialog().show() }
    }
}
