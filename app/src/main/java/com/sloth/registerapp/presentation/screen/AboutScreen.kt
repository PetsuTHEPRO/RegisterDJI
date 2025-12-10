package com.sloth.registerapp.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.presentation.theme.IFMAProjectTheme

// --- COMPONENTE REUTILIZÁVEL PARA MEMBROS DA EQUIPE ---
@Composable
fun TeamMemberCard(name: String, role: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone representando o membro
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .
                    background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = role,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Nome e Função
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// --- TELA PRINCIPAL "SOBRE" ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre o Projeto", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Seção 1: Descrição do Projeto
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Motivação do Projeto",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Este projeto visa desenvolver uma solução tecnológica para a detecção e monitoramento de pessoas em áreas remotas ou de difícil acesso. Utilizando drones e inteligência artificial, buscamos aumentar a segurança e a eficiência em operações de busca e salvamento, vigilância de fronteiras e monitoramento ambiental.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Justify
                        )
                    }
                }
            }

            // Seção 2: Créditos da Equipe
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Equipe do Projeto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Lista de Membros
            item {
                TeamMemberCard(
                    name = "Daniel Lima",
                    role = "Coordenador",
                    icon = Icons.Default.School // Ícone para Coordenador/Professor
                )
            }
            item {
                TeamMemberCard(
                    name = "Helder",
                    role = "Orientador",
                    icon = Icons.Default.MenuBook // Ícone para Orientador
                )
            }
            item {
                TeamMemberCard(
                    name = "Francisco Rafael",
                    role = "Bolsista",
                    icon = Icons.Default.Code // Ícone para Desenvolvedor/Bolsista
                )
            }
            item {
                TeamMemberCard(
                    name = "José Peterson",
                    role = "Voluntário",
                    icon = Icons.Default.Handshake // Ícone para Voluntário/Colaborador
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    IFMAProjectTheme {
        AboutScreen()
    }
}
