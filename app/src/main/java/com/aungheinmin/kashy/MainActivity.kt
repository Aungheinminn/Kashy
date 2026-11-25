package com.aungheinmin.kashy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aungheinmin.kashy.ui.HomeScreen
import com.aungheinmin.kashy.ui.theme.KashyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KashyTheme {
                HomeScreen()
            }
        }
    }
}
