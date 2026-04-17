package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.ChecklistItem
import com.example.storechecklist.domain.UserChecklistMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserChecklistUiState(
    val title: String = "",
    val mode: UserChecklistMode = UserChecklistMode.HIDE_ON_TAP,
    val items: List<ChecklistItem> = emptyList(),
    val isLoading: Boolean = true,
    val isNotFound: Boolean = false,
)

class UserChecklistViewModel(
    private val checklistId: Long,
) : ViewModel() {
    private val repository = AppGraph.repository
    private val modeFlow = MutableStateFlow(UserChecklistMode.HIDE_ON_TAP)

    val uiState: StateFlow<UserChecklistUiState> = combine(
        repository.observeChecklistDetails(checklistId),
        modeFlow,
    ) { details, mode ->
        if (details == null) {
            UserChecklistUiState(
                mode = mode,
                isLoading = false,
                isNotFound = true,
            )
        } else {
            val visibleItems = when (mode) {
                UserChecklistMode.HIDE_ON_TAP -> details.items
                    .filterNot { it.hiddenInHideMode }
                    .sortedBy { it.position }
                UserChecklistMode.MARKER -> details.items.sortedBy { it.position }
            }
            UserChecklistUiState(
                title = details.title,
                mode = mode,
                items = visibleItems,
                isLoading = false,
                isNotFound = false,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserChecklistUiState(),
    )

    fun setMode(mode: UserChecklistMode) {
        modeFlow.value = mode
    }

    fun onItemTapped(itemId: Long) {
        viewModelScope.launch {
            repository.onUserTappedItem(
                checklistId = checklistId,
                itemId = itemId,
                mode = modeFlow.value,
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
