package com.example.storechecklist.data.local

import android.content.Context
import com.example.storechecklist.BuildConfig
import java.net.URI

class ServerConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getServerBaseUrl(): String {
        val stored = preferences.getString(KEY_SERVER_BASE_URL, null)
        return stored ?: BuildConfig.SERVER_BASE_URL
    }

    fun saveServerBaseUrl(rawUrl: String): String {
        val normalized = normalizeBaseUrl(rawUrl)
        preferences.edit()
            .putString(KEY_SERVER_BASE_URL, normalized)
            .apply()
        return normalized
    }

    companion object {
        private const val PREFS_NAME = "server_config"
        private const val KEY_SERVER_BASE_URL = "server_base_url"

        fun normalizeBaseUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            require(trimmed.isNotEmpty()) { "URL сервера не может быть пустым." }
            require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                "URL должен начинаться с http:// или https://"
            }
            val withSlash = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            val parsed = URI(withSlash)
            require(!parsed.host.isNullOrBlank()) { "В URL отсутствует host." }
            return withSlash
        }
    }
}
