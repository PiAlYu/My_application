package com.example.storechecklist.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storechecklist.domain.ChecklistSummary
import com.example.storechecklist.ui.viewmodel.AdminListsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListsScreen(
    isSuperUserMode: Boolean,
    onBack: () -> Unit,
    onOpenChecklist: (Long) -> Unit,
    viewModel: AdminListsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newChecklistTitle by remember { mutableStateOf("") }
    var serverUrlDraft by remember(state.serverBaseUrl) { mutableStateOf(state.serverBaseUrl) }
    var readTokenDraft by remember(state.serverReadToken) { mutableStateOf(state.serverReadToken) }
    var writeTokenDraft by remember(state.serverWriteToken) { mutableStateOf(state.serverWriteToken) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.syncMessage) {
        val message = state.syncMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeSyncMessage()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isSuperUserMode) {
                            "Super user: управление"
                        } else {
                            "Управление списками"
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncWithServer(isSuperUserMode) },
                        enabled = !state.isSyncing,
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = "Синхронизировать")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.PlaylistAdd, contentDescription = "Добавить список")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Подключение к серверу",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            item {
                OutlinedTextField(
                    value = serverUrlDraft,
                    onValueChange = { serverUrlDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Например: https://checklists.example.ru/api/") },
                    supportingText = {
                        Text("Для внешнего сервера используйте HTTPS. HTTP оставлен только для localhost и локальной сети.")
                    },
                )
            }
            item {
                OutlinedTextField(
                    value = readTokenDraft,
                    onValueChange = { readTokenDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Read token, если сервер его требует") },
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = {
                        Text("Это токен для чтения списков. Если публичное чтение разрешено, поле можно оставить пустым.")
                    },
                )
            }
            if (isSuperUserMode) {
                item {
                    OutlinedTextField(
                        value = writeTokenDraft,
                        onValueChange = { writeTokenDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Write token для режима super user") },
                        visualTransformation = PasswordVisualTransformation(),
                        supportingText = {
                            Text("Нужен только устройствам, которые могут полностью переписывать серверную базу.")
                        },
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        viewModel.saveServerConnection(
                            rawUrl = serverUrlDraft,
                            rawReadToken = readTokenDraft,
                            rawWriteToken = writeTokenDraft,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Сохранить подключение")
                }
            }
            item {
                SyncModeBanner(isSuperUserMode = isSuperUserMode)
            }

            item {
                Text(
                    text = "Выберите список для редактирования",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (state.checklists.isEmpty()) {
                item {
                    Text(
                        text = "Списков пока нет. Создайте локальный список или выполните синхронизацию.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(state.checklists, key = { it.id }) { checklist ->
                    AdminChecklistCard(
                        checklist = checklist,
                        onEdit = { onOpenChecklist(checklist.id) },
                        onDelete = { viewModel.deleteChecklist(checklist.id) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Новый список") },
            text = {
                OutlinedTextField(
                    value = newChecklistTitle,
                    onValueChange = { newChecklistTitle = it },
                    singleLine = true,
                    label = { Text("Название") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createChecklist(newChecklistTitle)
                        newChecklistTitle = ""
                        showCreateDialog = false
                    },
                ) { Text("Создать") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun SyncModeBanner(
    isSuperUserMode: Boolean,
) {
    val containerColor = if (isSuperUserMode) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isSuperUserMode) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val title = if (isSuperUserMode) {
        "Режим super user"
    } else {
        "Обычный режим подключения"
    }
    val body = if (isSuperUserMode) {
        "Синхронизация полностью заменит базу сервера текущими локальными списками. Если локальная база пуста, сервер тоже станет пустым."
    } else {
        "Синхронизация только добавит на устройство списки, которые уже есть на сервере. Локальные списки и удаления не отправляются."
    }

    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun AdminChecklistCard(
    checklist: ChecklistSummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = checklist.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Товаров: ${checklist.itemCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 2.dp),
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(if (checklist.isFromServer) "Есть на сервере" else "Только локально")
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Редактировать")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}
