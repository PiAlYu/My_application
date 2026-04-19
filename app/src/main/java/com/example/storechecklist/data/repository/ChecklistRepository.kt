package com.example.storechecklist.data.repository

import androidx.room.withTransaction
import com.example.storechecklist.data.local.AppDatabase
import com.example.storechecklist.data.local.ChecklistDao
import com.example.storechecklist.data.local.ChecklistEntity
import com.example.storechecklist.data.local.ChecklistItemEntity
import com.example.storechecklist.data.local.ChecklistWithItems
import com.example.storechecklist.data.local.ItemProgressEntity
import com.example.storechecklist.data.local.ServerConfigStore
import com.example.storechecklist.data.remote.RemoteChecklistDto
import com.example.storechecklist.data.remote.RemoteChecklistItemDto
import com.example.storechecklist.data.remote.ServerApiFactory
import com.example.storechecklist.domain.ChecklistDetails
import com.example.storechecklist.domain.ChecklistItem
import com.example.storechecklist.domain.ChecklistSummary
import com.example.storechecklist.domain.ServerConnectionSettings
import com.example.storechecklist.domain.SyncReport
import com.example.storechecklist.domain.UserChecklistMode
import java.util.UUID
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
    fun getServerConnectionSettings(): ServerConnectionSettings {
        return serverConfigStore.getSettings()
    }

    fun saveServerConnectionSettings(
        rawUrl: String,
        rawReadToken: String,
        rawWriteToken: String,
    ): Result<ServerConnectionSettings> {
        return try {
            Result.success(serverConfigStore.saveSettings(rawUrl, rawReadToken, rawWriteToken))
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
        val normalized = normalizeItemNames(names)
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

    suspend fun importMissingFromServer(): SyncReport = withContext(Dispatchers.IO) {
        val serverSettings = serverConfigStore.getSettings()
        val serverApi = serverApiFactory.create(
            baseUrl = serverSettings.baseUrl,
            authToken = serverSettings.readToken,
        )
        val remoteChecklists = serverApi.getChecklists().map(::normalizeRemoteChecklist)
        val localChecklists = checklistDao.getAllChecklistsWithItems()
        val knownServerIds = localChecklists.mapNotNull { it.checklist.serverId }.toMutableSet()
        val knownTitles = localChecklists
            .map { normalizeTitleKey(it.checklist.title) }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        var addedToLocal = 0

        database.withTransaction {
            remoteChecklists.forEach { remote ->
                val titleKey = normalizeTitleKey(remote.title)
                if (titleKey.isEmpty()) return@forEach
                if (knownServerIds.contains(remote.id) || knownTitles.contains(titleKey)) {
                    return@forEach
                }

                val checklistId = checklistDao.insertChecklist(
                    ChecklistEntity(
                        serverId = remote.id,
                        title = remote.title,
                        isFromServer = true,
                        updatedAt = remote.updatedAt ?: System.currentTimeMillis(),
                    ),
                )
                insertChecklistItems(
                    checklistId = checklistId,
                    itemNames = remote.items.map { it.name },
                )
                knownServerIds += remote.id
                knownTitles += titleKey
                addedToLocal += 1
            }
        }

        SyncReport(
            addedToLocal = addedToLocal,
            addedToServer = 0,
            updatedOnServer = 0,
        )
    }

    suspend fun replaceServerWithLocal(): SyncReport = withContext(Dispatchers.IO) {
        val serverSettings = serverConfigStore.getSettings()
        val serverApi = serverApiFactory.create(
            baseUrl = serverSettings.baseUrl,
            authToken = serverSettings.resolveWriteToken(),
        )
        val localChecklists = checklistDao.getAllChecklistsWithItems()
        val now = System.currentTimeMillis()

        val exportPlan = localChecklists.mapNotNull { localChecklist ->
            buildExportChecklist(localChecklist, now)
        }

        val syncedRemoteChecklists = serverApi.replaceChecklists(exportPlan.map { it.remoteChecklist })
        val syncedRemoteById = syncedRemoteChecklists.associateBy { it.id }

        database.withTransaction {
            exportPlan.forEach { exportChecklist ->
                val existing = checklistDao.getChecklistById(exportChecklist.localChecklistId) ?: return@forEach
                val syncedRemote = syncedRemoteById[exportChecklist.remoteChecklist.id]
                val updatedChecklist = existing.copy(
                    serverId = exportChecklist.remoteChecklist.id,
                    isFromServer = true,
                    updatedAt = syncedRemote?.updatedAt ?: maxOf(existing.updatedAt, now),
                )
                if (updatedChecklist != existing) {
                    checklistDao.updateChecklist(updatedChecklist)
                }
            }
        }

        SyncReport(
            addedToLocal = 0,
            addedToServer = exportPlan.size,
            updatedOnServer = 0,
        )
    }

    private fun buildExportChecklist(
        localChecklist: ChecklistWithItems,
        now: Long,
    ): ExportChecklist? {
        val normalizedTitle = localChecklist.checklist.title.trim()
        if (normalizedTitle.isEmpty()) return null

        val serverId = localChecklist.checklist.serverId
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        return ExportChecklist(
            localChecklistId = localChecklist.checklist.id,
            remoteChecklist = RemoteChecklistDto(
                id = serverId,
                title = normalizedTitle,
                updatedAt = maxOf(now, localChecklist.checklist.updatedAt),
                items = normalizeItemNames(localChecklist.items.map { it.name })
                    .map(::RemoteChecklistItemDto),
            ),
        )
    }

    private suspend fun insertChecklistItems(
        checklistId: Long,
        itemNames: List<String>,
    ) {
        val normalizedItems = normalizeItemNames(itemNames)
        if (normalizedItems.isEmpty()) return
        checklistDao.insertChecklistItems(
            normalizedItems.mapIndexed { index, name ->
                ChecklistItemEntity(
                    checklistId = checklistId,
                    name = name,
                    position = index,
                )
            },
        )
    }

    private fun normalizeRemoteChecklist(remoteChecklist: RemoteChecklistDto): RemoteChecklistDto {
        return RemoteChecklistDto(
            id = remoteChecklist.id,
            title = remoteChecklist.title.trim(),
            updatedAt = remoteChecklist.updatedAt ?: System.currentTimeMillis(),
            items = normalizeItemNames(remoteChecklist.items.map { it.name }).map(::RemoteChecklistItemDto),
        )
    }

    private fun normalizeItemNames(rawItems: List<String>): List<String> {
        return rawItems
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun normalizeTitleKey(rawTitle: String): String {
        return rawTitle.trim()
    }

    private data class ExportChecklist(
        val localChecklistId: Long,
        val remoteChecklist: RemoteChecklistDto,
    )
}
