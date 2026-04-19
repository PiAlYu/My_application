package com.example.storechecklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.storechecklist.ui.navigation.AppNavHost
import com.example.storechecklist.ui.theme.StoreChecklistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings = AppGraph.appSettingsStore.settings.collectAsStateWithLifecycle()
            StoreChecklistTheme(themeMode = settings.value.themeMode) {
                Surface {
                    AppNavHost()
                }
            }
        }
    }
}
