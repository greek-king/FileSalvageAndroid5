package com.filesalvage.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.filesalvage.ui.theme.*
import com.filesalvage.viewmodels.ScanViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanningScreen(vm: ScanViewModel) {
    val state by vm.scanState.collectAsState()
    val progress = state.progress

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Spacer(Modifier.height(32.dp))

            // ── Animated radar ring ───────────────────────────────────────────
            ScanRadar(progress.percentage)

            Spacer(Modifier.height(36.dp))

            // ── Current step ─────────────────────────────────────────────────
            AnimatedContent(
                targetState = progress.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step"
            ) { step ->
                Text(
                    step.ifEmpty { "Initializing..." },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Progress bar ──────────────────────────────────────────────────
            LinearProgressIndicator(
                progress        = { progress.percentage },
                modifier        = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color           = BrandCyan,
                trackColor      = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "${(progress.percentage * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(28.dp))

            // ── Files found counter ───────────────────────────────────────────
            FilesFoundCounter(count = progress.filesFound)

            Spacer(Modifier.height(48.dp))

            // ── Cancel button ─────────────────────────────────────────────────
            OutlinedButton(
                onClick = { vm.cancelScan() },
                shape   = RoundedCornerShape(12.dp),
                border  = androidx.compose.foundation.BorderStroke(1.dp, BrandRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, null, tint = BrandRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cancel", color = BrandRed)
            }
        }
    }
}

@Composable
private fun ScanRadar(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    // Spinning sweep
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "sweep"
    )

    // Pulsing outer ring
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    // Orbiting dots
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label = "orbit"
    )

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(pulseScale)
                .background(BrandCyan.copy(alpha = 0.05f), CircleShape)
        )

        // Progress arc (canvas)
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            val strokeWidth = 3.dp.toPx()
            val radius = (size.minDimension / 2) - strokeWidth / 2

            // Track
            drawArc(
                color      = BrandCyan.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter  = false,
                style      = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap   = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Progress fill
            drawArc(
                brush      = Brush.sweepGradient(
                    listOf(BrandCyan.copy(alpha = 0f), BrandCyan, BrandBlue)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter  = false,
                style      = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth * 2,
                    cap   = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Orbiting dot
            val orbitRadius = radius - strokeWidth
            val dotX = center.x + orbitRadius * cos(Math.toRadians(orbitAngle.toDouble())).toFloat()
            val dotY = center.y + orbitRadius * sin(Math.toRadians(orbitAngle.toDouble())).toFloat()
            drawCircle(color = BrandCyan, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
        }

        // Center icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.radialGradient(listOf(BrandCyan.copy(alpha = 0.2f), Color.Transparent)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FindInPage,
                contentDescription = null,
                tint     = BrandCyan,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun FilesFoundCounter(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.InsertDriveFile, null, tint = BrandCyan,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = count,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith
                        (slideOutVertically { -it } + fadeOut())
                    },
                    label = "count"
                ) { c ->
                    Text(
                        "$c",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandCyan
                    )
                }
                Text(
                    "files found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
