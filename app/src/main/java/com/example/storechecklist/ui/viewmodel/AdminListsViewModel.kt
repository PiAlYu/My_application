package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.ChecklistSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminListsUiState(
    val checklists: List<ChecklistSummary> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val serverBaseUrl: String = "",
)

class AdminListsViewModel : ViewModel() {
    private val repository = AppGraph.repository
    private val syncInProgress = MutableStateFlow(false)
    private val syncMessage = MutableStateFlow<String?>(null)
    private val serverBaseUrl = MutableStateFlow(repository.getServerBaseUrl())

    val uiState: StateFlow<AdminListsUiState> = combine(
        repository.observeChecklistSummaries(),
        syncInProgress,
        syncMessage,
        serverBaseUrl,
    ) { checklists, isSyncing, message, baseUrl ->
        AdminListsUiState(
            checklists = checklists,
            isSyncing = isSyncing,
            syncMessage = message,
            serverBaseUrl = baseUrl,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminListsUiState(serverBaseUrl = repository.getServerBaseUrl()),
    )

    fun createChecklist(title: String) {
        viewModelScope.launch {
            repository.createChecklist(title)
        }
    }

    fun deleteChecklist(checklistId: Long) {
        viewModelScope.launch {
            repository.deleteChecklist(checklistId)
        }
    }

    fun saveServerBaseUrl(rawUrl: String) {
        val result = repository.saveServerBaseUrl(rawUrl)
        if (result.isSuccess) {
            val savedUrl = result.getOrNull().orEmpty()
            serverBaseUrl.value = savedUrl
            syncMessage.value = "Адрес сервера сохранён: $savedUrl"
        } else {
            val reason = result.exceptionOrNull()?.message ?: "Неверный формат URL."
            syncMessage.value = "Адрес сервера не сохранён. $reason"
        }
    }

    fun syncWithServer() {
        if (syncInProgress.value) return
        viewModelScope.launch {
            syncInProgress.value = true
            syncMessage.value = try {
                val report = repository.syncWithServer()
                "Синхронизация завершена. На устройство: ${report.addedToLocal}, на сервер: ${report.addedToServer}, обновлено на сервере: ${report.updatedOnServer}."
            } catch (error: Exception) {
                "Сервер недоступен. Приложение продолжает работать офлайн."
            } finally {
                syncInProgress.value = false
            }
        }
    }

    fun consumeSyncMessage() {
        syncMessage.value = null
    }
}
