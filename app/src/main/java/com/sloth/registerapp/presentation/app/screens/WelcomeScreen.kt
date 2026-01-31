package com.sloth.registerapp.presentation.app.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    // Animações
    val infiniteTransition = rememberInfiniteTransition(label = "drone_float")
    val droneOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drone_animation"
    )

    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Cores do tema centralizado
    val colorScheme = MaterialTheme.colorScheme
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            colorScheme.background,
            colorScheme.surface,
            colorScheme.surfaceVariant
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        // Glowing orbs no background
        GlowingOrb(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-50).dp, y = 50.dp),
            color = colorScheme.primary,
            size = 200.dp
        )

        GlowingOrb(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 100.dp),
            color = colorScheme.tertiary,
            size = 250.dp
        )

        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Hero Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status Badge
                StatusBadge()

                Spacer(modifier = Modifier.height(32.dp))

                // Drone Icon com animação
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .offset(y = droneOffset.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulse effect
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .alpha(0.3f)
                            .background(
                                color = colorScheme.primary,
                                shape = CircleShape
                            )
                    )

                    // Drone emoji
                    Text(
                        text = "🚁",
                        fontSize = 80.sp,
                        modifier = Modifier.offset(y = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Title
                Text(
                    text = "Mission Control",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Gradient subtitle
                Text(
                    text = "Plataforma Inteligente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.primaryContainer.copy(alpha = 0.1f),
                                    colorScheme.primary.copy(alpha = 0.1f),
                                    colorScheme.primaryContainer.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Description
                Text(
                    text = "Sistema avançado de planejamento e monitoramento de missões autônomas para drones",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = "Autônomo", value = "100%")
                    StatDivider()
                    StatItem(label = "Monitoramento", value = "Real-time")
                    StatDivider()
                    StatItem(label = "Detecção", value = "IA")
                }
            }

            Spacer(modifier = Modifier.height(32.dp)) // Replace weight spacer

            // Buttons Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Button (Login)
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        colorScheme.primary,
                                        colorScheme.primaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚀",
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Entrar",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Secondary Button (Register)
                OutlinedButton(
                    onClick = onRegisterClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = colorScheme.surface.copy(alpha = 0.6f),
                        contentColor = colorScheme.onSurface
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                colorScheme.outline,
                                colorScheme.primary
                            )
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Criar Conta",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatusBadge() {
    val colorScheme = MaterialTheme.colorScheme
    val pulseAnimation = rememberInfiniteTransition(label = "badge_pulse")
    val badgePulse by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge_pulse"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colorScheme.primary.copy(alpha = 0.1f),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    colorScheme.primary.copy(alpha = 0.3f),
                    colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .scale(badgePulse)
                    .background(
                        color = colorScheme.primary,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sistema Autônomo de Missões",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatDivider() {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(
                color = colorScheme.outline.copy(alpha = 0.5f)
            )
    )
}

@Composable
fun GlowingOrb(
    modifier: Modifier = Modifier,
    color: Color,
    size: Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .alpha(0.4f)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}
