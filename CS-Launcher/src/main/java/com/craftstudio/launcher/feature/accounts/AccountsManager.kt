package com.craftstudio.launcher.feature.accounts

import android.content.Context
import android.widget.Toast
import com.craftstudio.launcher.R
import com.craftstudio.launcher.context.ContextExecutor
import com.craftstudio.launcher.context.ContextExecutor.Companion.showToast
import com.craftstudio.launcher.event.single.AccountUpdateEvent
import com.craftstudio.launcher.feature.accounts.AccountUtils.Companion.isMicrosoftAccount
import com.craftstudio.launcher.feature.accounts.AccountUtils.Companion.isNoLoginRequired
import com.craftstudio.launcher.feature.accounts.AccountUtils.Companion.isOtherLoginAccount
import com.craftstudio.launcher.feature.accounts.AccountUtils.Companion.microsoftLogin
import com.craftstudio.launcher.feature.accounts.AccountUtils.Companion.otherLogin
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.setting.AllSettings
import com.craftstudio.launcher.task.TaskExecutors
import com.craftstudio.launcher.ui.dialog.TipDialog
import com.craftstudio.launcher.utils.path.PathManager
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.authenticator.listener.DoneListener
import com.craftstudio.launcher.authenticator.listener.ErrorListener
import com.craftstudio.launcher.authenticator.microsoft.PresentedException
import com.craftstudio.launcher.value.MinecraftAccount
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

object AccountsManager {
    private val accountsLock = Any()
    private val accounts = CopyOnWriteArrayList<MinecraftAccount>()

    val doneListener by lazy {
        DoneListener { account ->
            TaskExecutors.runInUIThread {
                showToast(R.string.account_login_done, Toast.LENGTH_SHORT)
            }

            synchronized(accountsLock) {
                if (accounts.any { it.uniqueUUID == account.uniqueUUID }) {
                    EventBus.getDefault().post(AccountUpdateEvent())
                    return@DoneListener
                }

                reloadInternal()
                if (accounts.isEmpty()) currentAccount = account
                else EventBus.getDefault().post(AccountUpdateEvent())
            }
        }
    }

    val errorListener by lazy {
        ErrorListener { error ->
            ContextExecutor.executeTaskWithAllContext { context ->
                when (error) {
                    is PresentedException -> handlePresentedException(context, error)
                    else -> Tools.showError(context, error)
                }
            }
        }
    }

    @JvmOverloads
    fun performLogin(
        context: Context,
        minecraftAccount: MinecraftAccount,
        doneListener1: DoneListener = doneListener,
        errorListener1: ErrorListener = errorListener
    ) {
        when {
            isNoLoginRequired(minecraftAccount) -> doneListener1.onLoginDone(minecraftAccount)
            isOtherLoginAccount(minecraftAccount) -> otherLogin(context, minecraftAccount, doneListener1, errorListener1)
            isMicrosoftAccount(minecraftAccount) -> microsoftLogin(context, minecraftAccount, doneListener1, errorListener1)
        }
    }

    fun reload() {
        synchronized(accountsLock) {
            reloadInternal()
        }
    }

    var currentAccount: MinecraftAccount?
        get() = synchronized(accountsLock) {
            MinecraftAccount.loadFromUniqueUUID(AllSettings.currentAccount.getValue())
                ?: accounts.firstOrNull()?.also { currentAccount = it }
        }
        set(value) {
            requireNotNull(value) { "Account cannot be null" }
            AllSettings.currentAccount.put(value.uniqueUUID).save()
            EventBus.getDefault().post(AccountUpdateEvent())
        }

    val allAccounts: List<MinecraftAccount>
        get() = synchronized(accountsLock) { accounts.toList() }

    fun hasMicrosoftAccount(): Boolean = synchronized(accountsLock) {
        accounts.any(::isMicrosoftAccount)
    }

    private fun reloadInternal() {
        accounts.clear()
        File(PathManager.DIR_ACCOUNT_NEW).takeIf { it.exists() && it.isDirectory }
            ?.listFiles()
            ?.forEach { file ->
                try {
                    MinecraftAccount.parse(Tools.read(file))?.let {
                        if (!accounts.contains(it)) accounts.add(it)
                    }
                } catch (e: IOException) {
                    Logging.e("AccountsManager", "Failed to read account file: ${file.name}", e)
                } catch (e: Exception) {
                    Logging.e("AccountsManager", "Invalid account format in file: ${file.name}", e)
                }
            }
        Logging.i("AccountsManager", "Reloaded ${accounts.size} accounts")
    }

    private fun handlePresentedException(activity: Context, exception: PresentedException) {
        exception.cause?.let {
            Tools.showError(activity, exception.toString(activity), it)
        } ?: run {
            TipDialog.Builder(activity)
                .setTitle(R.string.generic_error)
                .setMessage(exception.toString(activity))
                .setWarning()
                .setConfirm(android.R.string.ok)
                .setShowCancel(false)
                .showDialog()
        }
    }
}