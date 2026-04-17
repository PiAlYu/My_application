package com.example.storechecklist.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "item_progress",
    primaryKeys = ["checklistId", "itemId"],
    foreignKeys = [
        ForeignKey(
            entity = ChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["checklistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("checklistId"), Index("itemId")],
)
data class ItemProgressEntity(
    val checklistId: Long,
    val itemId: Long,
    val hiddenInHideMode: Boolean = false,
    val markedInMarkerMode: Boolean = false,
)
