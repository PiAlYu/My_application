package com.example.storechecklist.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.storechecklist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(
    onOpenAdmin: () -> Unit,
    onOpenUser: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Store Checklist") },
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
                modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )

            Text(
                text = "Выберите окно работы",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "В режиме управления можно редактировать и синхронизировать списки. В режиме прохождения можно быстро отмечать товары по выбранному списку.",
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
