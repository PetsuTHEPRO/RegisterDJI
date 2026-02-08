package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
fun AboutScreen(
    onBackClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Sobre o aplicativo") },
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
                .padding(16.dp)
        ) {
            Text("Vantly Neural", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Versão 1.0.0", color = colorScheme.onSurfaceVariant)

            Text(
                text = "\nO Vantly Neural é uma plataforma para planejamento, execução e análise de missões com drones, com suporte a telemetria em tempo real e relatórios operacionais.",
                color = colorScheme.onSurface
            )

            Text(
                text = "\nEste projeto integra componentes de operação aérea e inteligência aplicada, visando aumentar segurança, rastreabilidade e eficiência em missões técnicas.",
                color = colorScheme.onSurface
            )

            Text(
                text = "\nDesenvolvido no contexto acadêmico com apoio institucional para pesquisa e inovação.",
                color = colorScheme.onSurface
            )
        }
    }
}
