package com.sloth.registerapp.presentation.app.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Esquema claro — mesma identidade neutra (preto/cinza/branco) invertida.
 * Mantido para respeitar a preferência de tema do usuário.
 */
val LightColorScheme = lightColorScheme(
    background        = Color(0xFFF4F4F4),
    onBackground      = Color(0xFF0A0A0A),

    primary           = Color(0xFF111111),
    onPrimary         = Color.White,
    primaryContainer  = Color(0xFFE6E6E6),
    onPrimaryContainer = Color(0xFF111111),

    secondary         = Color(0xFF1E9E3A),
    onSecondary       = Color.White,
    secondaryContainer = Color(0xFFE6E6E6),
    onSecondaryContainer = Color(0xFF111111),

    tertiary          = Color(0xFFB7791F),
    onTertiary        = Color.White,
    tertiaryContainer = Color(0xFFFCEFD6),
    onTertiaryContainer = Color(0xFF4A3000),

    error             = Color(0xFFD92D20),
    onError           = Color.White,
    errorContainer    = Color(0xFFFDE3E1),
    onErrorContainer  = Color(0xFF5A0F0A),

    surface           = Color.White,
    onSurface         = Color(0xFF0A0A0A),
    surfaceVariant    = Color(0xFFF0F0F0),
    onSurfaceVariant  = Color(0xFF5C5C5C),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow    = Color(0xFFFAFAFA),
    surfaceContainer       = Color(0xFFF4F4F4),
    surfaceContainerHigh   = Color(0xFFECECEC),
    surfaceContainerHighest= Color(0xFFE2E2E2),

    outline           = Color(0xFFCFCFCF),
    outlineVariant    = Color(0xFFE3E3E3),

    inverseSurface    = Color(0xFF141414),
    inverseOnSurface  = Color.White,
    inversePrimary    = Color(0xFFF5F5F5),

    surfaceTint       = Color.Transparent,
    scrim             = Color(0x99000000),
)
