package com.example.storechecklist.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storechecklist.R
import com.example.storechecklist.domain.AppThemeMode
import com.example.storechecklist.domain.UserChecklistMode
import com.example.storechecklist.ui.viewmodel.RoleSelectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onOpenAdmin: () -> Unit,
    onOpenSuperUser: () -> Unit,
    onOpenUser: () -> Unit,
    viewModel: RoleSelectionViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }

    if (isSettingsOpen) {
        MainSettingsSheet(
            selectedThemeMode = state.themeMode,
            selectedChecklistMode = state.checklistMode,
            onThemeSelected = viewModel::setThemeMode,
            onChecklistModeSelected = viewModel::setChecklistMode,
            onDismiss = { isSettingsOpen = false },
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Store Checklist") },
                navigationIcon = {
                    IconButton(onClick = { isSettingsOpen = true }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Настройки",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSuperUser) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Super user",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_brand_robot),
                contentDescription = "Аватар приложения",
                modifier = Modifier.size(210.dp),
                contentScale = ContentScale.Fit,
            )

            Text(
                text = "Store Checklist",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Робот-помощник помогает быстро открывать нужный режим и работать со списками без визуальной перегрузки.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )

            SettingsSnapshotCard(
                themeMode = state.themeMode,
                checklistMode = state.checklistMode,
                modifier = Modifier.padding(top = 18.dp, bottom = 28.dp),
            )

            Text(
                text = "Выберите окно работы",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "В режиме управления можно редактировать локальные списки и добавлять в них списки с сервера. Кнопка super user в правом верхнем углу открывает режим, который полностью заменяет серверную базу локальной.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )

            Button(
                onClick = onOpenAdmin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                )
                Text(
                    text = " Управление списками",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            OutlinedButton(
                onClick = onOpenUser,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(top = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                )
                Text(
                    text = " Прохождение списка",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun SettingsSnapshotCard(
    themeMode: AppThemeMode,
    checklistMode: UserChecklistMode,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Быстрые настройки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Тема: ${themeMode.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Режим прохождения: ${checklistMode.title()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsSheet(
    selectedThemeMode: AppThemeMode,
    selectedChecklistMode: UserChecklistMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onChecklistModeSelected: (UserChecklistMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Здесь собраны основные параметры внешнего вида и прохождения списков.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Тема",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsChoiceCard(
                    title = "Как в системе",
                    description = "Приложение подстраивается под системную тему устройства.",
                    selected = selectedThemeMode == AppThemeMode.SYSTEM,
                    onClick = { onThemeSelected(AppThemeMode.SYSTEM) },
                )
                SettingsChoiceCard(
                    title = "Светлая",
                    description = "Чистый светлый интерфейс с голубыми акцентами.",
                    selected = selectedThemeMode == AppThemeMode.LIGHT,
                    onClick = { onThemeSelected(AppThemeMode.LIGHT) },
                )
                SettingsChoiceCard(
                    title = "Тёмная",
                    description = "Контрастный тёмный стиль для вечерней работы и AMOLED-экрана.",
                    selected = selectedThemeMode == AppThemeMode.DARK,
                    onClick = { onThemeSelected(AppThemeMode.DARK) },
                )
            }

            HorizontalDivider()

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Режим прохождения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Этот режим применяется ко всем пользовательским спискам и больше не переключается внутри экрана прохождения.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsChoiceCard(
                    title = UserChecklistMode.HIDE_ON_TAP.title(),
                    description = "После нажатия товар исчезает из текущего списка.",
                    selected = selectedChecklistMode == UserChecklistMode.HIDE_ON_TAP,
                    onClick = { onChecklistModeSelected(UserChecklistMode.HIDE_ON_TAP) },
                )
                SettingsChoiceCard(
                    title = UserChecklistMode.MARKER.title(),
                    description = "Товар остаётся на месте и помечается галочкой рядом.",
                    selected = selectedChecklistMode == UserChecklistMode.MARKER,
                    onClick = { onChecklistModeSelected(UserChecklistMode.MARKER) },
                )
            }

            Text(
                text = "Настройки применяются сразу и сохраняются для следующих запусков приложения.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
    }
}

@Composable
private fun SettingsChoiceCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun AppThemeMode.label(): String {
    return when (this) {
        AppThemeMode.SYSTEM -> "Как в системе"
        AppThemeMode.LIGHT -> "Светлая"
        AppThemeMode.DARK -> "Тёмная"
    }
}

private fun UserChecklistMode.title(): String {
    return when (this) {
        UserChecklistMode.HIDE_ON_TAP -> "Скрывать при нажатии"
        UserChecklistMode.MARKER -> "Маркер рядом"
    }
}
