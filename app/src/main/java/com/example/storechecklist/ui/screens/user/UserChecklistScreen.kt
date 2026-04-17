package com.example.storechecklist.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
                        ChecklistSectionHeader(
                            itemCount = state.items.size,
                            mode = state.mode,
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
    val accentColor = MaterialTheme.colorScheme.primary
    val isMarked = mode == UserChecklistMode.MARKER && item.markedInMarkerMode

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isMarked) {
                accentColor.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isMarked) {
                accentColor.copy(alpha = 0.06f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mode == UserChecklistMode.MARKER) {
                val icon = if (item.markedInMarkerMode) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Filled.RadioButtonUnchecked
                }
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (item.markedInMarkerMode) {
                            accentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(8.dp),
                    )
                }
            } else {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .size(8.dp)
                            .background(accentColor, CircleShape),
                    )
                }
            }

            Text(
                text = item.name,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            )
        }
    }
}

@Composable
private fun ChecklistSectionHeader(
    itemCount: Int,
    mode: UserChecklistMode,
    modifier: Modifier = Modifier,
) {
    val helperText = if (mode == UserChecklistMode.HIDE_ON_TAP) {
        "Нажмите на товар, и он исчезнет из текущего списка."
    } else {
        "Нажмите на товар, чтобы отметить его маркером."
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.08f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Товары",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Text(
                    text = itemCount.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
