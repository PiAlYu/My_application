package com.example.storechecklist.domain

enum class UserChecklistMode {
    HIDE_ON_TAP,
    MARKER,
}

data class ChecklistSummary(
    val id: Long,
    val title: String,
    val itemCount: Int,
    val isFromServer: Boolean,
)

data class ChecklistItem(
    val id: Long,
    val name: String,
    val position: Int,
    val hiddenInHideMode: Boolean,
    val markedInMarkerMode: Boolean,
)

data class ChecklistDetails(
    val id: Long,
    val title: String,
    val isFromServer: Boolean,
    val items: List<ChecklistItem>,
)

data class SyncReport(
    val created: Int,
    val updated: Int,
)

