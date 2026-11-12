package com.sloth.registerapp.presentation.app.settings.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import com.sloth.registerapp.BuildConfig
import com.sloth.registerapp.core.settings.MediaStorageSettingsRepository
import com.sloth.registerapp.core.settings.MeasurementSettingsRepository
import com.sloth.registerapp.core.settings.RtmpSettingsRepository
import com.sloth.registerapp.core.settings.WeatherProviderSettingsRepository
import com.sloth.registerapp.features.mission.data.manager.MissionStorageManager
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userName: String = "Usuário",
    userEmail: String = "usuario@email.com",
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
    val weatherProviderRepo = remember(context) { WeatherProviderSettingsRepository.getInstance(context) }
    val missionStorageManager = remember(context) { MissionStorageManager.getInstance(context) }
    val rtmpUrl by rtmpRepo.rtmpUrl.collectAsState(initial = RtmpSettingsRepository.DEFAULT_URL)
    val mediaStorageTarget by mediaStorageRepo.mediaStorageTarget.collectAsState(initial = MediaStorageSettingsRepository.TARGET_PHONE)
    val measurementSystem by measurementRepo.measurementSystem.collectAsState(initial = MeasurementSettingsRepository.SYSTEM_METRIC)
    val weatherPrimary by weatherProviderRepo.primaryProvider.collectAsState(initial = WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
    val weatherFallback by weatherProviderRepo.fallbackProvider.collectAsState(initial = WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
    var rtmpUrlInput by remember { mutableStateOf(rtmpUrl) }
    val hasWeatherApiKey = remember { BuildConfig.WEATHER_API_KEY.isNotBlank() }

    LaunchedEffect(rtmpUrl) {
        if (rtmpUrlInput != rtmpUrl) rtmpUrlInput = rtmpUrl
    }
    LaunchedEffect(Unit) {
        cacheSizeBytes = missionStorageManager.getMissionCacheSizeBytes()
    }
    LaunchedEffect(hasWeatherApiKey, weatherPrimary, weatherFallback) {
        if (!hasWeatherApiKey) {
            if (weatherPrimary == WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
                weatherProviderRepo.setPrimaryProvider(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
            if (weatherFallback == WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
                weatherProviderRepo.setFallbackProvider(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeuralColors.DashboardBg)
    ) {
        // Header padrão do sistema (alinhado a DashboardScreen e AiCenterScreen)
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
                    text = "Configurações",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = "Personalize o sistema",
                    fontSize = 11.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
            }
        }

        HorizontalDivider(color = NeuralColors.DashboardBorder)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
                if (isLoggedIn) {
                    item {
                        NeuralProfileCard(
                            userName = userName,
                            userEmail = userEmail,
                            onChangeProfilePhoto = onChangeProfilePhoto
                        )
                    }

                    item {
                        ExpandableSection(
                            title = "Conta e Segurança",
                            icon = Icons.Default.Security,
                            isExpanded = expandedSection == "account",
                            onToggle = { expandedSection = if (expandedSection == "account") null else "account" }
                        ) {
                            SettingsItem(Icons.Default.Person, "Alterar Nome de Usuário", userName, onChangeUsername)
                            SettingsItem(Icons.Default.Lock, "Alterar Senha", "••••••••", onChangePassword)
                            SettingsItem(Icons.Default.History, "Últimos Logins", "Histórico de acessos", onRecentLogins)
                        }
                    }
                }

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
                                scope.launch { mediaStorageRepo.setMediaStorageTarget(label.toStorageTarget()) }
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.CleaningServices,
                            title = "Limpar missões no dispositivo",
                            subtitle = "Uso atual: ${cacheSizeBytes.toReadableBytes()}",
                            onClick = { showClearCacheDialog = true }
                        )
                    }
                }

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
                                scope.launch { measurementRepo.setMeasurementSystem(label.toMeasurementSystem()) }
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

                item {
                    ExpandableSection(
                        title = "Configurações Climáticas",
                        icon = Icons.Default.Cloud,
                        isExpanded = expandedSection == "weather",
                        onToggle = { expandedSection = if (expandedSection == "weather") null else "weather" }
                    ) {
                        SettingsDropdownItem(
                            icon = Icons.Default.FilterDrama,
                            title = "Provider Principal",
                            options = weatherProviderOptions(hasWeatherApiKey),
                            selectedOption = weatherPrimary.toWeatherProviderLabel(),
                            onOptionSelected = { label ->
                                scope.launch {
                                    val selected = label.toWeatherProviderId()
                                    weatherProviderRepo.setPrimaryProvider(selected)
                                    if (selected == weatherFallback)
                                        weatherProviderRepo.setFallbackProvider(selected.alternativeWeatherProvider())
                                }
                            }
                        )
                        SettingsDropdownItem(
                            icon = Icons.Default.SwapHoriz,
                            title = "Provider Fallback",
                            options = weatherProviderOptions(hasWeatherApiKey),
                            selectedOption = weatherFallback.toWeatherProviderLabel(),
                            onOptionSelected = { label ->
                                scope.launch {
                                    val selected = label.toWeatherProviderId()
                                    weatherProviderRepo.setFallbackProvider(selected)
                                    if (selected == weatherPrimary)
                                        weatherProviderRepo.setPrimaryProvider(selected.alternativeWeatherProvider())
                                }
                            }
                        )
                        NeuralInfoText(
                            if (!hasWeatherApiKey)
                                "WeatherAPI desativada: chave não configurada. Usando Open-Meteo."
                            else
                                "Chave WeatherAPI detectada. Fallback ativado em caso de erro da WeatherAPI."
                        )
                    }
                }

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

                item {
                    ExpandableSection(
                        title = "Streaming RTMP",
                        icon = Icons.Default.Cast,
                        isExpanded = expandedSection == "rtmp",
                        onToggle = { expandedSection = if (expandedSection == "rtmp") null else "rtmp" }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = rtmpUrlInput,
                                onValueChange = { rtmpUrlInput = it },
                                label = { Text("URL RTMP", color = NeuralColors.DashboardTextSecondary) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = NeuralColors.TextPrimary,
                                    unfocusedTextColor = NeuralColors.TextPrimary,
                                    focusedBorderColor = NeuralColors.DashboardBlueLight,
                                    unfocusedBorderColor = NeuralColors.DashboardBorder,
                                    focusedContainerColor = NeuralColors.DashboardInner,
                                    unfocusedContainerColor = NeuralColors.DashboardInner,
                                    cursorColor = NeuralColors.DashboardBlueLight
                                )
                            )
                            Button(
                                onClick = { scope.launch { rtmpRepo.setRtmpUrl(rtmpUrlInput.trim()) } },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.DashboardBlue)
                            ) {
                                Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            NeuralInfoText("Atual: $rtmpUrl")
                        }
                    }
                }

                item {
                    ExpandableSection(
                        title = "Permissões e Sobre",
                        icon = Icons.Default.Info,
                        isExpanded = expandedSection == "about",
                        onToggle = { expandedSection = if (expandedSection == "about") null else "about" }
                    ) {
                        SettingsItem(Icons.Default.Security, "Gerenciar Permissões", "Configurações do sistema", onManagePermissions)
                        SettingsItem(Icons.Default.Info, "Sobre o Aplicativo", "Versão ${BuildConfig.VERSION_NAME}", onAbout)
                        SettingsItem(Icons.Default.Policy, "Política de Privacidade", "Termos e condições", onPrivacyPolicy)
                    }
                }

                item {
                    Button(
                        onClick = { if (isLoggedIn) onLogout() else onLoginClick() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoggedIn) NeuralColors.Danger.copy(alpha = 0.15f) else NeuralColors.DashboardBlue
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isLoggedIn) NeuralColors.Danger.copy(alpha = 0.40f) else NeuralColors.DashboardBlueLight.copy(alpha = 0.50f)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isLoggedIn) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = if (isLoggedIn) NeuralColors.Danger else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isLoggedIn) "Sair da Conta" else "Realizar Login",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLoggedIn) NeuralColors.Danger else Color.White
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = NeuralColors.DashboardCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeuralColors.Warning)
                    Spacer(Modifier.width(8.dp))
                    Text("Limpar missões locais", color = NeuralColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Remove as missões salvas neste dispositivo (${cacheSizeBytes.toReadableBytes()}). Esta ação não pode ser desfeita.",
                    color = NeuralColors.DashboardTextSecondary,
                    fontSize = 13.sp
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
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeuralColors.Danger)
                ) { Text("Limpar", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancelar", color = NeuralColors.DashboardTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun NeuralProfileCard(
    userName: String,
    userEmail: String,
    onChangeProfilePhoto: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.DashboardCard,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onChangeProfilePhoto,
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = NeuralColors.DashboardInner,
                border = BorderStroke(1.5.dp, NeuralColors.DashboardBlueLight.copy(alpha = 0.50f))
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = userName.take(1).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeuralColors.DashboardBlueLight
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeuralColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = userEmail,
                    fontSize = 12.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NeuralColors.Success.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, NeuralColors.Success.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(NeuralColors.Success, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "AUTENTICADO",
                            color = NeuralColors.Success,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = NeuralColors.DashboardCard,
        border = BorderStroke(
            1.dp,
            if (isExpanded) NeuralColors.DashboardBlueLight.copy(alpha = 0.40f) else NeuralColors.DashboardBorder
        )
    ) {
        Column {
            Surface(
                onClick = onToggle,
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = NeuralColors.DashboardInner,
                        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = NeuralColors.DashboardBlueLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuralColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NeuralColors.DashboardTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = NeuralColors.DashboardBorder)
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = NeuralColors.DashboardInner,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                color = NeuralColors.DashboardCard,
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

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = NeuralColors.DashboardTextSecondary,
                modifier = Modifier.size(16.dp)
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = NeuralColors.DashboardInner,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                color = NeuralColors.DashboardCard,
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

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NeuralColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = NeuralColors.DashboardTextSecondary
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = NeuralColors.DashboardBlue,
                    uncheckedThumbColor = NeuralColors.DashboardTextSecondary,
                    uncheckedTrackColor = NeuralColors.DashboardCard,
                    uncheckedBorderColor = NeuralColors.DashboardBorder
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

    Surface(
        onClick = { expanded = true },
        shape = RoundedCornerShape(8.dp),
        color = NeuralColors.DashboardInner,
        border = BorderStroke(1.dp, NeuralColors.DashboardBorder)
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = NeuralColors.DashboardCard,
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

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeuralColors.TextPrimary
                    )
                    Text(
                        text = selectedOption,
                        fontSize = 11.sp,
                        color = NeuralColors.DashboardBlueLight,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = NeuralColors.DashboardTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(NeuralColors.DashboardCard)
                    .border(BorderStroke(1.dp, NeuralColors.DashboardBorder), RoundedCornerShape(8.dp))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedOption) NeuralColors.DashboardBlueLight else NeuralColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
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

@Composable
private fun NeuralInfoText(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = NeuralColors.DashboardTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .background(NeuralColors.DashboardInner, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, NeuralColors.DashboardBorder), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

private fun Long.toReadableBytes(): String {
    if (this <= 0L) return "0 B"
    val kb = this / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.1f KB", kb)
}

private fun weatherProviderOptions(hasWeatherApiKey: Boolean): List<String> {
    return if (hasWeatherApiKey) {
        listOf(
            WeatherProviderSettingsRepository.PROVIDER_WEATHER_API.toWeatherProviderLabel(),
            WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO.toWeatherProviderLabel()
        )
    } else {
        listOf(WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO.toWeatherProviderLabel())
    }
}

private fun String.toWeatherProviderLabel(): String = when (this) {
    WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO -> "Open-Meteo (grátis)"
    else -> "WeatherAPI (principal)"
}

private fun String.toWeatherProviderId(): String = when {
    startsWith("Open-Meteo") -> WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO
    else -> WeatherProviderSettingsRepository.PROVIDER_WEATHER_API
}

private fun String.alternativeWeatherProvider(): String =
    if (this == WeatherProviderSettingsRepository.PROVIDER_WEATHER_API)
        WeatherProviderSettingsRepository.PROVIDER_OPEN_METEO
    else WeatherProviderSettingsRepository.PROVIDER_WEATHER_API

private fun String.toStorageLabel(): String = when (this) {
    MediaStorageSettingsRepository.TARGET_DRONE_SD -> "SD do Drone"
    else -> "Celular"
}

private fun String.toStorageTarget(): String =
    if (this == "SD do Drone") MediaStorageSettingsRepository.TARGET_DRONE_SD
    else MediaStorageSettingsRepository.TARGET_PHONE

private fun String.toMeasurementLabel(): String =
    if (this == MeasurementSettingsRepository.SYSTEM_IMPERIAL) "Imperial (ft, mph)" else "Métrico (m, km/h)"

private fun String.toMeasurementSystem(): String =
    if (startsWith("Imperial")) MeasurementSettingsRepository.SYSTEM_IMPERIAL
    else MeasurementSettingsRepository.SYSTEM_METRIC
