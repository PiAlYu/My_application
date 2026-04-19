package com.example.storechecklist.ui.screens.user

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storechecklist.domain.ChecklistItem
import com.example.storechecklist.domain.UserChecklistMode
import com.example.storechecklist.ui.viewmodel.UserChecklistViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserChecklistScreen(
    viewModel: UserChecklistViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var celebrationTrigger by rememberSaveable { mutableIntStateOf(0) }
    var previousCompletion by rememberSaveable { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(state.isLoading, state.isNotFound, state.totalItems, state.isCompleted) {
        if (state.isLoading || state.isNotFound) {
            return@LaunchedEffect
        }

        val completedNow = state.totalItems > 0 && state.isCompleted
        val completedBefore = previousCompletion
        previousCompletion = completedNow

        if (completedBefore != null && !completedBefore && completedNow) {
            celebrationTrigger += 1
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            ChecklistModeCard(
                                mode = state.mode,
                                modifier = Modifier.padding(top = 12.dp),
                            )
                        }
                        item {
                            Button(onClick = viewModel::resetProgress) {
                                Text("Сбросить прогресс списка")
                            }
                        }

                        if (state.isCompleted) {
                            item {
                                ChecklistCompletionCard(totalItems = state.totalItems)
                            }
                        }

                        item {
                            ChecklistSectionHeader(
                                remainingItems = state.items.size,
                                totalItems = state.totalItems,
                                completedItems = state.completedItems,
                                mode = state.mode,
                                isCompleted = state.isCompleted,
                            )
                        }

                        if (state.items.isEmpty()) {
                            item {
                                Text(
                                    text = when {
                                        state.totalItems == 0 -> "В списке пока нет товаров."
                                        state.isCompleted -> "Список полностью пройден. Можно сбросить прогресс и пройти его заново."
                                        else -> "Все товары обработаны в текущем режиме."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 16.dp),
                                )
                            }
                        } else {
                            items(state.items, key = { item -> item.id }) { item ->
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

        CelebrationOverlay(
            trigger = celebrationTrigger,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ChecklistModeCard(
    mode: UserChecklistMode,
    modifier: Modifier = Modifier,
) {
    val modeTitle = when (mode) {
        UserChecklistMode.HIDE_ON_TAP -> "Скрывать при нажатии"
        UserChecklistMode.MARKER -> "Маркер рядом"
    }
    val helperText = when (mode) {
        UserChecklistMode.HIDE_ON_TAP -> "Выбранный товар исчезает из списка, чтобы внимание оставалось только на оставшихся позициях."
        UserChecklistMode.MARKER -> "Выбранный товар остаётся на месте и получает отметку, чтобы было удобно сверять весь список целиком."
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Режим прохождения",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = modeTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                text = "$helperText Изменить режим можно в настройках на главном экране.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ChecklistCompletionCard(totalItems: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp),
                )
            }

            Column {
                Text(
                    text = "Список завершён",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Все $totalItems товаров пройдены. Отличная работа.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
                )
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
    remainingItems: Int,
    totalItems: Int,
    completedItems: Int,
    mode: UserChecklistMode,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val helperText = when {
        isCompleted -> "Все товары уже отмечены. Можно вернуться к спискам или начать заново после сброса прогресса."
        mode == UserChecklistMode.HIDE_ON_TAP -> "Нажмите на товар, и он исчезнет из текущего списка."
        else -> "Нажмите на товар, чтобы отметить его маркером, не убирая из общего списка."
    }
    val badgeText = when (mode) {
        UserChecklistMode.HIDE_ON_TAP -> "$remainingItems осталось"
        UserChecklistMode.MARKER -> "$completedItems/$totalItems"
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
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CelebrationOverlay(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    if (trigger == 0) return

    val progress = remember(trigger) { Animatable(0f) }
    var isVisible by remember(trigger) { mutableStateOf(true) }

    LaunchedEffect(trigger) {
        isVisible = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 2200,
                easing = LinearOutSlowInEasing,
            ),
        )
        isVisible = false
    }

    if (!isVisible) return

    val overlayAlpha = when {
        progress.value < 0.76f -> 1f
        else -> (1f - ((progress.value - 0.76f) / 0.24f)).coerceIn(0f, 1f)
    }

    Box(
        modifier = modifier.alpha(overlayAlpha),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val palette = listOf(
                Color(0xFFFFB703),
                Color(0xFF4CC9F0),
                Color(0xFFFF6B6B),
                Color(0xFF80ED99),
            )
            val burstCenters = listOf(
                Offset(size.width * 0.16f, size.height * 0.2f),
                Offset(size.width * 0.4f, size.height * 0.12f),
                Offset(size.width * 0.7f, size.height * 0.18f),
                Offset(size.width * 0.84f, size.height * 0.28f),
            )

            burstCenters.forEachIndexed { index, center ->
                val localProgress = ((progress.value - index * 0.08f) / 0.58f).coerceIn(0f, 1f)
                if (localProgress > 0f) {
                    drawFireworkBurst(
                        center = center,
                        progress = FastOutSlowInEasing.transform(localProgress),
                        color = palette[index % palette.size],
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.96f),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Список полностью пройден",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private fun DrawScope.drawFireworkBurst(
    center: Offset,
    progress: Float,
    color: Color,
) {
    val rayCount = 12
    val maxRadius = size.minDimension * 0.16f
    val radius = maxRadius * progress
    val lineAlpha = (1f - progress * 0.55f).coerceIn(0f, 1f)
    val particleAlpha = (1f - progress * 0.35f).coerceIn(0f, 1f)

    repeat(rayCount) { index ->
        val angle = ((2f * PI.toFloat()) / rayCount) * index + progress * 0.8f
        val directionX = cos(angle)
        val directionY = sin(angle)
        val start = Offset(
            x = center.x + directionX * radius * 0.24f,
            y = center.y + directionY * radius * 0.24f,
        )
        val end = Offset(
            x = center.x + directionX * radius,
            y = center.y + directionY * radius,
        )

        drawLine(
            color = color.copy(alpha = lineAlpha),
            start = start,
            end = end,
            strokeWidth = 6f * (1f - progress * 0.4f),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color.copy(alpha = particleAlpha),
            radius = 7f * (1f - progress * 0.45f),
            center = end,
        )
    }

    drawCircle(
        color = color.copy(alpha = 0.22f * (1f - progress)),
        radius = radius * 0.5f,
        center = center,
    )
}
