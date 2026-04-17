package com.example.storechecklist.data.local

data class ChecklistSummaryRow(
    val id: Long,
    val title: String,
    val isFromServer: Boolean,
    val itemCount: Int,
)

data class ChecklistItemStateRow(
    val itemId: Long,
    val name: String,
    val position: Int,
    val hiddenInHideMode: Boolean,
    val markedInMarkerMode: Boolean,
)

