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
)

class AdminListsViewModel : ViewModel() {
    private val repository = AppGraph.repository
    private val syncInProgress = MutableStateFlow(false)
    private val syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AdminListsUiState> = combine(
        repository.observeChecklistSummaries(),
        syncInProgress,
        syncMessage,
    ) { checklists, isSyncing, message ->
        AdminListsUiState(
            checklists = checklists,
            isSyncing = isSyncing,
            syncMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminListsUiState(),
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

    fun syncWithServer(isSuperUserMode: Boolean) {
        if (syncInProgress.value) return
        viewModelScope.launch {
            syncInProgress.value = true
            syncMessage.value = try {
                if (isSuperUserMode) {
                    val report = repository.replaceServerWithLocal()
                    "Серверная база полностью заменена локальной. Отправлено списков: ${report.addedToServer}."
                } else {
                    val report = repository.importMissingFromServer()
                    "С сервера добавлено списков: ${report.addedToLocal}. Локальные списки не удалялись."
                }
            } catch (error: Exception) {
                val details = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?.let { " $it" }
                    .orEmpty()
                if (isSuperUserMode) {
                    "Не удалось переписать сервер локальной базой. Проверьте URL, токен и доступность сервера.$details"
                } else {
                    "Сервер недоступен или требует токен. Локальные списки остаются без изменений.$details"
                }
            } finally {
                syncInProgress.value = false
            }
        }
    }

    fun consumeSyncMessage() {
        syncMessage.value = null
    }
}
