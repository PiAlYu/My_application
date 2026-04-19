package com.example.storechecklist.data.local

import android.content.Context
import com.example.storechecklist.BuildConfig
import com.example.storechecklist.domain.ServerConnectionSettings
import java.net.URI

class ServerConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSettings(): ServerConnectionSettings {
        val storedBaseUrl = preferences.getString(KEY_SERVER_BASE_URL, null)
        val storedReadToken = preferences.getString(KEY_SERVER_READ_TOKEN, null)
        val storedWriteToken = preferences.getString(KEY_SERVER_WRITE_TOKEN, null)
        return ServerConnectionSettings(
            baseUrl = storedBaseUrl ?: BuildConfig.SERVER_BASE_URL,
            readToken = storedReadToken ?: BuildConfig.SERVER_READ_TOKEN,
            writeToken = storedWriteToken ?: BuildConfig.SERVER_WRITE_TOKEN,
        )
    }

    fun saveSettings(
        rawUrl: String,
        rawReadToken: String,
        rawWriteToken: String,
    ): ServerConnectionSettings {
        val normalizedUrl = normalizeBaseUrl(rawUrl)
        val normalizedReadToken = normalizeToken(rawReadToken)
        val normalizedWriteToken = normalizeToken(rawWriteToken)
        preferences.edit()
            .putString(KEY_SERVER_BASE_URL, normalizedUrl)
            .putString(KEY_SERVER_READ_TOKEN, normalizedReadToken)
            .putString(KEY_SERVER_WRITE_TOKEN, normalizedWriteToken)
            .apply()
        return ServerConnectionSettings(
            baseUrl = normalizedUrl,
            readToken = normalizedReadToken,
            writeToken = normalizedWriteToken,
        )
    }

    companion object {
        private const val PREFS_NAME = "server_config"
        private const val KEY_SERVER_BASE_URL = "server_base_url"
        private const val KEY_SERVER_READ_TOKEN = "server_read_token"
        private const val KEY_SERVER_WRITE_TOKEN = "server_write_token"

        private val privateIpv4Pattern = Regex(
            pattern = "^(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|192\\.168\\.\\d{1,3}\\.\\d{1,3}|172\\.(1[6-9]|2\\d|3[0-1])\\.\\d{1,3}\\.\\d{1,3}|127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|169\\.254\\.\\d{1,3}\\.\\d{1,3})$",
        )

        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            require(trimmed.isNotEmpty()) { "URL сервера не может быть пустым." }

            val withSlash = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            val parsed = try {
                URI.create(withSlash)
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Неверный формат URL.", error)
            }

            val scheme = parsed.scheme?.lowercase()
            require(scheme == "http" || scheme == "https") {
                "URL должен начинаться с http:// или https://"
            }

            val host = parsed.host?.trim()
            require(!host.isNullOrBlank()) { "В URL отсутствует host." }

            if (scheme == "http" && requiresHttps(host)) {
                throw IllegalArgumentException(
                    "Для внешнего адреса используйте https://. HTTP оставлен только для localhost и локальной сети.",
                )
            }

            return withSlash
        }

        private fun normalizeToken(rawToken: String): String {
            return rawToken.trim()
        }

        private fun requiresHttps(host: String): Boolean {
            val normalized = host.removePrefix("[").removeSuffix("]").lowercase()
            if (normalized == "localhost") return false
            if (normalized == "::1") return false
            if (normalized.startsWith("fe80:")) return false
            if (normalized.startsWith("fc") || normalized.startsWith("fd")) return false
            if (normalized.endsWith(".local") || normalized.endsWith(".lan")) return false
            if (!normalized.contains(".")) return false
            if (privateIpv4Pattern.matches(normalized)) return false
            return true
        }
    }
}
