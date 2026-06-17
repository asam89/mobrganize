package com.lattice.launcher.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class AnthropicMessage(val role: String, val content: String)

@Serializable
data class AnthropicRequest(
    val model: String = "claude-sonnet-4-20250514",
    val max_tokens: Int = 4096,
    val messages: List<AnthropicMessage>,
    val system: String? = null
)

class AnthropicClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun sendMessage(
        userPrompt: String,
        systemPrompt: String? = null
    ): String? {
        val body = AnthropicRequest(
            messages = listOf(AnthropicMessage(role = "user", content = userPrompt)),
            system = systemPrompt
        )
        val jsonBody = json.encodeToString(body)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string() ?: return null
                val root = json.parseToJsonElement(responseBody).jsonObject
                root["content"]?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("text")?.jsonPrimitive?.content
            }
        } catch (_: Exception) {
            null
        }
    }
}
