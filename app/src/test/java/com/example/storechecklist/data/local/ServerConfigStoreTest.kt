package com.example.storechecklist.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ServerConfigStoreTest {
    @Test
    fun normalizesPublicHttpsUrl() {
        assertEquals(
            "https://checklists.example.ru/api/",
            ServerConfigStore.normalizeBaseUrl("https://checklists.example.ru/api"),
        )
    }

    @Test
    fun allowsHttpInsideLocalNetwork() {
        assertEquals(
            "http://192.168.1.50:8080/api/",
            ServerConfigStore.normalizeBaseUrl("http://192.168.1.50:8080/api"),
        )
    }

    @Test
    fun rejectsPublicHttpUrl() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ServerConfigStore.normalizeBaseUrl("http://checklists.example.ru/api")
        }
        assertEquals(
            "Для внешнего адреса используйте https://. HTTP оставлен только для localhost и локальной сети.",
            error.message,
        )
    }
}
