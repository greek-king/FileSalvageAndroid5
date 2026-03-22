package com.filesalvage.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.filesalvage.models.ScanDepth
import com.filesalvage.ui.theme.*
import com.filesalvage.viewmodels.ScanViewModel

@Composable
fun HomeScreen(vm: ScanViewModel) {
    val depth by vm.scanDepth.collectAsState()
    val freeSpace by remember { derivedStateOf { vm.formattedFreeSpace() } }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "gradient"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1A1A3E),
                        Color(0xFF0F0F1A)
                    ),
                    center = Offset(200f * gradientOffset + 100f, 300f),
                    radius = 900f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Logo + title ─────────────────────────────────────────────────
            Spacer(Modifier.height(16.dp))
            AppLogo()
            Spacer(Modifier.height(20.dp))

            Text(
                "FileSalvage",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(listOf(BrandCyan, BrandBlue))
                )
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Recover deleted photos, videos, audio & documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // ── Storage info card ─────────────────────────────────────────────
            StorageInfoCard(freeSpace = freeSpace)

            Spacer(Modifier.height(24.dp))

            // ── Scan depth selector ───────────────────────────────────────────
            Text(
                "SELECT SCAN TYPE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(10.dp))

            ScanDepth.values().forEach { d ->
                ScanDepthCard(
                    depth      = d,
                    isSelected = d == depth,
                    onSelect   = { vm.setScanDepth(d) }
                )
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(24.dp))

            // ── Start scan button ─────────────────────────────────────────────
            StartScanButton(onClick = { vm.startScan() })

            Spacer(Modifier.height(32.dp))

            // ── Feature chips ─────────────────────────────────────────────────
            FeatureRow()
        }
    }
}

@Composable
private fun AppLogo() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(pulse)
            .background(
                Brush.radialGradient(listOf(BrandCyan.copy(alpha = 0.3f), Color.Transparent)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .background(
                    Brush.linearGradient(listOf(BrandCyan, BrandBlue)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.FindInPage,
                contentDescription = "FileSalvage",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun StorageInfoCard(freeSpace: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Storage, null, tint = BrandCyan, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Internal Storage", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(freeSpace, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            StatusBadge("Ready", BrandGreen)
        }
    }
}

@Composable
private fun ScanDepthCard(depth: ScanDepth, isSelected: Boolean, onSelect: () -> Unit) {
    val borderColor = if (isSelected) BrandCyan else Color.Transparent
    val bgColor     = if (isSelected) BrandCyan.copy(alpha = 0.08f)
                      else MaterialTheme.colorScheme.surface

    val iconMap = mapOf(
        ScanDepth.QUICK to Icons.Default.FlashOn,
        ScanDepth.DEEP  to Icons.Default.ManageSearch,
        ScanDepth.FULL  to Icons.Default.Layers
    )
    val colorMap = mapOf(
        ScanDepth.QUICK to BrandGreen,
        ScanDepth.DEEP  to BrandAmber,
        ScanDepth.FULL  to BrandRed
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape  = RoundedCornerShape(14.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        (colorMap[depth] ?: BrandCyan).copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(iconMap[depth] ?: Icons.Default.Search, null,
                    tint = colorMap[depth] ?: BrandCyan, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(depth.label, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(depth.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = BrandCyan,
                    modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun StartScanButton(onClick: () -> Unit) {
    Button(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor = BrandCyan,
            contentColor   = Color(0xFF003333)
        )
    ) {
        Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Start Scan", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureRow() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        FeatureChip(Icons.Default.Photo,    "Photos",    BrandCyan)
        FeatureChip(Icons.Default.Videocam, "Videos",    BrandRed)
        FeatureChip(Icons.Default.MusicNote,"Audio",     BrandPurple)
        FeatureChip(Icons.Default.Article,  "Docs",      BrandBlue)
    }
}

@Composable
private fun FeatureChip(icon: ImageVector, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatusBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.SemiBold)
    }
}
