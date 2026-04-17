package com.example.storechecklist.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storechecklist.ui.viewmodel.AdminChecklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChecklistScreen(
    viewModel: AdminChecklistViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Изменение списка") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text("Загрузка списка...")
                }
            }

            state.isNotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                ) {
                    Text("Список не найден.")
                }
            }

            else -> {
                var titleDraft by rememberSaveable(state.checklistId) { mutableStateOf(state.title) }
                var singleItemDraft by rememberSaveable { mutableStateOf("") }
                var batchItemsDraft by rememberSaveable { mutableStateOf("") }
                var selectedIds by remember { mutableStateOf(setOf<Long>()) }

                LaunchedEffect(state.items) {
                    val availableIds = state.items.map { it.id }.toSet()
                    selectedIds = selectedIds.intersect(availableIds)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Название списка",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = titleDraft,
                        onValueChange = { titleDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Button(
                        onClick = { viewModel.renameChecklist(titleDraft) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Сохранить название")
                    }

                    HorizontalDivider()

                    Text(
                        text = "Добавить один товар",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = singleItemDraft,
                            onValueChange = { singleItemDraft = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Название товара") },
                        )
                        Button(
                            onClick = {
                                viewModel.addSingleItem(singleItemDraft)
                                singleItemDraft = ""
                            },
                        ) {
                            Text("Добавить")
                        }
                    }

                    Text(
                        text = "Добавить несколько товаров",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedTextField(
                        value = batchItemsDraft,
                        onValueChange = { batchItemsDraft = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        label = { Text("Каждый товар с новой строки") },
                    )
                    Button(
                        onClick = {
                            viewModel.addBatchItems(batchItemsDraft)
                            batchItemsDraft = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Добавить список товаров")
                    }

                    HorizontalDivider()

                    Text(
                        text = "Удаление товаров (по одному или пакетно)",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    if (state.items.isEmpty()) {
                        Text(
                            text = "В этом списке пока нет товаров.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        state.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .toggleable(
                                        value = selectedIds.contains(item.id),
                                        onValueChange = { isChecked ->
                                            selectedIds = if (isChecked) {
                                                selectedIds + item.id
                                            } else {
                                                selectedIds - item.id
                                            }
                                        },
                                    )
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(item.id),
                                    onCheckedChange = null,
                                )
                                Text(
                                    text = item.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp),
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.deleteItems(listOf(item.id))
                                        selectedIds = selectedIds - item.id
                                    },
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Удалить товар")
                                }
                            }
                            HorizontalDivider()
                        }

                        Button(
                            onClick = {
                                viewModel.deleteItems(selectedIds.toList())
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Удалить выбранные (${selectedIds.size})")
                        }
                    }
                }
            }
        }
    }
}
