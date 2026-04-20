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

data class UserListsUiState(
    val checklists: List<ChecklistSummary> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
)

class UserListsViewModel : ViewModel() {
    private val repository = AppGraph.repository
    private val syncInProgress = MutableStateFlow(false)
    private val syncMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<UserListsUiState> = combine(
        repository.observeChecklistSummaries(),
        syncInProgress,
        syncMessage,
    ) { checklists, isSyncing, message ->
        UserListsUiState(
            checklists = checklists,
            isSyncing = isSyncing,
            syncMessage = message,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserListsUiState(),
        )

    fun syncWithServer() {
        if (syncInProgress.value) return
        viewModelScope.launch {
            syncInProgress.value = true
            syncMessage.value = try {
                val report = repository.importMissingFromServer()
                "С сервера добавлено списков: ${report.addedToLocal}."
            } catch (error: Exception) {
                val details = error.message
                    ?.takeIf { it.isNotBlank() }
                    ?.let { " $it" }
                    .orEmpty()
                "Не удалось обновить списки.$details"
            } finally {
                syncInProgress.value = false
            }
        }
    }

    fun consumeSyncMessage() {
        syncMessage.value = null
    }
}
