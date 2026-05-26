package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.craftstudio.launcher.databinding.DialogSelectItemBinding

abstract class AbstractSelectDialog(context: Context) : FullScreenDialog(context) {
    protected val binding = DialogSelectItemBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.closeButton.setOnClickListener { this.dismiss() }
        initDialog(binding.recyclerView)
    }

    fun setTitleText(text: Int) {
        setTitleText(context.getString(text))
    }

    fun setTitleText(text: String) {
        binding.titleView.text = text
    }

    fun setMessageText(text: Int) {
        setMessageText(context.getString(text))
    }

    fun setMessageText(text: String?) {
        binding.messageView.text = text
        binding.messageView.visibility = if (text != null) View.VISIBLE else View.GONE
    }

    abstract fun initDialog(recyclerView: RecyclerView)
}
