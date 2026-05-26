package com.craftstudio.launcher.ui.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.annotation.CheckResult
import com.craftstudio.launcher.databinding.DialogTipBinding
import com.craftstudio.launcher.ui.dialog.DraggableDialog.DialogInitializationListener


class TipDialog private constructor(
    context: Context,
    private val title: String?,
    private val message: String?,
    private val confirm: String?,
    private val cancel: String?,
    private val checkBoxText: String?,
    private val showCheckBox: Boolean,
    private val showCancel: Boolean,
    private val showConfirm: Boolean,
    private val centerMessage: Boolean,
    private var selectable: Boolean,
    private val confirmButtonCountdown: Long,
    private val warning: Boolean,
    private val textBeautifier: TextBeautifier?,
    private val cancelListener: OnCancelClickListener?,
    private val confirmListener: OnConfirmClickListener?,
    private val dismissListener: OnDialogDismissListener?
) : FullScreenDialog(context), DialogInitializationListener {
    private val binding = DialogTipBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fun TextView.addText(textString: String?) {
            this.text = textString
            textString ?: run { this.visibility = View.GONE }
        }

        binding.apply {
            setContentView(root)
            DraggableDialog.initDialog(this@TipDialog)

            titleView.addText(title)
            messageView.addText(message)

            messageView.setTextIsSelectable(selectable)

            textBeautifier?.beautify(titleView, messageView)

            checkHeight()

            cancel?.apply { cancelButton.text = this }
            confirm?.apply { confirmButton.text = this }
            if (centerMessage) messageView.gravity = Gravity.CENTER_HORIZONTAL
            if (showCheckBox) {
                checkBox.visibility = View.VISIBLE
                checkBoxText?.let { checkBox.text = it }
            }

            cancelButton.setOnClickListener {
                cancelListener?.onCancelClick()
                this@TipDialog.dismiss()
            }
            confirmButton.setOnClickListener {
                confirmListener?.onConfirmClick(checkBox.isChecked)
                this@TipDialog.dismiss()
            }

            cancelButton.visibility = if (showCancel) View.VISIBLE else View.GONE
            confirmButton.visibility = if (showConfirm) View.VISIBLE else View.GONE

            //如果开启了警告模式，那么就为标题添加一个红色的警告图标
            if (warning) {
                warningIcon.visibility = View.VISIBLE
                warningIcon.drawable.setTint(Color.RED)
            }
        }
    }

    private fun checkHeight() {
        checkHeight(binding.root, binding.contentView, binding.scrollView)
    }

    override fun show() {
        super.show()
        //尝试修复一些设备上的View宽度不正确的问题，在这里进行测量
        window?.findViewById<View>(android.R.id.content)?.measure(0, 0)

        if (confirmButtonCountdown > 0) {
            binding.confirmButton.apply {
                isEnabled = false

                val buttonText = text
                var remainingTime = confirmButtonCountdown

                val interval = 500L //更新频率
                val handler = Handler(Looper.getMainLooper())
                val runnable = object : Runnable {
                    @SuppressLint("SetTextI18n")
                    override fun run() {
                        if (remainingTime > 0) {
                            val secondsRemaining = (remainingTime / 1000.0).toInt()
                            text = "$buttonText (${secondsRemaining}s)"
                            remainingTime -= interval
                            handler.postDelayed(this, interval)
                        } else {
                            isEnabled = true
                            text = buttonText
                        }
                    }
                }

                handler.post(runnable)
            }
        }
    }

    override fun dismiss() {
        if (dismissListener?.onDismiss() == false) return
        super.dismiss()
    }

    override fun onInit(): Window? = window

    fun interface TextBeautifier {
        fun beautify(titleText: TextView, messageText: TextView)
    }

    fun interface OnCancelClickListener {
        fun onCancelClick()
    }

    fun interface OnConfirmClickListener {
        fun onConfirmClick(checked: Boolean)
    }

    fun interface OnDialogDismissListener {
        fun onDismiss(): Boolean
    }

    open class Builder(private val context: Context) {
        private var title: String? = null
        private var message: String? = null
        private var cancel: String? = null
        private var confirm: String? = null
        private var checkBox: String? = null
        private var textBeautifier: TextBeautifier? = null
        private var cancelClickListener: OnCancelClickListener? = null
        private var confirmClickListener: OnConfirmClickListener? = null
        private var dialogDismissListener: OnDialogDismissListener? = null
        private var confirmButtonCountdown: Long = 0L
        private var cancelable = true
        private var showCheckBox = false
        private var showCancel = true
        private var showConfirm = true
        private var centerMessage = true
        private var selectable = false
        private var warning = false

        fun buildDialog(): TipDialog {
            if (confirmButtonCountdown > 0 && cancelable)
                throw IllegalArgumentException("Before setting the confirm button countdown, please disable the cancelable option first.")
            return TipDialog(
                this.context,
                title, message, confirm, cancel, checkBox,
                showCheckBox,
                showCancel, showConfirm, centerMessage, selectable, confirmButtonCountdown, warning,
                textBeautifier, cancelClickListener, confirmClickListener, dialogDismissListener
            ).apply {
                setCancelable(cancelable)
                create()
            }
        }

        fun showDialog() {
            buildDialog().show()
        }

        @CheckResult
        fun setTitle(title: String?): Builder {
            this.title = title
            return this
        }

        @CheckResult
        fun setTitle(title: Int): Builder {
            return setTitle(context.getString(title))
        }

        @CheckResult
        fun setMessage(message: String?): Builder {
            this.message = message
            return this
        }

        @CheckResult
        fun setMessage(message: Int): Builder {
            return setMessage(context.getString(message))
        }

        @CheckResult
        fun setCancel(cancel: String?): Builder {
            this.cancel = cancel
            return this
        }

        @CheckResult
        fun setCancel(cancel: Int): Builder {
            return setCancel(context.getString(cancel))
        }

        @CheckResult
        fun setCheckBox(checkBoxText: String?): Builder {
            this.checkBox = checkBoxText
            return this
        }

        @CheckResult
        fun setCheckBox(checkBoxText: Int): Builder {
            return setCheckBox(context.getString(checkBoxText))
        }

        @CheckResult
        fun setConfirm(confirm: String?): Builder {
            this.confirm = confirm
            return this
        }

        @CheckResult
        fun setConfirm(confirm: Int): Builder {
            return setConfirm(context.getString(confirm))
        }

        @CheckResult
        fun setShowCheckBox(show: Boolean): Builder {
            showCheckBox = show
            return this
        }

        @CheckResult
        fun setTextBeautifier(beautifier: TextBeautifier): Builder {
            this.textBeautifier = beautifier
            return this
        }

        @CheckResult
        fun setCancelClickListener(cancelClickListener: OnCancelClickListener?): Builder {
            this.cancelClickListener = cancelClickListener
            return this
        }

        @CheckResult
        fun setConfirmClickListener(confirmClickListener: OnConfirmClickListener?): Builder {
            this.confirmClickListener = confirmClickListener
            return this
        }

        @CheckResult
        fun setDialogDismissListener(dialogDismissListener: OnDialogDismissListener?): Builder {
            this.dialogDismissListener = dialogDismissListener
            return this
        }

        @CheckResult
        fun setConfirmButtonCountdown(countdownMillis: Long): Builder {
            if (countdownMillis < 0L) throw IllegalArgumentException("The countdown cannot be negative!")
            this.confirmButtonCountdown = countdownMillis
            return this
        }

        @CheckResult
        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        @CheckResult
        fun setShowCancel(showCancel: Boolean): Builder {
            this.showCancel = showCancel
            return this
        }

        @CheckResult
        fun setShowConfirm(showConfirm: Boolean): Builder {
            this.showConfirm = showConfirm
            return this
        }

        @CheckResult
        fun setCenterMessage(center: Boolean): Builder {
            this.centerMessage = center
            return this
        }

        @CheckResult
        fun setSelectable(selectable: Boolean): Builder {
            this.selectable = selectable
            return this
        }

        /**
         * 为标题栏添加红色警告图标
         */
        @CheckResult
        fun setWarning(): Builder {
            this.warning = true
            return this
        }
    }
}
