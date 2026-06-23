package io.github.tommaso.mappand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.tommaso.mappand.ui.AppNavigation
import io.github.tommaso.mappand.ui.theme.MappandTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MappandTheme {
                AppNavigation()
            }
        }
    }
}
