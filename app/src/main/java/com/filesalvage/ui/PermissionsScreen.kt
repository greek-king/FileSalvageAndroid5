package com.filesalvage.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.filesalvage.ui.theme.*
import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onAllGranted: @Composable () -> Unit) {
    val context = LocalContext.current

    // Build the permission list based on API level
    val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    val multiPerm = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        multiPerm.launchMultiplePermissionRequest()
    }

    if (multiPerm.allPermissionsGranted) {
        onAllGranted()
    } else {
        PermissionUI(
            deniedPermissions = multiPerm.permissions.filter { !it.status.isGranted },
            onRequest = { multiPerm.launchMultiplePermissionRequest() },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionUI(
    deniedPermissions: List<PermissionState>,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val shouldShowRationale = deniedPermissions.any { it.status.shouldShowRationale }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(BrandCyan.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = BrandCyan,
                    modifier = Modifier.size(44.dp))
            }

            Spacer(Modifier.height(24.dp))

            Text("Storage Access Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

            Spacer(Modifier.height(10.dp))

            Text(
                if (shouldShowRationale)
                    "FileSalvage needs access to your media files to scan for and recover deleted content. " +
                    "Please grant the required permissions."
                else
                    "Storage permissions are required to scan for recoverable files. " +
                    "Without this, the app cannot function.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            // What each permission does
            PermissionExplanationCard()

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = if (shouldShowRationale) onOpenSettings else onRequest,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandCyan,
                    contentColor = Color(0xFF003333))
            ) {
                Icon(if (shouldShowRationale) Icons.Default.Settings else Icons.Default.Check,
                    null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (shouldShowRationale) "Open Settings" else "Grant Permissions",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PermissionExplanationCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PermExplainRow(Icons.Default.Image, "Read Media Images",
                "Scan for deleted photos", BrandCyan)
            PermExplainRow(Icons.Default.Videocam, "Read Media Videos",
                "Scan for deleted videos", BrandRed)
            PermExplainRow(Icons.Default.MusicNote, "Read Media Audio",
                "Scan for deleted audio files", BrandPurple)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                PermExplainRow(Icons.Default.Storage, "Read External Storage",
                    "Access device storage to scan", BrandBlue)
            }
        }
    }
}

@Composable
private fun PermExplainRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, sub: String, color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(sub, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
