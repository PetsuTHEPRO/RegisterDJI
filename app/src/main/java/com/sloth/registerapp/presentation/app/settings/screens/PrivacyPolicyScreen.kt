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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.presentation.app.theme.NeuralColors

@Composable
fun PrivacyPolicyScreen(
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
                    text = "Política de Privacidade",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = "Termos e condições de uso",
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = NeuralColors.DashboardCard,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = NeuralColors.DashboardInner,
                        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = NeuralColors.DashboardBlueLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Documento Operacional — Versão 1.0",
                        color = NeuralColors.DashboardBlueLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            PolicySection(
                number = "1",
                title = "Coleta de Dados",
                body = "Coletamos dados de autenticação, telemetria operacional e mídias de missão para funcionamento do aplicativo."
            )
            PolicySection(
                number = "2",
                title = "Uso dos Dados",
                body = "Os dados são utilizados para execução de missões, geração de relatórios e melhoria da experiência operacional."
            )
            PolicySection(
                number = "3",
                title = "Armazenamento e Retenção",
                body = "Informações podem ser armazenadas localmente no dispositivo e, quando configurado, sincronizadas com backend autorizado."
            )
            PolicySection(
                number = "4",
                title = "Compartilhamento",
                body = "Não compartilhamos dados com terceiros fora do escopo operacional e institucional do projeto."
            )
            PolicySection(
                number = "5",
                title = "Contato",
                body = "Para dúvidas sobre privacidade, entre em contato com a equipe responsável pelo projeto."
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PolicySection(number: String, title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = NeuralColors.DashboardCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = RoundedCornerShape(6.dp),
                color = NeuralColors.DashboardInner,
                border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = number,
                        color = NeuralColors.DashboardBlueLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NeuralColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    color = NeuralColors.DashboardTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
