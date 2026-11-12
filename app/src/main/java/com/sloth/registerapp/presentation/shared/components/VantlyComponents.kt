package com.sloth.registerapp.presentation.shared.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sloth.registerapp.presentation.app.theme.NeuralColors
import com.sloth.registerapp.presentation.app.theme.VantlyMotion
import com.sloth.registerapp.presentation.app.theme.rememberReducedMotion
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Microinterações
// ---------------------------------------------------------------------------

/** Feedback de toque: escala leve (0.97) durante o pressionamento. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(VantlyMotion.Quick, easing = VantlyMotion.Standard),
        label = "press_scale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Entrada suave (fade + deslocamento curto) com escalonamento por índice.
 * Executa apenas uma vez por composição e é desativada com animações reduzidas.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reduced = rememberReducedMotion()
    var visible by rememberSaveable { mutableStateOf(reduced) }
    LaunchedEffect(Unit) {
        if (!visible) {
            delay((index.coerceAtMost(8) * VantlyMotion.Stagger).toLong())
            visible = true
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(VantlyMotion.Slow, easing = VantlyMotion.Emphasized)) +
            slideInVertically(tween(VantlyMotion.Slow, easing = VantlyMotion.Emphasized)) { it / 8 }
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Cabeçalhos
// ---------------------------------------------------------------------------

/** Barra superior compacta (56dp) com navegação, título, subtítulo e ações. */
@Composable
fun VantlyTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String = "Voltar",
    onNavigationClick: (() -> Unit)? = null,
    applyStatusBarPadding: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NeuralColors.Bg)
            .then(if (applyStatusBarPadding) Modifier.statusBarsPadding() else Modifier)
            .heightIn(min = 56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (navigationIcon != null && onNavigationClick != null) {
            IconButton(onClick = onNavigationClick) {
                Icon(navigationIcon, contentDescription = navigationContentDescription, tint = NeuralColors.TextPrimary)
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = title,
                color = NeuralColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = NeuralColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Título de seção pequeno com ação textual opcional. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = NeuralColors.TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                color = NeuralColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(vertical = 10.dp, horizontal = 4.dp)
                    .semantics { role = Role.Button }
                    .then(Modifier.clickableNoIndication(onActionClick))
            )
        }
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

// ---------------------------------------------------------------------------
// Métricas e ações
// ---------------------------------------------------------------------------

/** Métrica compacta: valor em destaque + rótulo pequeno. */
@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = NeuralColors.TextPrimary
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = NeuralColors.TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = value,
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = label,
            color = NeuralColors.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Tile quadrado da grade de ações rápidas (ícone linear + rótulo). */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 76.dp)
            .pressScale(interaction),
        shape = RoundedCornerShape(14.dp),
        color = NeuralColors.Surface,
        border = BorderStroke(1.dp, NeuralColors.Border),
        interactionSource = interaction
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            Icon(icon, contentDescription = null, tint = NeuralColors.TextPrimary, modifier = Modifier.size(22.dp))
            Text(
                text = label,
                color = NeuralColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Botão primário neutro: fundo branco, texto preto. */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp).pressScale(interaction),
        shape = RoundedCornerShape(12.dp),
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NeuralColors.Primary,
            contentColor = NeuralColors.OnPrimary,
            disabledContainerColor = NeuralColors.SurfaceElevated,
            disabledContentColor = NeuralColors.TextDisabled
        )
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Botão secundário: contorno cinza, texto branco. */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    contentColor: Color = NeuralColors.TextPrimary
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, NeuralColors.BorderStrong),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------------------------------------------------------------------------
// Busca e filtros
// ---------------------------------------------------------------------------

/** Campo de busca discreto (44dp). */
@Composable
fun VantlySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(NeuralColors.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, NeuralColors.Border, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = NeuralColors.TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (value.isEmpty()) {
                Text(placeholder, color = NeuralColors.TextDisabled, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = NeuralColors.TextPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(NeuralColors.TextPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = NeuralColors.TextSecondary, modifier = Modifier.size(18.dp))
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
    }
}

/** Chip de filtro com transição de cor suave. Selecionado = branco/preto. */
@Composable
fun VantlyFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        if (selected) NeuralColors.Primary else NeuralColors.Surface,
        tween(VantlyMotion.Medium, easing = VantlyMotion.Standard), label = "chip_bg"
    )
    val fg by animateColorAsState(
        if (selected) NeuralColors.OnPrimary else NeuralColors.TextSecondary,
        tween(VantlyMotion.Medium, easing = VantlyMotion.Standard), label = "chip_fg"
    )
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics { this.selected = selected },
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = if (selected) null else BorderStroke(1.dp, NeuralColors.Border)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

/** Linha horizontal rolável de filtros — nunca corta opções. */
@Composable
fun <T> VantlyFilterRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            VantlyFilterChip(label = label(option), selected = option == selected, onClick = { onSelect(option) })
        }
    }
}

// ---------------------------------------------------------------------------
// Preview de rota (dados reais de waypoints)
// ---------------------------------------------------------------------------

/**
 * Desenha a rota a partir de coordenadas reais (lat, lng). Sem dados,
 * apenas a grade é desenhada — nada é inventado.
 */
@Composable
fun RoutePreview(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    poi: Pair<Double, Double>? = null,
    lineColor: Color = NeuralColors.TextPrimary
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(NeuralColors.BgSoft)
        val step = 18.dp.toPx()
        var gx = 0f
        while (gx <= size.width) {
            drawLine(NeuralColors.GridLine, Offset(gx, 0f), Offset(gx, size.height), 1f); gx += step
        }
        var gy = 0f
        while (gy <= size.height) {
            drawLine(NeuralColors.GridLine, Offset(0f, gy), Offset(size.width, gy), 1f); gy += step
        }
        if (points.isEmpty()) return@Canvas

        val all = if (poi != null && (poi.first != 0.0 || poi.second != 0.0)) points + poi else points
        val minLat = all.minOf { it.first }; val maxLat = all.maxOf { it.first }
        val minLng = all.minOf { it.second }; val maxLng = all.maxOf { it.second }
        val spanX = maxOf(0.00001, maxLng - minLng)
        val spanY = maxOf(0.00001, maxLat - minLat)
        val pad = 10.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        // Mantém a proporção geográfica
        val scale = minOf(w / spanX.toFloat(), h / spanY.toFloat())
        val offX = pad + (w - spanX.toFloat() * scale) / 2
        val offY = pad + (h - spanY.toFloat() * scale) / 2
        fun map(p: Pair<Double, Double>) = Offset(
            offX + ((p.second - minLng).toFloat() * scale),
            offY + ((maxLat - p.first).toFloat() * scale)
        )
        val screen = points.map(::map)
        if (screen.size > 1) {
            val path = Path().apply {
                screen.forEachIndexed { i, o -> if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
            }
            drawPath(path, lineColor.copy(alpha = 0.85f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        screen.forEachIndexed { i, o ->
            drawCircle(NeuralColors.Bg, radius = 3.5.dp.toPx(), center = o)
            drawCircle(if (i == 0) NeuralColors.Success else lineColor, radius = 2.5.dp.toPx(), center = o)
        }
        if (all.size > points.size) {
            drawCircle(NeuralColors.Warning, radius = 3.dp.toPx(), center = map(all.last()))
        }
    }
}

/** Divisor fino neutro. */
@Composable
fun VantlyDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(NeuralColors.Border))
}

