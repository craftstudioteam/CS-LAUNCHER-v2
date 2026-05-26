package com.craftstudio.launcher.ai

import com.craftstudio.launcher.feature.log.Logging
import com.craftstudio.launcher.utils.path.UrlManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

class CrashAIAnalyzer {

    companion object {
        private const val TAG = "CrashAIAnalyzer"
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val API_KEY = "Bearer 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"

        private const val SYSTEM_PROMPT =
            "You are Craft Studio AI, an expert Minecraft developer. " +
            "Read the following crash log and explain exactly how to fix it in 1 or 2 simple sentences. " +
            "Do not use jargon. Just tell the user what mod to remove, or if they need more RAM, or wrong Java version."

        private val MODELS = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant"
        )

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    interface Callback {
        fun onSuccess(analysis: String)
        fun onFailure(error: String)
    }

    /**
     * Analyze a crash log file using Groq API with model fallback.
     * Runs on background thread — callback is invoked on the same thread.
     */
    fun analyze(logFile: File, callback: Callback) {
        val logContent = readLastLines(logFile, 100)
        if (logContent.isBlank()) {
            callback.onFailure("Could not read crash log file.")
            return
        }
        analyzeText(logContent, callback)
    }

    /**
     * Analyze raw crash log text with model fallback.
     */
    fun analyzeText(logText: String, callback: Callback) {
        val client = UrlManager.createOkHttpClient()
        val trimmedLog = logText.take(6000)

        for (model in MODELS) {
            try {
                val requestBody = buildRequestBody(model, trimmedLog)
                val request = Request.Builder()
                    .url(GROQ_API_URL)
                    .addHeader("Authorization", API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val analysis = parseResponse(body)
                    if (analysis != null) {
                        callback.onSuccess(analysis)
                        return
                    }
                } else {
                    Logging.w(TAG, "Model $model failed with HTTP ${response.code}")
                }
            } catch (e: IOException) {
                Logging.w(TAG, "Model $model failed: ${e.message}")
            }
        }

        callback.onFailure("AI analysis is currently unavailable. Please check the crash log manually.")
    }

    private fun buildRequestBody(model: String, logContent: String): okhttp3.RequestBody {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", "Here is the crash log:\n\n$logContent")
            })
        }

        val json = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("max_tokens", 256)
            put("temperature", 0.3)
        }

        return json.toString().toRequestBody(JSON_MEDIA_TYPE)
    }

    private fun parseResponse(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            val choices = json.getJSONArray("choices")
            if (choices.length() > 0) {
                choices.getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                    .trim()
            } else null
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to parse AI response", e)
            null
        }
    }

    private fun readLastLines(file: File, maxLines: Int): String {
        if (!file.exists()) return ""
        return try {
            val lines = file.readLines()
            val tail = if (lines.size > maxLines) lines.takeLast(maxLines) else lines
            tail.joinToString("\n")
        } catch (e: Exception) {
            Logging.e(TAG, "Failed to read log file: ${file.absolutePath}", e)
            ""
        }
    }
}
