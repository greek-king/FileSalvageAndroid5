package com.filesalvage.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.filesalvage.services.FileRecoveryService
import com.filesalvage.ui.theme.*
import com.filesalvage.viewmodels.ScanViewModel

@Composable
fun RecoveryScreen(vm: ScanViewModel) {
    val state by vm.recoveryState.collectAsState()

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = state.result != null,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith
                fadeOut(animationSpec = tween(400))
            },
            label = "recoveryContent"
        ) { isDone ->
            if (isDone) {
                state.result?.let { CompletionView(result = it, vm = vm) }
            } else {
                RecoveryProgressView(
                    progress   = state.progress.percentage,
                    step       = state.progress.currentFileName,
                    completed  = state.progress.completed,
                    total      = state.progress.total,
                    failedNames = state.progress.failed,
                    onCancel   = { vm.cancelRecovery() }
                )
            }
        }
    }
}

// ─── In-progress view ─────────────────────────────────────────────────────────

@Composable
private fun RecoveryProgressView(
    progress: Float,
    step: String,
    completed: Int,
    total: Int,
    failedNames: List<String>,
    onCancel: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Spinning shield icon
        RecoverySpinner()

        Spacer(Modifier.height(28.dp))

        Text("Recovering Files...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            step.ifEmpty { "Preparing..." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Progress bar
        LinearProgressIndicator(
            progress      = { progress },
            modifier      = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color         = BrandGreen,
            trackColor    = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$completed / $total files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = BrandGreen, fontWeight = FontWeight.SemiBold)
        }

        if (failedNames.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = BrandRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${failedNames.size} file(s) failed so far",
                        style = MaterialTheme.typography.bodySmall, color = BrandRed)
                }
            }
        }

        Spacer(Modifier.height(48.dp))
        OutlinedButton(onClick = onCancel,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrandRed.copy(alpha = 0.5f))) {
            Text("Cancel", color = BrandRed)
        }
    }
}

@Composable
private fun RecoverySpinner() {
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "spinAngle"
    )
    Box(
        modifier = Modifier
            .size(100.dp)
            .rotate(spin)
            .background(
                Brush.sweepGradient(listOf(BrandGreen.copy(alpha = 0f), BrandGreen, BrandCyan)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(MaterialTheme.colorScheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Restore, null, tint = BrandGreen,
                modifier = Modifier.size(36.dp))
        }
    }
}

// ─── Completion view ──────────────────────────────────────────────────────────

@Composable
private fun CompletionView(result: FileRecoveryService.RecoveryResult, vm: ScanViewModel) {
    val successRate = result.successRate
    val isFullSuccess = result.failed.isEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // Success/partial icon
        CompletionIcon(isFullSuccess)

        Spacer(Modifier.height(20.dp))

        Text(
            if (isFullSuccess) "Recovery Complete!" else "Recovery Finished",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (isFullSuccess) BrandGreen else BrandAmber
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${result.succeeded.size} of ${result.succeeded.size + result.failed.size} files recovered",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Stats row
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("✅ Recovered", "${result.succeeded.size}", BrandGreen, Modifier.weight(1f))
            StatCard("❌ Failed",    "${result.failed.size}",    BrandRed,   Modifier.weight(1f))
            StatCard("⏱ Time",
                "${result.durationMs / 1000}s", BrandBlue, Modifier.weight(1f))
        }

        if (result.succeeded.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("SAVED TO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, null, tint = BrandAmber,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Pictures / Movies / Downloads",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("FileSalvage subfolder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (result.failed.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("FAILED FILES",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = BrandRed.copy(alpha = 0.07f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                result.failed.take(5).forEach { f ->
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = BrandRed,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(f.name, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (result.failed.size > 5) {
                    Text(
                        "+ ${result.failed.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = { vm.navigate(com.filesalvage.viewmodels.Screen.Home) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandCyan,
                contentColor   = Color(0xFF003333)
            )
        ) {
            Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Back to Home", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                vm.navigate(com.filesalvage.viewmodels.Screen.Results)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, BrandCyan.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp), tint = BrandCyan)
            Spacer(Modifier.width(8.dp))
            Text("Back to Results", color = BrandCyan)
        }
    }
}

@Composable
private fun CompletionIcon(isSuccess: Boolean) {
    val scale by rememberInfiniteTransition(label = "icon").animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconScale"
    )
    val color = if (isSuccess) BrandGreen else BrandAmber
    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(scale)
            .background(color.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSuccess) Icons.Default.CheckCircle else Icons.Default.TaskAlt,
                null, tint = color, modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}
