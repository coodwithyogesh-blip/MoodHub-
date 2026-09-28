package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ai.AssistantState
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SonicWaveSphere(
    state: AssistantState,
    amplitude: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SonicWaveTransition")

    // Rotation phase for swirling energy
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Breathing pulse
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Dynamic colors reflecting Arushi's emotional & assistant state
    val (coreColors, glowColor) = when (state) {
        AssistantState.LISTENING -> Pair(
            listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1)),
            Color(0xFF38BDF8)
        )
        AssistantState.SPEAKING -> Pair(
            listOf(Color(0xFFC084FC), Color(0xFF9333EA), Color(0xFF6B21A8)),
            Color(0xFFC084FC)
        )
        AssistantState.WORKING -> Pair(
            listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF047857)),
            Color(0xFF34D399)
        )
        AssistantState.CONNECTING -> Pair(
            listOf(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFB45309)),
            Color(0xFFFBBF24)
        )
        AssistantState.ERROR -> Pair(
            listOf(Color(0xFFF87171), Color(0xFFDC2626), Color(0xFF991B1B)),
            Color(0xFFF87171)
        )
        AssistantState.IDLE -> Pair(
            listOf(Color(0xFF818CF8), Color(0xFF4F46E5), Color(0xFF312E81)),
            Color(0xFF6366F1)
        )
    }

    val dynamicAmp = remember { Animatable(0f) }
    LaunchedEffect(amplitude, state) {
        val target = when (state) {
            AssistantState.LISTENING -> maxOf(amplitude, 0.25f)
            AssistantState.SPEAKING -> 0.7f
            AssistantState.WORKING -> 0.45f
            else -> 0.1f
        }
        dynamicAmp.animateTo(target, tween(150))
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .testTag("sonic_wave_sphere")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 3.4f) * breathScale

            // Outer Pulsing Glow Rings
            val glowAlpha = (0.25f + dynamicAmp.value * 0.4f).coerceIn(0.1f, 0.8f)
            drawCircle(
                color = glowColor.copy(alpha = glowAlpha * 0.35f),
                radius = baseRadius * (1.35f + dynamicAmp.value * 0.3f),
                center = center
            )
            drawCircle(
                color = glowColor.copy(alpha = glowAlpha * 0.55f),
                radius = baseRadius * (1.18f + dynamicAmp.value * 0.18f),
                center = center
            )

            // Inner Gradient Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // Interactive Sonic Waveform Ribbons
            val waveCount = 3
            for (w in 0 until waveCount) {
                val wavePath = Path()
                val waveOffset = (w * PI / 3f).toFloat()
                val ampFactor = baseRadius * (0.15f + dynamicAmp.value * 0.35f)

                val points = 36
                for (i in 0..points) {
                    val angle = (i.toFloat() / points) * 2 * PI.toFloat()
                    val waveR = baseRadius + sin(angle * 4 + phase + waveOffset) * ampFactor
                    val x = center.x + waveR * kotlin.math.cos(angle)
                    val y = center.y + waveR * kotlin.math.sin(angle)

                    if (i == 0) {
                        wavePath.moveTo(x.toFloat(), y.toFloat())
                    } else {
                        wavePath.lineTo(x.toFloat(), y.toFloat())
                    }
                }
                wavePath.close()

                drawPath(
                    path = wavePath,
                    color = Color.White.copy(alpha = 0.45f - w * 0.12f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}
