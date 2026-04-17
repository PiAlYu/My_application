package com.example.storechecklist.data.repository

import androidx.room.withTransaction
import com.example.storechecklist.data.local.AppDatabase
import com.example.storechecklist.data.local.ChecklistDao
import com.example.storechecklist.data.local.ChecklistEntity
import com.example.storechecklist.data.local.ChecklistItemEntity
import com.example.storechecklist.data.local.ItemProgressEntity
import com.example.storechecklist.data.local.ServerConfigStore
import com.example.storechecklist.data.remote.ServerApiFactory
import com.example.storechecklist.domain.ChecklistDetails
import com.example.storechecklist.domain.ChecklistItem
import com.example.storechecklist.domain.ChecklistSummary
import com.example.storechecklist.domain.SyncReport
import com.example.storechecklist.domain.UserChecklistMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChecklistRepository(
    private val database: AppDatabase,
    private val checklistDao: ChecklistDao,
    private val serverApiFactory: ServerApiFactory,
    private val serverConfigStore: ServerConfigStore,
) {
    fun getServerBaseUrl(): String {
        return serverConfigStore.getServerBaseUrl()
    }

    fun saveServerBaseUrl(rawUrl: String): Result<String> {
        return try {
            Result.success(serverConfigStore.saveServerBaseUrl(rawUrl))
        } catch (error: IllegalArgumentException) {
            Result.failure(error)
        }
    }

    fun observeChecklistSummaries(): Flow<List<ChecklistSummary>> {
        return checklistDao.observeChecklistSummaries().map { rows ->
            rows.map { row ->
                ChecklistSummary(
                    id = row.id,
                    title = row.title,
                    itemCount = row.itemCount,
                    isFromServer = row.isFromServer,
                )
            }
        }
    }

    fun observeChecklistDetails(checklistId: Long): Flow<ChecklistDetails?> {
        return combine(
            checklistDao.observeChecklistById(checklistId),
            checklistDao.observeChecklistItems(checklistId),
        ) { checklist, items ->
            checklist ?: return@combine null
            ChecklistDetails(
                id = checklist.id,
                title = checklist.title,
                isFromServer = checklist.isFromServer,
                items = items.map { row ->
                    ChecklistItem(
                        id = row.itemId,
                        name = row.name,
                        position = row.position,
                        hiddenInHideMode = row.hiddenInHideMode,
                        markedInMarkerMode = row.markedInMarkerMode,
                    )
                },
            )
        }
    }

    suspend fun createChecklist(title: String) = withContext(Dispatchers.IO) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return@withContext
        checklistDao.insertChecklist(
            ChecklistEntity(
                title = normalizedTitle,
                isFromServer = false,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun renameChecklist(checklistId: Long, title: String) = withContext(Dispatchers.IO) {
        val normalizedTitle = title.trim()
        if (normalizedTitle.isEmpty()) return@withContext
        checklistDao.updateChecklistTitle(
            checklistId = checklistId,
            title = normalizedTitle,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun deleteChecklist(checklistId: Long) = withContext(Dispatchers.IO) {
        checklistDao.deleteChecklistById(checklistId)
    }

    suspend fun addItems(checklistId: Long, names: List<String>) = withContext(Dispatchers.IO) {
        val normalized = names.map { it.trim() }.filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return@withContext
        val startPosition = checklistDao.getNextItemPosition(checklistId)
        val items = normalized.mapIndexed { index, name ->
            ChecklistItemEntity(
                checklistId = checklistId,
                name = name,
                position = startPosition + index,
            )
        }
        checklistDao.insertChecklistItems(items)
        val checklist = checklistDao.getChecklistById(checklistId) ?: return@withContext
        checklistDao.updateChecklist(
            checklist.copy(updatedAt = System.currentTimeMillis()),
        )
    }

    suspend fun deleteItems(itemIds: List<Long>) = withContext(Dispatchers.IO) {
        if (itemIds.isEmpty()) return@withContext
        checklistDao.deleteItemsByIds(itemIds)
    }

    suspend fun onUserTappedItem(
        checklistId: Long,
        itemId: Long,
        mode: UserChecklistMode,
    ) = withContext(Dispatchers.IO) {
        val current = checklistDao.getItemProgress(checklistId, itemId)
            ?: ItemProgressEntity(checklistId = checklistId, itemId = itemId)

        val updated = when (mode) {
            UserChecklistMode.HIDE_ON_TAP -> current.copy(hiddenInHideMode = true)
            UserChecklistMode.MARKER -> current.copy(markedInMarkerMode = !current.markedInMarkerMode)
        }
        checklistDao.upsertItemProgress(updated)
    }

    suspend fun resetChecklistProgress(checklistId: Long) = withContext(Dispatchers.IO) {
        checklistDao.clearProgressForChecklist(checklistId)
    }

    suspend fun syncFromServer(): SyncReport = withContext(Dispatchers.IO) {
        val baseUrl = serverConfigStore.getServerBaseUrl()
        val serverApi = serverApiFactory.create(baseUrl)
        val remoteChecklists = serverApi.getChecklists()
        var created = 0
        var updated = 0
        database.withTransaction {
            remoteChecklists.forEach { remote ->
                val existing = checklistDao.getChecklistByServerId(remote.id)
                if (existing == null) {
                    val checklistId = checklistDao.insertChecklist(
                        ChecklistEntity(
                            serverId = remote.id,
                            title = remote.title.trim(),
                            isFromServer = true,
                            updatedAt = remote.updatedAt ?: System.currentTimeMillis(),
                        ),
                    )
                    checklistDao.insertChecklistItems(
                        remote.items
                            .map { it.name.trim() }
                            .filter { it.isNotEmpty() }
                            .mapIndexed { index, name ->
                                ChecklistItemEntity(
                                    checklistId = checklistId,
                                    name = name,
                                    position = index,
                                )
                            },
                    )
                    created += 1
                } else {
                    checklistDao.updateChecklist(
                        existing.copy(
                            title = remote.title.trim(),
                            isFromServer = true,
                            updatedAt = remote.updatedAt ?: System.currentTimeMillis(),
                        ),
                    )
                    checklistDao.replaceChecklistItems(
                        checklistId = existing.id,
                        items = remote.items.map { it.name },
                    )
                    updated += 1
                }
            }
        }
        SyncReport(created = created, updated = updated)
    }
}
