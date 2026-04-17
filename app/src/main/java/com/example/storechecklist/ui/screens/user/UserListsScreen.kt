package com.example.storechecklist.ui.screens.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storechecklist.domain.ChecklistSummary
import com.example.storechecklist.ui.viewmodel.UserListsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListsScreen(
    onBack: () -> Unit,
    onOpenChecklist: (Long) -> Unit,
    viewModel: UserListsViewModel = viewModel(),
) {
    val checklists by viewModel.checklists.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Пользователь: списки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
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
                    text = "Выберите список для прохождения",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (checklists.isEmpty()) {
                item {
                    Text(
                        text = "Списков нет. Выполните синхронизацию или создайте список в режиме управления.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(checklists, key = { it.id }) { checklist ->
                    UserChecklistCard(
                        checklist = checklist,
                        onClick = { onOpenChecklist(checklist.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserChecklistCard(
    checklist: ChecklistSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = checklist.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Товаров: ${checklist.itemCount}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
