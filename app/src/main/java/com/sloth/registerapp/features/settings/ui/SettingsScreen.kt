package com.sloth.registerapp.features.mission.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "Usuário",
    userEmail: String = "usuario@email.com",
    onBackClick: () -> Unit = {},
    onChangeProfilePhoto: () -> Unit = {},
    onChangeUsername: () -> Unit = {},
    onChangeEmail: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onEnable2FA: () -> Unit = {},
    onActivityHistory: () -> Unit = {},
    onManagePermissions: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // Cores do tema
    val primaryBlue = Color(0xFF3B82F6)
    val darkBlue = Color(0xFF1D4ED8)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)
    val textGray = Color(0xFF94A3B8)
    val textWhite = Color(0xFFE2E8F0)

    // Estados
    var theme by remember { mutableStateOf("Escuro") }
    var units by remember { mutableStateOf("Métrico") }
    var notifications by remember { mutableStateOf(true) }
    var twoFactorEnabled by remember { mutableStateOf(false) }
    var wifiOnlySync by remember { mutableStateOf(true) }
    var lowResDownload by remember { mutableStateOf(false) }
    var autoCleanCache by remember { mutableStateOf(true) }
    var streamQuality by remember { mutableStateOf("Automática") }
    var showGrid by remember { mutableStateOf(false) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        darkBg,
                        Color(0xFF1A1F3A),
                        darkBg
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(cardBg.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = textWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Configurações",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Perfil do Usuário
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg.copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.2f)),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Surface(
                                onClick = onChangeProfilePhoto,
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = primaryBlue.copy(alpha = 0.2f),
                                border = BorderStroke(3.dp, primaryBlue.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = userName.take(1).uppercase(),
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryBlue
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textWhite
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = userEmail,
                                    fontSize = 14.sp,
                                    color = textGray
                                )
                            }

                            IconButton(onClick = onChangeProfilePhoto) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar perfil",
                                    tint = primaryBlue
                                )
                            }
                        }
                    }
                }

                // Seção: Conta e Segurança
                item {
                    ExpandableSection(
                        title = "Conta e Segurança",
                        icon = Icons.Default.Security,
                        isExpanded = expandedSection == "account",
                        onToggle = { expandedSection = if (expandedSection == "account") null else "account" }
                    ) {
                        SettingsItem(
                            icon = Icons.Default.Person,
                            title = "Alterar Nome de Usuário",
                            subtitle = userName,
                            onClick = onChangeUsername
                        )
                        SettingsItem(
                            icon = Icons.Default.Email,
                            title = "Alterar E-mail",
                            subtitle = userEmail,
                            onClick = onChangeEmail
                        )
                        SettingsItem(
                            icon = Icons.Default.Lock,
                            title = "Alterar Senha",
                            subtitle = "••••••••",
                            onClick = onChangePassword
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.Shield,
                            title = "Autenticação de Dois Fatores",
                            subtitle = if (twoFactorEnabled) "Ativada" else "Desativada",
                            checked = twoFactorEnabled,
                            onCheckedChange = { 
                                twoFactorEnabled = it
                                if (it) onEnable2FA()
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.History,
                            title = "Histórico de Atividade",
                            subtitle = "Últimos logins",
                            onClick = onActivityHistory
                        )
                    }
                }

                // Seção: Armazenamento e Mídia
                item {
                    ExpandableSection(
                        title = "Armazenamento e Mídia",
                        icon = Icons.Default.Storage,
                        isExpanded = expandedSection == "storage",
                        onToggle = { expandedSection = if (expandedSection == "storage") null else "storage" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.Folder,
                            title = "Local de Salvamento",
                            options = listOf("Memória Interna", "Cartão SD do Drone"),
                            selectedOption = "Memória Interna",
                            onOptionSelected = { }
                        )
                        SettingsItem(
                            icon = Icons.Default.CleaningServices,
                            title = "Limpar Cache Agora",
                            subtitle = "Liberar 250 MB",
                            onClick = { }
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.AutoDelete,
                            title = "Auto-limpeza de Cache",
                            subtitle = "Apagar itens com mais de 30 dias",
                            checked = autoCleanCache,
                            onCheckedChange = { autoCleanCache = it }
                        )
                        SettingsDropdownItem(
                            icon = Icons.Default.DataUsage,
                            title = "Limite de Cache",
                            options = listOf("500 MB", "1 GB", "2 GB", "5 GB"),
                            selectedOption = "1 GB",
                            onOptionSelected = { }
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.Wifi,
                            title = "Sincronizar via Wi-Fi",
                            subtitle = "Economizar dados móveis",
                            checked = wifiOnlySync,
                            onCheckedChange = { wifiOnlySync = it }
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.HighQuality,
                            title = "Baixar em Baixa Resolução",
                            subtitle = "Economizar espaço",
                            checked = lowResDownload,
                            onCheckedChange = { lowResDownload = it }
                        )
                    }
                }

                // Seção: Configurações Gerais
                item {
                    ExpandableSection(
                        title = "Configurações Gerais",
                        icon = Icons.Default.Tune,
                        isExpanded = expandedSection == "general",
                        onToggle = { expandedSection = if (expandedSection == "general") null else "general" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.Palette,
                            title = "Tema do Aplicativo",
                            options = listOf("Claro", "Escuro", "Padrão do Sistema"),
                            selectedOption = theme,
                            onOptionSelected = { theme = it }
                        )
                        SettingsDropdownItem(
                            icon = Icons.Default.Speed,
                            title = "Unidades de Medida",
                            options = listOf("Métrico (m, km/h)", "Imperial (ft, mph)"),
                            selectedOption = units,
                            onOptionSelected = { units = it }
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.Notifications,
                            title = "Notificações",
                            subtitle = if (notifications) "Ativadas" else "Desativadas",
                            checked = notifications,
                            onCheckedChange = { notifications = it }
                        )
                    }
                }

                // Seção: Configurações da Câmera
                item {
                    ExpandableSection(
                        title = "Configurações da Câmera",
                        icon = Icons.Default.Videocam,
                        isExpanded = expandedSection == "camera",
                        onToggle = { expandedSection = if (expandedSection == "camera") null else "camera" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.VideoSettings,
                            title = "Qualidade da Transmissão",
                            options = listOf("Automática", "Alta Definição (HD)", "Fluida"),
                            selectedOption = streamQuality,
                            onOptionSelected = { streamQuality = it }
                        )
                        SettingsSwitchItem(
                            icon = Icons.Default.Grid3x3,
                            title = "Exibir Grade na Tela",
                            subtitle = "Regra dos Terços",
                            checked = showGrid,
                            onCheckedChange = { showGrid = it }
                        )
                    }
                }

                // Seção: Permissões e Sobre
                item {
                    ExpandableSection(
                        title = "Permissões e Sobre",
                        icon = Icons.Default.Info,
                        isExpanded = expandedSection == "about",
                        onToggle = { expandedSection = if (expandedSection == "about") null else "about" }
                    ) {
                        SettingsItem(
                            icon = Icons.Default.Security,
                            title = "Gerenciar Permissões",
                            subtitle = "Configurações do sistema",
                            onClick = onManagePermissions
                        )
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Sobre o Aplicativo",
                            subtitle = "Versão 1.0.0",
                            onClick = onAbout
                        )
                        SettingsItem(
                            icon = Icons.Default.Policy,
                            title = "Política de Privacidade",
                            subtitle = "Termos e condições",
                            onClick = { }
                        )
                    }
                }

                // Botão de Logout
                item {
                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Text(
                                    text = "Sair da Conta",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun ExpandableSection(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val primaryBlue = Color(0xFF3B82F6)
    val cardBg = Color(0xFF0F1729)
    val textWhite = Color(0xFFE2E8F0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, primaryBlue.copy(alpha = if (isExpanded) 0.5f else 0.2f)),
        shadowElevation = if (isExpanded) 8.dp else 4.dp
    ) {
        Column {
            // Header
            Surface(
                onClick = onToggle,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = primaryBlue.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = primaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textWhite,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = primaryBlue
                    )
                }
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val cardBg = Color(0xFF0F1729)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = cardBg.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textGray
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = textGray.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val cardBg = Color(0xFF0F1729)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textGray
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3B82F6),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF475569)
                )
            )
        }
    }
}

@Composable
fun SettingsDropdownItem(
    icon: ImageVector,
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val cardBg = Color(0xFF0F1729)

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(12.dp),
        color = cardBg.copy(alpha = 0.4f)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textWhite
                    )
                    Text(
                        text = selectedOption,
                        fontSize = 12.sp,
                        color = Color(0xFF3B82F6)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = textGray,
                    modifier = Modifier.size(24.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(cardBg)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedOption) Color(0xFF3B82F6) else textWhite
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}