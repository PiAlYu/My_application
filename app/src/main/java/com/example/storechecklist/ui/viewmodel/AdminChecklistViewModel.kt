package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.ChecklistItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminChecklistUiState(
    val checklistId: Long = -1L,
    val title: String = "",
    val items: List<ChecklistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
)

class AdminChecklistViewModel(
    private val checklistId: Long,
) : ViewModel() {
    private val repository = AppGraph.repository

    val uiState: StateFlow<AdminChecklistUiState> = repository.observeChecklistDetails(checklistId)
        .map { details ->
            if (details == null) {
                AdminChecklistUiState(
                    checklistId = checklistId,
                    isLoading = false,
                    isNotFound = true,
                )
            } else {
                AdminChecklistUiState(
                    checklistId = details.id,
                    title = details.title,
                    items = details.items.sortedBy { it.position },
                    isLoading = false,
                    isNotFound = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdminChecklistUiState(checklistId = checklistId),
        )

    fun renameChecklist(newTitle: String) {
        viewModelScope.launch {
            repository.renameChecklist(checklistId = checklistId, title = newTitle)
        }
    }

    fun addSingleItem(name: String) {
        viewModelScope.launch {
            repository.addItems(checklistId = checklistId, names = listOf(name))
        }
    }

    fun addBatchItems(rawText: String) {
        val items = rawText
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.addItems(checklistId = checklistId, names = items)
        }
    }

    fun deleteItems(itemIds: List<Long>) {
        viewModelScope.launch {
            repository.deleteItems(itemIds)
        }
    }

    companion object {
        fun factory(checklistId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminChecklistViewModel(checklistId) as T
                }
            }
        }
    }
}
