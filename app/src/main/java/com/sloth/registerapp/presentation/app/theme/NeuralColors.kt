package com.sloth.registerapp.presentation.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de cores centralizada do Vantly Neural.
 *
 * Identidade inspirada em aplicativos profissionais de drones (estilo DJI):
 * predominância de preto, cinza e branco. Cores semânticas (verde, âmbar e
 * vermelho) ficam reservadas exclusivamente para estados operacionais.
 *
 * Os nomes históricos (Primary, Secondary, Muted...) foram preservados para
 * manter a compatibilidade com todas as telas existentes.
 */
object NeuralColors {
    // Paleta do painel Início, compartilhada com a Central de IA.
    val DashboardBg = Color(0xFF0B111A)
    val DashboardCard = Color(0xFF111827)
    val DashboardInner = Color(0xFF151E2E)
    val DashboardBorder = Color(0xFF1F2937)
    val DashboardBlue = Color(0xFF1D4ED8)
    val DashboardTextSecondary = Color(0xFF94A3B8)
    // Fundos
    val Bg              = Color(0xFF000000)   // fundo principal
    val BgSoft          = Color(0xFF080808)   // fundo quase preto
    val Surface         = Color(0xFF141414)   // superfícies principais
    val SurfaceAlt      = Color(0xFF1E1E1E)   // cards secundários
    val SurfaceElevated = Color(0xFF262626)   // superfícies elevadas
    val SurfaceOverlay  = Color(0xD9141414)   // painéis sobrepostos ao mapa / vídeo

    // Bordas
    val Border          = Color(0xFF2A2A2A)
    val BorderStrong    = Color(0xFF343434)
    val BorderGlow      = Color(0x33FFFFFF)

    // Ações primárias neutras (alto contraste)
    val Primary         = Color(0xFFF5F5F5)   // branco suave — ação/destaque
    val OnPrimary       = Color(0xFF0A0A0A)   // conteúdo sobre ação primária
    val Secondary       = Color(0xFFBDBDBD)   // cinza claro
    val Accent          = Color(0xFFE0E0E0)

    // Semânticas — apenas estados operacionais
    val Success         = Color(0xFF32D74B)   // conectado / concluído / pronto
    val Warning         = Color(0xFFFFB020)   // atenção
    val Danger          = Color(0xFFFF453A)   // erro / falha / ação crítica
    val Info            = Color(0xFFD4D4D4)   // informação neutra

    // Texto
    val TextPrimary     = Color(0xFFFFFFFF)
    val TextSecondary   = Color(0xFFA3A3A3)
    val TextDisabled    = Color(0xFF686868)
    val Muted           = Color(0xFFA3A3A3)

    // Extras (HUD / mapas)
    val CardGlow        = Color(0xFF1A1A1A)
    val GridLine        = Color(0x0FFFFFFF)
    val HudBackground   = Color(0xB3000000)
    val HudGlass        = Color(0x99141414)
}
