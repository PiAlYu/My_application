package com.example.storechecklist.data.remote

data class RemoteChecklistDto(
    val id: String,
    val title: String,
    val updatedAt: Long? = null,
    val items: List<RemoteChecklistItemDto>,
)

data class RemoteChecklistItemDto(
    val name: String,
)

