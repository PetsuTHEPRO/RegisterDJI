package com.sloth.registerapp.presentation.app.dashboard.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirplanemodeInactive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.R
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import com.sloth.registerapp.presentation.shared.components.EmptyStateView

data class CompatibleDrone(
    val name: String,
    val description: String,
    val imageRes: Int,
    val officialUrl: String,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DronesListScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    val drones = remember { defaultCompatibleDrones() }
    val filteredDrones = remember(query, drones) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) drones
        else drones.filter {
            it.name.lowercase().contains(normalized) ||
                it.description.lowercase().contains(normalized)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Drones Compatíveis",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${filteredDrones.size} modelos homologados",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeuralColors.TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NeuralColors.Bg,
                    scrolledContainerColor = NeuralColors.Surface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = NeuralColors.Bg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeuralColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Pesquisar drone por modelo ou linha...",
                            color = NeuralColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeuralColors.Primary,
                        unfocusedBorderColor = NeuralColors.Border,
                        focusedContainerColor = NeuralColors.Surface,
                        unfocusedContainerColor = NeuralColors.Surface,
                        cursorColor = Color.White
                    )
                )
            }

            if (filteredDrones.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.AirplanemodeInactive,
                        title = "Nenhum drone encontrado",
                        description = "Não localizamos modelos para \"$query\". Verifique o termo digitado.",
                        actionLabel = "Limpar busca",
                        onActionClick = { query = "" }
                    )
                }
            } else {
                items(filteredDrones, key = { it.name }) { drone ->
                    CompactDroneCard(
                        modifier = Modifier.fillMaxWidth(),
                        drone = drone,
                        onClick = {
                            Toast.makeText(
                                context,
                                "Especificações oficiais DJI para ${drone.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactDroneCard(
    drone: CompatibleDrone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(1.dp, NeuralColors.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(8.dp),
                color = NeuralColors.SurfaceAlt,
                border = BorderStroke(1.dp, NeuralColors.Border)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = drone.imageRes),
                        contentDescription = drone.name,
                        modifier = Modifier.size(40.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drone.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = drone.description,
                    color = NeuralColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeuralColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

fun defaultCompatibleDrones(): List<CompatibleDrone> = listOf(
    compatibleDrone(
        name = "DJI Mavic Air",
        description = "Drone compacto da linha Air, focado em mobilidade e captura aérea leve.",
        accentColor = NeuralColors.Primary
    ),
    compatibleDrone(
        name = "DJI Mavic Air 2",
        description = "Modelo Air com foco em autonomia, estabilidade e uso geral de campo.",
        accentColor = NeuralColors.Secondary
    ),
    compatibleDrone(
        name = "DJI Mavic Air 2S",
        description = "Versão Air com sensor superior e perfil mais forte para imagem aérea.",
        accentColor = NeuralColors.Success
    ),
    compatibleDrone("DJI Air 3", "Air de nova geração com câmera dupla e operação mais versátil.", NeuralColors.Primary),
    compatibleDrone("DJI Mavic Mini", "Mini ultraleve para voo recreativo e missões visuais simples.", NeuralColors.Secondary),
    compatibleDrone("DJI Mini 2", "Mini portátil com melhor estabilidade e alcance para uso leve.", NeuralColors.Success),
    compatibleDrone("DJI Mini 3", "Mini com câmera otimizada e foco em operação portátil.", NeuralColors.Primary),
    compatibleDrone("DJI Mini 3 Pro", "Mini avançado com recursos premium de imagem e segurança.", NeuralColors.Secondary),
    compatibleDrone("DJI Mini 4 Pro", "Mini topo de linha para operação compacta com mais sensores.", NeuralColors.Success),
    compatibleDrone("DJI MAPIR", "Plataforma voltada para mapeamento e captura especializada.", NeuralColors.Primary),
    compatibleDrone("DJI Mavic Pro", "Clássico dobrável da linha Mavic para operação geral.", NeuralColors.Secondary),
    compatibleDrone("DJI Mavic 2 Pro", "Mavic com foco em imagem de maior qualidade e uso profissional.", NeuralColors.Success),
    compatibleDrone("DJI Mavic 2 Zoom", "Versão com câmera zoom para inspeção e observação.", NeuralColors.Primary),
    compatibleDrone("DJI Mavic 2 Enterprise Dual", "Enterprise com câmera dual para operação técnica.", NeuralColors.Secondary),
    compatibleDrone("DJI Mavic 2 Enterprise Advanced", "Enterprise avançado com foco em inspeção e resposta.", NeuralColors.Success),
    compatibleDrone("DJI Mavic 3 / DJI Mavic 3 Pro", "Linha Mavic 3 para captura avançada e missões premium.", NeuralColors.Primary),
    compatibleDrone("DJI Mavic 3 Multispectral", "Modelo voltado para agricultura e análise multiespectral.", NeuralColors.Secondary),
    compatibleDrone("DJI Mavic 3 Enterprise - 3E", "Enterprise com foco em mapeamento e inspeção corporativa.", NeuralColors.Success),
    compatibleDrone("DJI Mavic 3 Thermal - 3T", "Versão térmica para segurança, busca e inspeção.", NeuralColors.Primary),
    compatibleDrone("DJI Phantom 3", "Plataforma clássica Phantom para captura aérea e treino.", NeuralColors.Secondary),
    compatibleDrone("DJI Phantom 3 Advanced", "Phantom 3 com pacote avançado para operação estável.", NeuralColors.Success),
    compatibleDrone("DJI Phantom 3 Pro", "Versão Pro da linha Phantom 3 com perfil mais robusto.", NeuralColors.Primary),
    compatibleDrone("DJI Phantom 4", "Plataforma amplamente usada em mapeamento e inspeção.", NeuralColors.Secondary),
    compatibleDrone("DJI Phantom 4 Pro", "Phantom 4 com câmera melhorada para uso profissional.", NeuralColors.Success),
    compatibleDrone("DJI Phantom 4 Pro V2", "Refinamento da linha Phantom 4 Pro para uso técnico.", NeuralColors.Primary),
    compatibleDrone("DJI Phantom 4 RTK", "Modelo RTK para mapeamento preciso e georreferenciamento.", NeuralColors.Secondary),
    compatibleDrone("DJI Phantom 4 Multispectral", "Plataforma Phantom voltada para agricultura de precisão.", NeuralColors.Success),
    compatibleDrone("DJI Spark", "Drone compacto e leve para captura rápida e uso recreativo.", NeuralColors.Primary),
    compatibleDrone("DJI Matrice 100", "Plataforma Matrice modular para pesquisa e desenvolvimento.", NeuralColors.Secondary),
    compatibleDrone("DJI Matrice 200", "Matrice industrial para inspeção e missões corporativas.", NeuralColors.Success),
    compatibleDrone("DJI Matrice 200 V2", "Revisão V2 com foco em confiabilidade operacional.", NeuralColors.Primary),
    compatibleDrone("DJI Matrice 210", "Versão com suporte ampliado a cargas e sensores.", NeuralColors.Secondary),
    compatibleDrone("DJI Matrice 210 RTK", "Matrice com RTK para missões que exigem precisão.", NeuralColors.Success),
    compatibleDrone("DJI Matrice 210 V2", "Atualização da linha 210 com foco em estabilidade industrial.", NeuralColors.Primary),
    compatibleDrone("DJI Matrice 210 V2 RTK", "Variação RTK da linha 210 V2 para mapeamento técnico.", NeuralColors.Secondary),
    compatibleDrone("DJI Matrice 300 RTK", "Plataforma enterprise robusta para inspeção avançada.", NeuralColors.Success),
    compatibleDrone("DJI Matrice 30T", "Drone compacto enterprise com sensor térmico integrado.", NeuralColors.Primary),
    compatibleDrone("DJI Matrice 600", "Hexacóptero para cargas maiores e aplicações especializadas.", NeuralColors.Secondary),
    compatibleDrone("DJI Matrice 4 / 4T", "Linha enterprise recente para inspeção e operações técnicas.", NeuralColors.Success)
)

private fun compatibleDrone(
    name: String,
    description: String,
    accentColor: Color
): CompatibleDrone {
    return CompatibleDrone(
        name = name,
        description = description,
        imageRes = compatibleDroneImageRes(name),
        officialUrl = "https://www.dji.com/br",
        accentColor = accentColor
    )
}

private fun compatibleDroneImageRes(name: String): Int {
    val normalized = name.lowercase()
    return when {
        normalized == "dji air 3" -> R.drawable.drone_dji_air_3
        normalized == "dji mavic air" -> R.drawable.drone_dji_mavic_air
        normalized == "dji mavic air 2" -> R.drawable.drone_dji_mavic_air_2
        normalized == "dji mavic air 2s" -> R.drawable.drone_dji_mavic_air_2s
        normalized == "dji mavic mini" -> R.drawable.drone_dji_mavic_mini
        "matrice" in normalized -> R.drawable.ic_catalog_drone_matrice
        "phantom" in normalized -> R.drawable.ic_catalog_drone_phantom
        "air" in normalized || "mini" in normalized || "spark" in normalized -> R.drawable.ic_catalog_drone_air
        else -> R.drawable.ic_catalog_drone_mavic
    }
}
