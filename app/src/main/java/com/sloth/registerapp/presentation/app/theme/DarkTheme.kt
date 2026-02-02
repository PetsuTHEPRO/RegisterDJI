package com.sloth.registerapp.presentation.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme(
    // ORIGINAL (comentado para referência)
    // primary = Color(0xFF4EA1FF), // azul neon
    // onPrimary = Color(0xEBFFFFFF), // texto principal
    // secondary = Color(0xFF22C55E), // destaque secundário
    // onSecondary = Color(0xFF0B1220),
    // secondaryContainer = Color(0xFF14532D),
    // onSecondaryContainer = Color(0xFFDCFCE7),
    // tertiary = Color(0xFFF59E0B), // aviso
    // onTertiary = Color(0xFF0B1220),
    // tertiaryContainer = Color(0xFF78350F),
    // onTertiaryContainer = Color(0xFFFFEDD5),
    // background = Color(0xFF0B1220), // fundo base
    // onBackground = Color(0xEBFFFFFF),
    // surface = Color(0x0FFFFFFF), // card
    // surfaceVariant = Color(0x14FFFFFF),
    // onSurface = Color(0xEBFFFFFF),
    // error = Color(0xFFEF4444),
    // onError = Color(0xFFFFFFFF),
    // outline = Color(0x1AFFFFFF),

    // VERSÃO FINAL (ativa)
    // Base
    background = Color(0xFF0B1220),
    onBackground = Color(0xEBFFFFFF),

    // Primárias
    primary = Color(0xFF4EA1FF),
    onPrimary = Color(0xEBFFFFFF),

    // Success / Warning / Danger
    secondary = Color(0xFF22C55E),
    onSecondary = Color(0xEBFFFFFF),
    tertiary = Color(0xFFF59E0B),
    onTertiary = Color(0xEBFFFFFF),
    error = Color(0xFFEF4444),
    onError = Color(0xEBFFFFFF),

    // Superfícies e bordas
    surface = Color(0xFF101826),        // cards escuros (harmonizado com o fundo)
    surfaceVariant = Color(0xFF121C2C), // variação um pouco mais clara
    onSurface = Color(0xEBFFFFFF),      // rgba(255,255,255,0.92)
    onSurfaceVariant = Color(0xB3FFFFFF), // rgba(255,255,255,0.70)
    outline = Color(0x1AFFFFFF),        // rgba(255,255,255,0.10)
    outlineVariant = Color(0x8CFFFFFF), // rgba(255,255,255,0.55)

    // Containers (usados em barras/áreas de destaque)
    primaryContainer = Color(0xFF14223A),
    onPrimaryContainer = Color(0xEBFFFFFF),
    secondaryContainer = Color(0xFFFF0000),
    onSecondaryContainer = Color(0xFFFF0000),
    tertiaryContainer = Color(0xFFFF0000),
    onTertiaryContainer = Color(0xFFFF0000),
    errorContainer = Color(0xFFFF0000),
    onErrorContainer = Color(0xFFFF0000),
    inverseSurface = Color(0xFFFF0000),
    inverseOnSurface = Color(0xFFFF0000),
    inversePrimary = Color(0xFFFF0000),
    surfaceTint = Color(0xFFFF0000),
    scrim = Color(0xFFFF0000),
    // Extras customizados (não nativos do Material3)
)
