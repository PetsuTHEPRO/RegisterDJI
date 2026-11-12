package com.sloth.registerapp.presentation.shared.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import com.sloth.registerapp.presentation.app.theme.VantlyMotion
import com.sloth.registerapp.presentation.app.theme.rememberReducedMotion

/** Tons semânticos. NORMAL e MUTED são neutros; os demais representam estados. */
enum class TelemetryStatusTone {
    NORMAL,
    SUCCESS,
    WARNING,
    CRITICAL,
    MUTED
}

fun TelemetryStatusTone.accent(): Color = when (this) {
    TelemetryStatusTone.SUCCESS -> NeuralColors.Success
    TelemetryStatusTone.WARNING -> NeuralColors.Warning
    TelemetryStatusTone.CRITICAL -> NeuralColors.Danger
    TelemetryStatusTone.NORMAL -> NeuralColors.TextPrimary
    TelemetryStatusTone.MUTED -> NeuralColors.TextSecondary
}

/**
 * Chip de status compacto (ponto + texto), no estilo DJI.
 * A cor muda com animação suave quando o estado operacional muda.
 */
@Composable
fun TelemetryBadge(
    text: String,
    modifier: Modifier = Modifier,
    statusTone: TelemetryStatusTone = TelemetryStatusTone.NORMAL,
    icon: ImageVector? = null,
    showPulseDot: Boolean = false
) {
    val accent by animateColorAsState(
        targetValue = statusTone.accent(),
        animationSpec = tween(VantlyMotion.Medium, easing = VantlyMotion.Standard),
        label = "badge_accent"
    )
    val container = when (statusTone) {
        TelemetryStatusTone.NORMAL, TelemetryStatusTone.MUTED -> NeuralColors.SurfaceElevated
        else -> accent.copy(alpha = 0.14f)
    }

    Row(
        modifier = modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            showPulseDot -> {
                StatusDot(color = accent, pulse = statusTone == TelemetryStatusTone.SUCCESS || statusTone == TelemetryStatusTone.CRITICAL)
                Spacer(Modifier.width(6.dp))
            }
            icon != null -> {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
            }
        }
        Text(
            text = text,
            color = if (statusTone == TelemetryStatusTone.NORMAL) NeuralColors.TextPrimary else accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Ponto de estado. O pulso é sutil, só é usado para estados vivos
 * (conectado / crítico) e é desativado quando o sistema reduz animações.
 */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Int = 7,
    pulse: Boolean = false
) {
    val reduced = rememberReducedMotion()
    val alpha = if (pulse && !reduced) {
        val transition = rememberInfiniteTransition(label = "dot_pulse")
        val a by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        )
        a
    } else 1f
    Box(
        modifier = modifier
            .size(size.dp)
            .alpha(alpha)
            .background(color, CircleShape)
    )
}
