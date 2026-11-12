package com.sloth.registerapp.presentation.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sloth.registerapp.presentation.app.theme.NeuralColors

/**
 * Card base do Vantly Neural: superfície #141414, borda discreta, sem brilho,
 * cantos moderados. Quando clicável, aplica feedback de escala leve.
 */
@Composable
fun AviationCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    containerColor: Color = NeuralColors.Surface,
    borderColor: Color = NeuralColors.Border,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = NeuralColors.TextPrimary
    )
    val border = BorderStroke(1.dp, borderColor)
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .pressScale(interaction),
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            interactionSource = interaction
        ) {
            Box(modifier = Modifier.fillMaxWidth()) { content() }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border
        ) {
            Box(modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}
