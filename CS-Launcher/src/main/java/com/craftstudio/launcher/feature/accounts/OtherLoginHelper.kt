package com.craftstudio.launcher.feature.accounts

import android.content.Context
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.feature.login.AuthResult
import com.craftstudio.launcher.feature.login.OtherLoginApi
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.SelectRoleDialog
import com.craftstudio.launcher.value.MinecraftAccount
import java.util.Objects

/**
 * 帮助登录外置账号（创建新的外置账号、仅登录当前外置账号）
 */
class OtherLoginHelper(
    private val baseUrl: String,
    private val serverName: String,
    private val email: String,
    private val password: String,
    private val listener: OnLoginListener
) {
    private fun login(context: Context, loginListener: LoginAccountListener) {
        Task.runTask {
            OtherLoginApi.setBaseUrl(baseUrl)
            OtherLoginApi.login(context, email, password,
                object : OtherLoginApi.Listener {
                    override fun onSuccess(authResult: AuthResult) {
                        if (!Objects.isNull(authResult.selectedProfile)) {
                            loginListener.onlyOneRole(authResult)
                        } else {
                            loginListener.hasMultipleRoles(authResult)
                        }
                    }

                    override fun onFailed(error: String) {
                        TaskExecutors.runInUIThread {
                            listener.unLoading()
                            listener.onFailed(error)
                        }
                    }
                })
        }.beforeStart(TaskExecutors.getAndroidUI()) {
            listener.onLoading()
        }.onThrowable { e ->
            val message = "An exception was encountered while performing the login task."
            Logging.e("Other Login", message, e)
            TaskExecutors.runInUIThread {
                listener.onFailed(e.message ?: message)
            }
        }.execute()
    }

    /**
     * 将账号信息写入到账号对象中（单独区分出来是为了适配仅登录的情况，刷新账号信息）
     * @param account 需要写入的账号
     */
    private fun writeAccount(
        account: MinecraftAccount,
        authResult: AuthResult,
        userName: String,
        profileId: String,
        updateSkin: Boolean = true,
    ) {
        account.apply {
            this.accessToken = authResult.accessToken
            this.clientToken = authResult.clientToken
            this.otherBaseUrl = baseUrl
            this.otherAccount = email
            this.otherPassword = password
            this.accountType = serverName
            this.username = userName
            this.profileId = profileId
        }
        if (updateSkin) account.updateOtherSkin()
    }

    /**
     * 通过账号密码，登录一个新的账号
     */
    fun createNewAccount(context: Context) {
        login(context, object : LoginAccountListener {
            override fun onlyOneRole(authResult: AuthResult) {
                val profileId = authResult.selectedProfile.id
                val account: MinecraftAccount = MinecraftAccount.loadFromProfileID(profileId) ?: MinecraftAccount()
                writeAccount(account, authResult, authResult.selectedProfile.name, profileId)
                TaskExecutors.runInUIThread {
                    listener.unLoading()
                    listener.onSuccess(account)
                }
            }

            override fun hasMultipleRoles(authResult: AuthResult) {
                TaskExecutors.runInUIThread {
                    val selectRoleDialog = SelectRoleDialog(
                        context,
                        authResult.availableProfiles
                    ) { selectedProfile ->
                        val profileId = selectedProfile.id
                        val account: MinecraftAccount = MinecraftAccount.loadFromProfileID(profileId) ?: MinecraftAccount()
                        writeAccount(account, authResult, selectedProfile.name, profileId, updateSkin = false)
                        refresh(context, account)
                    }
                    listener.unLoading()
                    selectRoleDialog.show()
                }
            }
        })
    }

    /**
     * 仅仅只是登录外置账号（使用账号密码登录）
     * JUST DO IT!!!
     */
    fun justLogin(context: Context, account: MinecraftAccount) {
        //未找到匹配的ID
        fun roleNotFound() {
            TaskExecutors.runInUIThread {
                listener.onFailed(context.getString(R.string.other_login_role_not_found))
            }
        }

        login(context, object : LoginAccountListener {
            override fun onlyOneRole(authResult: AuthResult) {
                if (authResult.selectedProfile.id != account.profileId) {
                    roleNotFound()
                    return
                }
                writeAccount(account, authResult, authResult.selectedProfile.name, authResult.selectedProfile.id)
                TaskExecutors.runInUIThread {
                    listener.unLoading()
                    listener.onSuccess(account)
                }
            }

            override fun hasMultipleRoles(authResult: AuthResult) {
                authResult.availableProfiles.forEach { profile ->
                    if (profile.id == account.profileId) {
                        //匹配当前账号的ID时，那么这个角色就是这个账号
                        writeAccount(account, authResult, profile.name, profile.id)
                        TaskExecutors.runInUIThread {
                            listener.unLoading()
                            listener.onSuccess(account)
                        }
                        return
                    }
                }
                roleNotFound()
            }
        })
    }

    private fun refresh(context: Context, account: MinecraftAccount) {
        Task.runTask {
            OtherLoginApi.setBaseUrl(baseUrl)
            OtherLoginApi.refresh(context, account, true, object : OtherLoginApi.Listener {
                override fun onSuccess(authResult: AuthResult) {
                    account.accessToken = authResult.accessToken
                    account.updateOtherSkin()
                    TaskExecutors.runInUIThread {
                        listener.unLoading()
                        listener.onSuccess(account)
                    }
                }

                override fun onFailed(error: String) {
                    TaskExecutors.runInUIThread {
                        listener.unLoading()
                        listener.onFailed(error)
                    }
                }
            })
        }.beforeStart(TaskExecutors.getAndroidUI()) {
            listener.onLoading()
        }.onThrowable { e ->
            val message = "An exception was encountered while performing the refresh task."
            Logging.e("Other Login", message, e)
            TaskExecutors.runInUIThread {
                listener.onFailed(e.message ?: message)
            }
        }.execute()
    }

    interface OnLoginListener {
        fun onLoading()
        fun unLoading()
        fun onSuccess(account: MinecraftAccount)
        fun onFailed(error: String)
    }

    /**
     * 账号拥有的角色数量不同时，所做出的登陆决策
     */
    private interface LoginAccountListener {
        fun onlyOneRole(authResult: AuthResult)

        fun hasMultipleRoles(authResult: AuthResult)
    }
}