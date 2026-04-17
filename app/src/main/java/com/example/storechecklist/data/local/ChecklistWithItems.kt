package com.example.storechecklist.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class ChecklistWithItems(
    @Embedded val checklist: ChecklistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checklistId",
    )
    val items: List<ChecklistItemEntity>,
)
