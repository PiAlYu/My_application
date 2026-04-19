package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.ChecklistItem
import com.example.storechecklist.domain.UserChecklistMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserChecklistUiState(
    val title: String = "",
    val mode: UserChecklistMode = UserChecklistMode.HIDE_ON_TAP,
    val items: List<ChecklistItem> = emptyList(),
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
)

class UserChecklistViewModel(
    private val checklistId: Long,
) : ViewModel() {
    private val repository = AppGraph.repository
    private val settingsStore = AppGraph.appSettingsStore

    val uiState: StateFlow<UserChecklistUiState> = combine(
        repository.observeChecklistDetails(checklistId),
        settingsStore.settings,
    ) { details, settings ->
        val mode = settings.checklistMode
        if (details == null) {
            UserChecklistUiState(
                mode = mode,
                isLoading = false,
                isNotFound = true,
            )
        } else {
            val completedItems = when (mode) {
                UserChecklistMode.HIDE_ON_TAP -> details.items.count { it.hiddenInHideMode }
                UserChecklistMode.MARKER -> details.items.count { it.markedInMarkerMode }
            }
            val visibleItems = when (mode) {
                UserChecklistMode.HIDE_ON_TAP -> details.items
                    .filterNot { it.hiddenInHideMode }
                    .sortedBy { it.position }
                UserChecklistMode.MARKER -> details.items.sortedBy { it.position }
            }
            val totalItems = details.items.size
            UserChecklistUiState(
                title = details.title,
                mode = mode,
                items = visibleItems,
                totalItems = totalItems,
                completedItems = completedItems,
                isCompleted = totalItems > 0 && completedItems == totalItems,
                isLoading = false,
                isNotFound = false,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserChecklistUiState(),
    )

    fun onItemTapped(itemId: Long) {
        viewModelScope.launch {
            repository.onUserTappedItem(
                checklistId = checklistId,
                itemId = itemId,
                mode = settingsStore.settings.value.checklistMode,
            )
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetChecklistProgress(checklistId)
        }
    }

    companion object {
        fun factory(checklistId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UserChecklistViewModel(checklistId) as T
                }
            }
        }
    }
}
