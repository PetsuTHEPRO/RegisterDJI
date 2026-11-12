package com.sloth.registerapp.presentation.app.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Tokens de movimento do Vantly Neural.
 *
 * Personalidade "Corporate/Premium" (skill motion-design): durações curtas,
 * sem overshoot, curva padrão do Material 3. Uma curva para ~80% dos casos,
 * três durações e um padrão de entrada (fade + deslocamento curto).
 */
object VantlyMotion {
    /** Curva padrão M3 — transições na tela. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Entradas (desacelera). */
    val Emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Saídas (acelera). */
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val Quick = 150      // toque / toggle
    const val Medium = 250     // ícones, chips, seleção
    const val Slow = 350       // cards, painéis, expansão

    /** Atraso entre itens em entradas escalonadas. */
    const val Stagger = 40
}

/**
 * Retorna true quando o usuário desativou ou reduziu animações no sistema
 * (Opções do desenvolvedor / Acessibilidade → "Remover animações").
 * Usado para suprimir animações contínuas (pulsos) e entradas decorativas.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
        scale == 0f
    }
}

