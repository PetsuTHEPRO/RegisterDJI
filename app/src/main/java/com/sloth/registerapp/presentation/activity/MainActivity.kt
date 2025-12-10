package com.sloth.registerapp.presentation.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.sloth.registerapp.core.utils.PermissionHelper
import com.sloth.registerapp.data.datasource.DroneTelemetryManager
import com.sloth.registerapp.data.sdk.DJIConnectionHelper
import com.sloth.registerapp.presentation.screen.MainScreen
import com.sloth.registerapp.presentation.theme.RegisterAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Prepara a janela para desenhar por trás das barras do sistema (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Obtém o controlador das barras do sistema
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        // 3. Esconde as barras de status e de navegação
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // 4. Configura o comportamento para as barras reaparecerem com um deslize
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        PermissionHelper.initializeLaunchers(this) {
            DJIConnectionHelper.registerApp(applicationContext)
        }
        PermissionHelper.checkAndRequestAllPermissions(this) {
            DJIConnectionHelper.registerApp(applicationContext)
        }

        // --- NOVO: Inicia o gerenciador de telemetria ---
        DroneTelemetryManager.init(lifecycleScope)

        setContent {
            RegisterAppTheme {
                MainScreen()
            }
        }
    }
}