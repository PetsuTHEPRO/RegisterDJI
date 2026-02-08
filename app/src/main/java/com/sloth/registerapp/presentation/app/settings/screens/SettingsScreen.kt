package com.sloth.registerapp.presentation.app.settings.screens

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
import androidx.compose.ui.platform.LocalContext
import com.sloth.registerapp.core.settings.MediaStorageSettingsRepository
import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.features.mission.data.manager.MissionStorageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "Usuário",
    userEmail: String = "usuario@labubu.com",
    isLoggedIn: Boolean = false,
    selectedTheme: String = "Padrão do Sistema",
    onBackClick: () -> Unit = {},
    onChangeProfilePhoto: () -> Unit = {},
    onChangeUsername: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onRecentLogins: () -> Unit = {},
    onManagePermissions: () -> Unit = {},
    onAbout: () -> Unit = {},
    onPrivacyPolicy: () -> Unit = {},
    onThemeChange: (String) -> Unit = {},
    onLoginClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    // Estados
    var notifications by remember { mutableStateOf(true) }
    var streamQuality by remember { mutableStateOf("Automática") }
    var expandedSection by remember { mutableStateOf<String?>(null) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rtmpRepo = remember(context) { RtmpSettingsRepository.getInstance(context) }
    val mediaStorageRepo = remember(context) { MediaStorageSettingsRepository.getInstance(context) }
    val measurementRepo = remember(context) { MeasurementSettingsRepository.getInstance(context) }
    val missionStorageManager = remember(context) { MissionStorageManager.getInstance(context) }
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsState(initial = RtmpSettingsRepository.DEFAULT_URL)
    val mediaStorageTarget by mediaStorageRepo.mediaStorageTarget.collectAsState(initial = MediaStorageSettingsRepository.TARGET_PHONE)
    val measurementSystem by measurementRepo.measurementSystem.collectAsState(initial = MeasurementSettingsRepository.SYSTEM_METRIC)
    var rtmpUrlInput by remember { mutableStateOf(rtmpUrl) }

    LaunchedEffect(rtmpUrl) {
        if (rtmpUrlInput != rtmpUrl) {
            rtmpUrlInput = rtmpUrl
        }
    }

    LaunchedEffect(Unit) {
        cacheSizeBytes = missionStorageManager.getMissionCacheSizeBytes()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background,
                        colorScheme.surfaceVariant,
                        colorScheme.background
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
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            tint = colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Configurações",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoggedIn) {
                    // Perfil do Usuário
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = colorScheme.surface.copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.2f)),
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
                                    color = colorScheme.primary.copy(alpha = 0.2f),
                                    border = BorderStroke(3.dp, colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = userName.take(1).uppercase(),
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userName,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = userEmail,
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurfaceVariant
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
                                icon = Icons.Default.Lock,
                                title = "Alterar Senha",
                                subtitle = "••••••••",
                                onClick = onChangePassword
                            )
                            SettingsItem(
                                icon = Icons.Default.History,
                                title = "Últimos Logins",
                                subtitle = "Últimos logins",
                                onClick = onRecentLogins
                            )
                        }
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
                            options = listOf("Celular", "SD do Drone"),
                            selectedOption = mediaStorageTarget.toStorageLabel(),
                            onOptionSelected = { label ->
                                scope.launch {
                                    mediaStorageRepo.setMediaStorageTarget(label.toStorageTarget())
                                }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.CleaningServices,
                            title = "Limpar missões salvas no dispositivo",
                            subtitle = "Uso atual: ${cacheSizeBytes.toReadableBytes()}",
                            onClick = { showClearCacheDialog = true }
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
                            selectedOption = selectedTheme,
                            onOptionSelected = onThemeChange
                        )
                        SettingsDropdownItem(
                            icon = Icons.Default.Speed,
                            title = "Unidades de Medida",
                            options = listOf("Métrico (m, km/h)", "Imperial (ft, mph)"),
                            selectedOption = measurementSystem.toMeasurementLabel(),
                            onOptionSelected = { label ->
                                scope.launch {
                                    measurementRepo.setMeasurementSystem(label.toMeasurementSystem())
                                }
                            }
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
                    }
                }

                // Seção: Streaming RTMP
                item {
                    ExpandableSection(
                        title = "Streaming RTMP",
                        icon = Icons.Default.Cast,
                        isExpanded = expandedSection == "rtmp",
                        onToggle = { expandedSection = if (expandedSection == "rtmp") null else "rtmp" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = rtmpUrlInput,
                                onValueChange = { rtmpUrlInput = it },
                                label = { Text("URL RTMP") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        rtmpRepo.setRtmpUrl(rtmpUrlInput.trim())
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Salvar")
                            }
                            Text(
                                text = "Atual: $rtmpUrl",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                            onClick = onPrivacyPolicy
                        )
                    }
                }

                // Botão de Login/Logout
                item {
                    Button(
                        onClick = {
                            if (isLoggedIn) onLogout() else onLoginClick()
                        },
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
                                        colors = if (isLoggedIn) {
                                            listOf(colorScheme.error, colorScheme.errorContainer)
                                        } else {
                                            listOf(colorScheme.primary, colorScheme.primaryContainer)
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLoggedIn) Icons.Default.Logout else Icons.Default.Login,
                                    contentDescription = null,
                                    tint = if (isLoggedIn) colorScheme.onError else colorScheme.onPrimary
                                )
                                Text(
                                    text = if (isLoggedIn) "Sair da Conta" else "Realizar Login",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLoggedIn) colorScheme.onError else colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Limpar missões locais") },
            text = {
                Text(
                    "Isso remove as missões salvas neste dispositivo (${cacheSizeBytes.toReadableBytes()}). Esta ação não pode ser desfeita."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            missionStorageManager.clearMissionCache()
                            cacheSizeBytes = missionStorageManager.getMissionCacheSizeBytes()
                            showClearCacheDialog = false
                        }
                    }
                ) {
                    Text("Limpar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = if (isExpanded) 0.5f else 0.2f)),
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
                        color = colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = colorScheme.primary
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
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surface.copy(alpha = 0.4f)
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
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surface.copy(alpha = 0.4f)
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
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorScheme.onPrimary,
                    checkedTrackColor = colorScheme.primary,
                    uncheckedThumbColor = colorScheme.onSurface,
                    uncheckedTrackColor = colorScheme.outline
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
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surface.copy(alpha = 0.4f)
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
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = selectedOption,
                    fontSize = 12.sp,
                    color = colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == selectedOption) colorScheme.primary else colorScheme.onSurface
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

private fun String.toStorageLabel(): String {
    return when (this) {
        MediaStorageSettingsRepository.TARGET_DRONE_SD -> "SD do Drone"
        else -> "Celular"
    }
}

private fun String.toStorageTarget(): String {
    return if (this == "SD do Drone") {
        MediaStorageSettingsRepository.TARGET_DRONE_SD
    } else {
        MediaStorageSettingsRepository.TARGET_PHONE
    }
}

private fun String.toMeasurementLabel(): String {
    return if (this == MeasurementSettingsRepository.SYSTEM_IMPERIAL) {
        "Imperial (ft, mph)"
    } else {
        "Métrico (m, km/h)"
    }
}

private fun String.toMeasurementSystem(): String {
    return if (startsWith("Imperial")) {
        MeasurementSettingsRepository.SYSTEM_IMPERIAL
    } else {
        MeasurementSettingsRepository.SYSTEM_METRIC
    }
}

private fun Long.toReadableBytes(): String {
    if (this <= 0L) return "0 B"
    val kb = this / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.2f MB", mb)
    } else {
        String.format("%.1f KB", kb)
    }
}
