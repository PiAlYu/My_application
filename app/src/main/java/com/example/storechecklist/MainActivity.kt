package com.example.storechecklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.example.storechecklist.ui.navigation.AppNavHost
import com.example.storechecklist.ui.theme.StoreChecklistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StoreChecklistTheme {
                Surface {
                    AppNavHost()
                }
            }
        }
    }
}

