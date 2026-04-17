package com.example.storechecklist.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storechecklist.AppGraph
import com.example.storechecklist.domain.ChecklistSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UserListsViewModel : ViewModel() {
    private val repository = AppGraph.repository

    val checklists: StateFlow<List<ChecklistSummary>> = repository.observeChecklistSummaries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
}

