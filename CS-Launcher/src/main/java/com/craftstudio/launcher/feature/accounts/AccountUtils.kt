package com.craftstudio.launcher.feature.accounts

import android.content.Context
import com.kdt.mcgui.ProgressLayout

// ✅ FIX: Purana import hata kar tera naya Craft Studio R class lagana hai.
// (Maine yahan example ke liye com.craftstudio.launcher.R likha hai, ise apne package se match kar lena)
import com.craftstudio.launcher.R 

import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.task.Task
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.authenticator.listener.DoneListener
import com.craftstudio.launcher.authenticator.listener.ErrorListener
import com.craftstudio.launcher.authenticator.microsoft.MicrosoftBackgroundLogin
import com.craftstudio.launcher.value.MinecraftAccount
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.Objects

class AccountUtils {
    companion object {
        @JvmStatic
        fun microsoftLogin(context: Context, account: MinecraftAccount, doneListener: DoneListener, errorListener: ErrorListener) {
            MicrosoftBackgroundLogin(true, account.msaRefreshToken)
                .performLogin(context, account, doneListener, errorListener)
        }

        @JvmStatic
        fun otherLogin(context: Context, account: MinecraftAccount, doneListener: DoneListener, errorListener: ErrorListener) {
            fun clearProgress() = ProgressLayout.clearProgress(ProgressLayout.LOGIN_ACCOUNT)

            Task.runTask {
                OtherLoginHelper(account.otherBaseUrl, account.accountType, account.otherAccount, account.otherPassword,
                    object : OtherLoginHelper.OnLoginListener {
                        override fun onLoading() {
                            ProgressLayout.setProgress(ProgressLayout.LOGIN_ACCOUNT, 0, R.string.account_login_start)
                        }

                        override fun unLoading() {}

                        override fun onSuccess(account: MinecraftAccount) {
                            account.save()
                            clearProgress()
                            doneListener.onLoginDone(account)
                        }

                        override fun onFailed(error: String) {
                            clearProgress()
                            errorListener.onLoginError(RuntimeException(error))
                            ProgressLayout.clearProgress(ProgressLayout.LOGIN_ACCOUNT)
                        }
                    }).justLogin(context, account)
            }.onThrowable { t -> errorListener.onLoginError(RuntimeException(t.message)) }.execute()
        }

        @JvmStatic
        fun isOtherLoginAccount(account: MinecraftAccount): Boolean {
            return !Objects.isNull(account.otherBaseUrl) && account.otherBaseUrl != "0"
        }

        @JvmStatic
        fun isMicrosoftAccount(account: MinecraftAccount): Boolean {
            return account.accountType == AccountType.MICROSOFT.type
        }

        @JvmStatic
        fun isNoLoginRequired(account: MinecraftAccount?): Boolean {
            return account == null || account.accountType == AccountType.LOCAL.type
        }

        @JvmStatic
        fun getAccountTypeName(context: Context, account: MinecraftAccount): String {
            return if (isMicrosoftAccount(account)) {
                context.getString(R.string.account_microsoft_account)
            } else if (isOtherLoginAccount(account)) {
                account.accountType
            } else {
                context.getString(R.string.account_local_account)
            }
        }

        fun tryGetFullServerUrl(baseUrl: String): String {
            fun String.addSlashIfMissing(): String {
                if (!endsWith("/")) return "$this/"
                return this
            }

            var url = addHttpsIfMissing(baseUrl)
            runCatching {
                var conn = URL(url).openConnection() as HttpURLConnection
                conn.getHeaderField("x-authlib-injector-api-location")?.let { ali ->
                    val absoluteAli = URL(conn.url, ali)
                    url = url.addSlashIfMissing()
                    val absoluteUrl = absoluteAli.toString().addSlashIfMissing()
                    if (url != absoluteUrl) {
                        conn.disconnect()
                        url = absoluteUrl
                        conn = absoluteAli.openConnection() as HttpURLConnection
                    }
                }

                return url.addSlashIfMissing()
            }.getOrElse { e ->
                Logging.e("getFullServerUrl", Tools.printToString(e))
            }
            return baseUrl
        }

        private fun addHttpsIfMissing(baseUrl: String): String {
            return if (!baseUrl.startsWith("http://", true) && !baseUrl.startsWith("https://")) {
                "https://$baseUrl".lowercase(Locale.ROOT)
            } else baseUrl.lowercase(Locale.ROOT)
        }
    }
}
