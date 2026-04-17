package com.example.storechecklist.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistDao {
    @Query(
        """
        SELECT c.id, c.title, c.isFromServer, COUNT(i.id) AS itemCount
        FROM checklists c
        LEFT JOIN checklist_items i ON i.checklistId = c.id
        GROUP BY c.id
        ORDER BY c.updatedAt DESC
        """,
    )
    fun observeChecklistSummaries(): Flow<List<ChecklistSummaryRow>>

    @Query("SELECT * FROM checklists WHERE id = :checklistId LIMIT 1")
    fun observeChecklistById(checklistId: Long): Flow<ChecklistEntity?>

    @Query(
        """
        SELECT
            ci.id AS itemId,
            ci.name AS name,
            ci.position AS position,
            COALESCE(ip.hiddenInHideMode, 0) AS hiddenInHideMode,
            COALESCE(ip.markedInMarkerMode, 0) AS markedInMarkerMode
        FROM checklist_items ci
        LEFT JOIN item_progress ip
            ON ip.itemId = ci.id
            AND ip.checklistId = :checklistId
        WHERE ci.checklistId = :checklistId
        ORDER BY ci.position ASC, ci.id ASC
        """,
    )
    fun observeChecklistItems(checklistId: Long): Flow<List<ChecklistItemStateRow>>

    @Insert
    suspend fun insertChecklist(checklist: ChecklistEntity): Long

    @Update
    suspend fun updateChecklist(checklist: ChecklistEntity)

    @Query("DELETE FROM checklists WHERE id = :checklistId")
    suspend fun deleteChecklistById(checklistId: Long)

    @Query("SELECT * FROM checklists WHERE id = :checklistId LIMIT 1")
    suspend fun getChecklistById(checklistId: Long): ChecklistEntity?

    @Query("SELECT * FROM checklists WHERE serverId = :serverId LIMIT 1")
    suspend fun getChecklistByServerId(serverId: String): ChecklistEntity?

    @Query("UPDATE checklists SET title = :title, updatedAt = :updatedAt WHERE id = :checklistId")
    suspend fun updateChecklistTitle(checklistId: Long, title: String, updatedAt: Long)

    @Insert
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)

    @Query("DELETE FROM checklist_items WHERE checklistId = :checklistId")
    suspend fun deleteItemsForChecklist(checklistId: Long)

    @Query("DELETE FROM checklist_items WHERE id IN (:itemIds)")
    suspend fun deleteItemsByIds(itemIds: List<Long>)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM checklist_items WHERE checklistId = :checklistId")
    suspend fun getNextItemPosition(checklistId: Long): Int

    @Query("SELECT * FROM item_progress WHERE checklistId = :checklistId AND itemId = :itemId LIMIT 1")
    suspend fun getItemProgress(checklistId: Long, itemId: Long): ItemProgressEntity?

    @Upsert
    suspend fun upsertItemProgress(progress: ItemProgressEntity)

    @Query("DELETE FROM item_progress WHERE checklistId = :checklistId")
    suspend fun clearProgressForChecklist(checklistId: Long)

    @Transaction
    suspend fun replaceChecklistItems(checklistId: Long, items: List<String>) {
        deleteItemsForChecklist(checklistId)
        val normalized = items
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return
        val entities = normalized.mapIndexed { index, itemName ->
            ChecklistItemEntity(
                checklistId = checklistId,
                name = itemName,
                position = index,
            )
        }
        insertChecklistItems(entities)
    }
}

