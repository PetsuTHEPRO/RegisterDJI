package com.sloth.registerapp.UI

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.sloth.registerapp.UI.Screen.AboutScreen

class AboutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Define o conteúdo da tela usando Jetpack Compose
        setContent {
            MaterialTheme { // Aplica o tema do Material Design 3
                AboutScreen()
            }
        }
    }
}