package com.example.storechecklist.data.repository

import androidx.room.withTransaction
import com.example.storechecklist.data.local.AppDatabase
import com.example.storechecklist.data.local.ChecklistDao
import com.example.storechecklist.data.local.ChecklistEntity
import com.example.storechecklist.data.local.ChecklistItemEntity
import com.example.storechecklist.data.local.ItemProgressEntity
import com.example.storechecklist.data.local.ServerConfigStore
import com.example.storechecklist.data.local.ChecklistWithItems
import com.example.storechecklist.data.remote.RemoteChecklistDto
import com.example.storechecklist.data.remote.RemoteChecklistItemDto
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
import java.util.UUID

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

    suspend fun syncWithServer(): SyncReport = withContext(Dispatchers.IO) {
        val baseUrl = serverConfigStore.getServerBaseUrl()
        val serverApi = serverApiFactory.create(baseUrl)
        val remoteChecklists = serverApi.getChecklists()
        val localChecklists = checklistDao.getAllChecklistsWithItems()
        val mergePlan = buildMergePlan(localChecklists, remoteChecklists)
        val mergedRemoteChecklists = serverApi.replaceChecklists(mergePlan.remoteChecklists)
        val mergedRemoteById = mergedRemoteChecklists.associateBy { it.id }

        database.withTransaction {
            mergePlan.localAssignments.forEach { assignment ->
                val mergedRemote = mergedRemoteById[assignment.serverId] ?: return@forEach
                val existing = checklistDao.getChecklistById(assignment.localChecklistId) ?: return@forEach
                val updatedChecklist = existing.copy(
                    serverId = assignment.serverId,
                    title = mergedRemote.title.trim(),
                    isFromServer = true,
                    updatedAt = mergedRemote.updatedAt ?: existing.updatedAt,
                )
                if (updatedChecklist != existing) {
                    checklistDao.updateChecklist(updatedChecklist)
                }
            }

            mergePlan.remoteOnlyServerIds.forEach { serverId ->
                if (checklistDao.getChecklistByServerId(serverId) != null) return@forEach
                val remote = mergedRemoteById[serverId] ?: return@forEach
                val checklistId = checklistDao.insertChecklist(
                    ChecklistEntity(
                        serverId = remote.id,
                        title = remote.title.trim(),
                        isFromServer = true,
                        updatedAt = remote.updatedAt ?: System.currentTimeMillis(),
                    ),
                )
                val items = normalizeItemNames(remote.items.map { it.name })
                if (items.isNotEmpty()) {
                    checklistDao.insertChecklistItems(
                        items.mapIndexed { index, name ->
                            ChecklistItemEntity(
                                checklistId = checklistId,
                                name = name,
                                position = index,
                            )
                        },
                    )
                }
            }
        }
        mergePlan.report
    }

    private fun buildMergePlan(
        localChecklists: List<ChecklistWithItems>,
        remoteChecklists: List<RemoteChecklistDto>,
    ): MergePlan {
        val now = System.currentTimeMillis()
        val remoteById = remoteChecklists.associateBy { it.id }
        val unmatchedRemoteIds = remoteChecklists.map { it.id }.toMutableSet()
        val remoteIdsByTitle = remoteChecklists
            .groupBy { normalizeTitleKey(it.title) }
            .mapValues { entry -> entry.value.map { it.id }.toMutableList() }
            .toMutableMap()

        val mergedRemoteChecklists = mutableListOf<RemoteChecklistDto>()
        val localAssignments = mutableListOf<LocalChecklistAssignment>()
        val remoteOnlyServerIds = mutableListOf<String>()

        var addedToLocal = 0
        var addedToServer = 0
        var updatedOnServer = 0

        localChecklists.forEach { local ->
            val localTitle = local.checklist.title.trim()
            if (localTitle.isEmpty()) return@forEach

            val localItems = normalizeItemNames(local.items.map { it.name })
            val remoteMatchById = local.checklist.serverId
                ?.takeIf { unmatchedRemoteIds.contains(it) }
                ?.let(remoteById::get)
            val remoteMatch = remoteMatchById ?: findRemoteByTitle(
                titleKey = normalizeTitleKey(localTitle),
                remoteById = remoteById,
                remoteIdsByTitle = remoteIdsByTitle,
                unmatchedRemoteIds = unmatchedRemoteIds,
            )

            if (remoteMatchById != null) {
                unmatchedRemoteIds.remove(remoteMatchById.id)
            }

            val resolvedServerId = when {
                remoteMatch != null -> remoteMatch.id
                !local.checklist.serverId.isNullOrBlank() -> local.checklist.serverId
                else -> UUID.randomUUID().toString()
            }.orEmpty()

            val remoteNeedsUpdate = remoteMatch?.let {
                !hasSameChecklistContent(
                    remoteChecklist = it,
                    localTitle = localTitle,
                    localItems = localItems,
                )
            } ?: false

            val resolvedUpdatedAt = when {
                remoteMatch == null -> maxOf(local.checklist.updatedAt, now)
                remoteNeedsUpdate -> maxOf(now, local.checklist.updatedAt, remoteMatch.updatedAt ?: 0L)
                else -> maxOf(local.checklist.updatedAt, remoteMatch.updatedAt ?: 0L)
            }

            mergedRemoteChecklists += RemoteChecklistDto(
                id = resolvedServerId,
                title = localTitle,
                updatedAt = resolvedUpdatedAt,
                items = localItems.map(::RemoteChecklistItemDto),
            )
            localAssignments += LocalChecklistAssignment(
                localChecklistId = local.checklist.id,
                serverId = resolvedServerId,
            )

            when {
                remoteMatch == null -> addedToServer += 1
                remoteNeedsUpdate -> updatedOnServer += 1
            }
        }

        remoteChecklists.forEach { remote ->
            if (!unmatchedRemoteIds.remove(remote.id)) return@forEach
            val normalizedRemote = normalizeRemoteChecklist(remote)
            mergedRemoteChecklists += normalizedRemote
            remoteOnlyServerIds += normalizedRemote.id
            addedToLocal += 1
        }

        return MergePlan(
            remoteChecklists = mergedRemoteChecklists,
            localAssignments = localAssignments,
            remoteOnlyServerIds = remoteOnlyServerIds,
            report = SyncReport(
                addedToLocal = addedToLocal,
                addedToServer = addedToServer,
                updatedOnServer = updatedOnServer,
            ),
        )
    }

    private fun findRemoteByTitle(
        titleKey: String,
        remoteById: Map<String, RemoteChecklistDto>,
        remoteIdsByTitle: MutableMap<String, MutableList<String>>,
        unmatchedRemoteIds: MutableSet<String>,
    ): RemoteChecklistDto? {
        val idsForTitle = remoteIdsByTitle[titleKey] ?: return null
        while (idsForTitle.isNotEmpty()) {
            val remoteId = idsForTitle.removeAt(0)
            if (!unmatchedRemoteIds.remove(remoteId)) continue
            return remoteById[remoteId]
        }
        return null
    }

    private fun hasSameChecklistContent(
        remoteChecklist: RemoteChecklistDto,
        localTitle: String,
        localItems: List<String>,
    ): Boolean {
        return remoteChecklist.title.trim() == localTitle &&
            normalizeItemNames(remoteChecklist.items.map { it.name }) == localItems
    }

    private fun normalizeRemoteChecklist(remoteChecklist: RemoteChecklistDto): RemoteChecklistDto {
        val title = remoteChecklist.title.trim()
        return RemoteChecklistDto(
            id = remoteChecklist.id,
            title = title,
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

    private data class MergePlan(
        val remoteChecklists: List<RemoteChecklistDto>,
        val localAssignments: List<LocalChecklistAssignment>,
        val remoteOnlyServerIds: List<String>,
        val report: SyncReport
    )

    private data class LocalChecklistAssignment(
        val localChecklistId: Long,
        val serverId: String
    )
}
