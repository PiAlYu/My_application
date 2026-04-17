package com.example.storechecklist.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklists",
    indices = [Index(value = ["serverId"], unique = true)],
)
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val serverId: String? = null,
    val title: String,
    val isFromServer: Boolean,
    val updatedAt: Long,
)

