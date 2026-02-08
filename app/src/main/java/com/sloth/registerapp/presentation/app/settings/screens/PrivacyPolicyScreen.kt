package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Política de Privacidade") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Text("Documento Simulado", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            Text(
                text = "\n1. Coleta de dados",
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Coletamos dados de autenticação, telemetria operacional e mídias de missão para funcionamento do aplicativo.",
                color = colorScheme.onSurfaceVariant
            )

            Text(
                text = "\n2. Uso dos dados",
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Os dados são utilizados para execução de missões, geração de relatórios e melhoria da experiência operacional.",
                color = colorScheme.onSurfaceVariant
            )

            Text(
                text = "\n3. Armazenamento e retenção",
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Informações podem ser armazenadas localmente no dispositivo e, quando configurado, sincronizadas com backend autorizado.",
                color = colorScheme.onSurfaceVariant
            )

            Text(
                text = "\n4. Compartilhamento",
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Não compartilhamos dados com terceiros fora do escopo operacional e institucional do projeto.",
                color = colorScheme.onSurfaceVariant
            )

            Text(
                text = "\n5. Contato",
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )
            Text(
                text = "Para dúvidas sobre privacidade, entre em contato com a equipe responsável pelo projeto.",
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
