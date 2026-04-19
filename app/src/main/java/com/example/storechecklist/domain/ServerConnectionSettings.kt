package com.example.storechecklist.domain

data class ServerConnectionSettings(
    val baseUrl: String = "",
    val readToken: String = "",
    val writeToken: String = "",
) {
    fun resolveWriteToken(): String {
        return writeToken.ifBlank { readToken }
    }
}
