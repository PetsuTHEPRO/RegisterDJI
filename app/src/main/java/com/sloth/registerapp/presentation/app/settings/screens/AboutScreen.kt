package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.presentation.app.theme.NeuralColors

@Composable
fun AboutScreen(
    onBackClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.DashboardBg)
    ) {
        // Header padrão do sistema
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(NeuralColors.DashboardBg)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onBackClick,
                shape = RoundedCornerShape(8.dp),
                color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sobre o Aplicativo",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = "Versão ${BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
            }
        }

        HorizontalDivider(color = NeuralColors.DashboardBorder)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Identity Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = NeuralColors.DashboardInner,
                        border = BorderStroke(1.dp, NeuralColors.DashboardBlueLight.copy(alpha = 0.35f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = NeuralColors.DashboardBlueLight,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Vantly Neural",
                        color = NeuralColors.TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        color = NeuralColors.DashboardTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NeuralColors.Success.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, NeuralColors.Success.copy(alpha = 0.30f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(NeuralColors.Success, CircleShape)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Plataforma Operacional de Drones",
                                color = NeuralColors.Success,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Description Cards
            AboutInfoCard(
                icon = Icons.Default.Info,
                title = "O Projeto",
                body = "O Vantly Neural é uma plataforma avançada para planejamento, execução e análise de missões com drones. Integra telemetria em tempo real, visão computacional com IA embarcada e relatórios operacionais completos."
            )

            AboutInfoCard(
                icon = Icons.Default.AirplanemodeActive,
                title = "Funcionalidades",
                body = "Controle de drones DJI via SDK, planejamento de rotas com waypoints no mapa, detecção de pessoas via IA, transmissão de vídeo ao vivo (RTMP), monitoramento climático e geração de relatórios em PDF."
            )

            AboutInfoCard(
                icon = Icons.Default.Science,
                title = "Contexto Acadêmico",
                body = "Desenvolvido no contexto acadêmico com apoio institucional do IFMA e FAPEMA, com foco em pesquisa aplicada, inovação tecnológica e desenvolvimento de sistemas críticos de missão."
            )

            AboutInfoCard(
                icon = Icons.Default.Business,
                title = "Apoio Institucional",
                body = "Instituto Federal do Maranhão (IFMA) e Fundação de Amparo à Pesquisa e ao Desenvolvimento Científico e Tecnológico do Maranhão (FAPEMA)."
            )

            // Tech tags
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tecnologias",
                        color = NeuralColors.DashboardTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Jetpack Compose", "DJI SDK", "Mapbox", "Kotlin").forEach { tech ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeuralColors.DashboardInner,
                                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                            ) {
                                Text(
                                    text = tech,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    color = NeuralColors.DashboardBlueLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AboutInfoCard(icon: ImageVector, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.DashboardCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = NeuralColors.DashboardInner,
                    border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = NeuralColors.DashboardBlueLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    color = NeuralColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = body,
                color = NeuralColors.DashboardTextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
