package com.example.storechecklist.ui.screens.user

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storechecklist.domain.ChecklistItem
import com.example.storechecklist.domain.UserChecklistMode
import com.example.storechecklist.ui.viewmodel.UserChecklistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserChecklistScreen(
    viewModel: UserChecklistViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val title = if (state.title.isNotBlank()) state.title else "Список"
                    Text(title)
                },
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
                    Text("Загрузка...")
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text = "Режим прохождения",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.mode == UserChecklistMode.HIDE_ON_TAP,
                                onClick = { viewModel.setMode(UserChecklistMode.HIDE_ON_TAP) },
                                label = { Text("Скрывать при нажатии") },
                            )
                            FilterChip(
                                selected = state.mode == UserChecklistMode.MARKER,
                                onClick = { viewModel.setMode(UserChecklistMode.MARKER) },
                                label = { Text("Маркер рядом") },
                            )
                        }
                    }
                    item {
                        Button(onClick = viewModel::resetProgress) {
                            Text("Сбросить прогресс списка")
                        }
                    }
                    item {
                        Text(
                            text = "Товары",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }

                    if (state.items.isEmpty()) {
                        item {
                            Text(
                                text = if (state.mode == UserChecklistMode.HIDE_ON_TAP) {
                                    "Все товары обработаны в текущем режиме."
                                } else {
                                    "В списке нет товаров."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            UserChecklistRow(
                                item = item,
                                mode = state.mode,
                                onTap = { viewModel.onItemTapped(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserChecklistRow(
    item: ChecklistItem,
    mode: UserChecklistMode,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mode == UserChecklistMode.MARKER) {
            val icon = if (item.markedInMarkerMode) {
                Icons.Filled.CheckCircle
            } else {
                Icons.Filled.RadioButtonUnchecked
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        }

        Text(
            text = item.name,
            modifier = Modifier.padding(start = 10.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}
