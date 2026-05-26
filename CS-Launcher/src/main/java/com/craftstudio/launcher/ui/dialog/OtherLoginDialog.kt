package com.craftstudio.launcher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import com.craftstudio.launcher.R
import com.craftstudio.launcher.databinding.DialogOtherLoginBinding
import com.craftstudio.launcher.feature.accounts.OtherLoginHelper
import com.craftstudio.launcher.feature.login.Servers.Server
import com.craftstudio.launcher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.craftstudio.launcher.utils.ZHTools

class OtherLoginDialog(
    context: Context,
    private val server: Server,
    private val listener: OtherLoginHelper.OnLoginListener
) : FullScreenDialog(context), View.OnClickListener, DialogInitializationListener {
    private val binding = DialogOtherLoginBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        binding.apply {

            serverName.text = server.serverName
            if (server.register.isEmpty()) {
                registryText.visibility = View.GONE
            } else {
                registryText.setOnClickListener(this@OtherLoginDialog)
            }

            cancelButton.setOnClickListener(this@OtherLoginDialog)
            loginButton.setOnClickListener(this@OtherLoginDialog)
        }

        DraggableDialog.initDialog(this)
    }

    private fun checkAccountInformation(email: String?, password: String?): Boolean {
        val emailEmpty = email.isNullOrEmpty()
        val passwordEmpty = password.isNullOrEmpty()

        return if (emailEmpty || passwordEmpty) {
            val errorString = context.getString(R.string.generic_error_field_empty)
            if (emailEmpty) binding.emailEdit.error = errorString
            if (passwordEmpty) binding.passwordEdit.error = errorString
            false
        } else true
    }

    override fun onInit(): Window? = window

    override fun onClick(v: View) {
        binding.apply {
            when (v) {
                cancelButton -> dismiss()
                registryText -> {
                    server.register.takeIf { it.isNotEmpty() }?.let { link ->
                        ZHTools.openLink(context, link)
                        dismiss()
                    }
                }
                loginButton -> {
                    val email = emailEdit.text.toString()
                    val password = passwordEdit.text.toString()
                    //登录前需检查邮箱、密码、基础链接
                    if (!checkAccountInformation(email, password)) return
                    if (server.baseUrl.isNullOrEmpty()) {
                        Toast.makeText(context, context.getString(R.string.other_login_server_not_empty), Toast.LENGTH_SHORT).show()
                        return
                    }

                    OtherLoginHelper(server.baseUrl, server.serverName, email, password, listener).createNewAccount(context)

                    dismiss()
                }
            }
        }
    }
}