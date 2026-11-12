package com.sloth.registerapp.presentation.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Esquema escuro principal do Vantly Neural (estilo DJI).
 * Preto/cinza/branco como base; verde, âmbar e vermelho somente para estados.
 */
val DarkColorScheme = darkColorScheme(
    // Base
    background        = NeuralColors.Bg,
    onBackground      = NeuralColors.TextPrimary,

    // Primária neutra (branco) — ações principais com alto contraste
    primary           = NeuralColors.Primary,
    onPrimary         = NeuralColors.OnPrimary,
    primaryContainer  = NeuralColors.SurfaceElevated,
    onPrimaryContainer = NeuralColors.TextPrimary,

    // Secundária — verde semântico (pronto / conectado). Container neutro
    // para que botões tonais permaneçam cinza.
    secondary         = NeuralColors.Success,
    onSecondary       = Color(0xFF00210A),
    secondaryContainer = NeuralColors.SurfaceElevated,
    onSecondaryContainer = NeuralColors.TextPrimary,

    // Terciária — âmbar (atenção)
    tertiary          = NeuralColors.Warning,
    onTertiary        = Color(0xFF2B1A00),
    tertiaryContainer = Color(0xFF3A2A0A),
    onTertiaryContainer = Color(0xFFFFE2A8),

    // Erro / ações críticas
    error             = NeuralColors.Danger,
    onError           = Color.White,
    errorContainer    = Color(0xFF3B1512),
    onErrorContainer  = Color(0xFFFFD6D2),

    // Superfícies
    surface           = NeuralColors.Surface,
    onSurface         = NeuralColors.TextPrimary,
    surfaceVariant    = NeuralColors.SurfaceAlt,
    onSurfaceVariant  = NeuralColors.TextSecondary,
    surfaceContainerLowest = NeuralColors.Bg,
    surfaceContainerLow    = NeuralColors.BgSoft,
    surfaceContainer       = NeuralColors.Surface,
    surfaceContainerHigh   = NeuralColors.SurfaceAlt,
    surfaceContainerHighest= NeuralColors.SurfaceElevated,
    surfaceBright     = NeuralColors.SurfaceElevated,
    surfaceDim        = NeuralColors.BgSoft,

    // Contornos
    outline           = NeuralColors.BorderStrong,
    outlineVariant    = NeuralColors.Border,

    // Inversas (snackbars / tooltips)
    inverseSurface    = Color(0xFFF2F2F2),
    inverseOnSurface  = Color(0xFF111111),
    inversePrimary    = Color(0xFF111111),

    // Sem tinta colorida nas superfícies elevadas
    surfaceTint       = Color.Transparent,
    scrim             = Color(0xB3000000),
)
