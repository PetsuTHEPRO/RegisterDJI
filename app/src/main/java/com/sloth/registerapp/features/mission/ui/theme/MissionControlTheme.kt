package com.sloth.registerapp.features.mission.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tema de cores centralizado para MissionControlScreen.
 * Evita duplicação de magic colors e facilita manutenção e testes.
 */
object MissionControlTheme {
    // Cores primárias
    val primaryBlue = Color(0xFF3B82F6)
    val darkBg = Color(0xFF0A0E27)
    val cardBg = Color(0xFF0F1729)

    // Cores de status
    val textWhite = Color(0xFFE2E8F0)
    val textGray = Color(0xFF94A3B8)
    val greenOnline = Color(0xFF22C55E)
    val redDanger = Color(0xFFEF4444)
    val yellowWarning = Color(0xFFF59E0B)
}
