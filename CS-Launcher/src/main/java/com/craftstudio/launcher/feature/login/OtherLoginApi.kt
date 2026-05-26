package com.craftstudio.launcher.feature.login

import android.content.Context
import com.google.gson.Gson
import com.craftstudio.launcher.R
import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.path.UrlManager
import com.craftstudio.launcher.utils.path.UrlManager.Companion.createRequestBuilder
import com.craftstudio.launcher.utils.stringutils.StringUtilsKt
import com.craftstudio.launcher.Tools
import com.craftstudio.launcher.value.MinecraftAccount
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.Objects
import java.util.UUID

object OtherLoginApi {
    private var client: OkHttpClient = UrlManager.createOkHttpClient()
    private var baseUrl: String? = null

    fun setBaseUrl(baseUrl: String) {
        var url = baseUrl
        if (baseUrl.endsWith("/")) {
            url = baseUrl.substring(0, baseUrl.length - 1)
        }
        this.baseUrl = url
    }

    @Throws(IOException::class)
    fun login(context: Context, userName: String?, password: String?, listener: Listener) {
        if (Objects.isNull(baseUrl)) {
            listener.onFailed(context.getString(R.string.other_login_baseurl_not_set))
            return
        }
        val agent = AuthRequest.Agent().apply {
            this.name = "Minecraft"
            this.version = 1
        }
        val authRequest = AuthRequest().apply {
            this.username = userName
            this.password = password
            this.agent = agent
            this.requestUser = true
            this.clientToken = UUID.randomUUID().toString().lowercase()
        }
        val data = Gson().toJson(authRequest)
        callLogin(data, "/authserver/authenticate", listener)
    }

    @Throws(IOException::class)
    fun refresh(context: Context, account: MinecraftAccount, select: Boolean, listener: Listener) {
        if (Objects.isNull(baseUrl)) {
            listener.onFailed(context.getString(R.string.other_login_baseurl_not_set))
            return
        }
        val refresh = Refresh().apply {
            this.clientToken = account.clientToken
            this.accessToken = account.accessToken
        }
        if (select) {
            val selectedProfile = Refresh.SelectedProfile().apply {
                this.name = account.username
                this.id = account.profileId
            }
            refresh.selectedProfile = selectedProfile
        }
        val data = Gson().toJson(refresh)
        callLogin(data, "/authserver/refresh", listener)
    }

    private fun callLogin(data: String, url: String, listener: Listener) {
        val body = data.toRequestBody("application/json".toMediaTypeOrNull())
        val call = client.newCall(createRequestBuilder(baseUrl + url, body).build())

        call.execute().use { response ->
            val res = response.body?.string()
            if (response.code == 200) {
                val result = Gson().fromJson(res, AuthResult::class.java)
                listener.onSuccess(result)
            } else {
                var errorMessage: String = res ?: "null"
                runCatching parseMessage@{
                    if (errorMessage == "null") return@parseMessage

                    val jsonObject = JSONObject(errorMessage)
                    errorMessage = if (jsonObject.has("errorMessage")) {
                        jsonObject.getString("errorMessage")
                    } else if (jsonObject.has("message")) {
                        jsonObject.getString("message")
                    } else {
                        Logging.e("Other Login", "The error message returned by the server could not be retrieved.")
                        return@parseMessage
                    }

                    if (errorMessage.contains("\\u"))
                        errorMessage = StringUtilsKt.decodeUnicode(errorMessage.replace("\\\\u", "\\u"))
                }.onFailure { e -> Logging.e("Other Login", Tools.printToString(e)) }
                listener.onFailed("(${response.code}) $errorMessage")
            }
        }
    }

    fun getServeInfo(context: Context, url: String): String? {
        val call = client.newCall(createRequestBuilder(url).get().build())
        runCatching {
            call.execute().use { response ->
                val res = response.body?.string()
                if (response.code == 200) return res
            }
        }.onFailure { e -> Tools.showError(context, e) }
        return null
    }

    interface Listener {
        fun onSuccess(authResult: AuthResult)
        fun onFailed(error: String)
    }
}