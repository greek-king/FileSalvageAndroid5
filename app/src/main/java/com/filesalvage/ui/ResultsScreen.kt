package com.filesalvage.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.filesalvage.models.*
import com.filesalvage.ui.theme.*
import com.filesalvage.viewmodels.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(vm: ScanViewModel) {
    val state        by vm.scanState.collectAsState()
    val selectedIds  by vm.selectedFiles.collectAsState()
    val activeFilter by vm.activeFilter.collectAsState()
    val result = state.result ?: return

    val displayFiles = vm.filteredFiles()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scan Results", fontWeight = FontWeight.Bold)
                        Text(
                            "${result.files.size} files found · ${result.durationMs / 1000}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.navigate(com.filesalvage.viewmodels.Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (selectedIds.size == displayFiles.size) vm.deselectAll()
                        else vm.selectAll()
                    }) {
                        Text(
                            if (selectedIds.size == displayFiles.size) "Deselect All" else "Select All",
                            color = BrandCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                RecoveryBottomBar(
                    count      = selectedIds.size,
                    totalSize  = vm.selectedSize,
                    onRecover  = { vm.startRecovery() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.padding(padding)) {

            // ── Summary cards ─────────────────────────────────────────────────
            SummaryRow(result)

            // ── Type filter tabs ──────────────────────────────────────────────
            TypeFilterBar(
                result       = result,
                activeFilter = activeFilter,
                onFilterChange = { vm.setFilter(it) }
            )

            Spacer(Modifier.height(4.dp))

            // ── File list ─────────────────────────────────────────────────────
            if (displayFiles.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayFiles, key = { it.id }) { file ->
                        FileCard(
                            file       = file,
                            isSelected = file.id in selectedIds,
                            onToggle   = { vm.toggleFileSelection(file.id) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Summary row ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryRow(result: ScanResult) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryChip(
            label = "${result.files.size}",
            sub   = "Recoverable",
            color = BrandCyan,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = formatBytes(result.totalSize),
            sub   = "Total Size",
            color = BrandPurple,
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            label = "${result.totalScanned}",
            sub   = "Scanned",
            color = BrandBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(label: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Type filter bar ──────────────────────────────────────────────────────────

@Composable
private fun TypeFilterBar(
    result: ScanResult,
    activeFilter: FileType?,
    onFilterChange: (FileType?) -> Unit
) {
    val tabs = listOf(null) + FileType.values().filter {
        result.byType.containsKey(it)
    }

    ScrollableTabRow(
        selectedTabIndex = tabs.indexOf(activeFilter).coerceAtLeast(0),
        containerColor   = MaterialTheme.colorScheme.background,
        contentColor     = BrandCyan,
        edgePadding      = 16.dp,
        indicator        = { tabPositions ->
            val idx = tabs.indexOf(activeFilter).coerceAtLeast(0)
            if (idx < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[idx]),
                    color = BrandCyan
                )
            }
        }
    ) {
        tabs.forEach { type ->
            val count = if (type == null) result.files.size
                        else result.byType[type]?.size ?: 0
            Tab(
                selected = activeFilter == type,
                onClick  = { onFilterChange(type) },
                text     = {
                    Text(
                        text = if (type == null) "All ($count)" else "${type.label} ($count)",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

// ─── File card ────────────────────────────────────────────────────────────────

@Composable
private fun FileCard(file: RecoverableFile, isSelected: Boolean, onToggle: () -> Unit) {
    val typeColor = Color(android.graphics.Color.parseColor(file.fileType.colorHex))
    val chanceColor = Color(android.graphics.Color.parseColor(file.recoveryChanceColor))
    val bgColor = if (isSelected) BrandCyan.copy(alpha = 0.08f)
                  else MaterialTheme.colorScheme.surface
    val borderColor = if (isSelected) BrandCyan else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape  = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / type icon
            FileThumbnail(file = file, typeColor = typeColor)
            Spacer(Modifier.width(12.dp))

            // File info
            Column(Modifier.weight(1f)) {
                Text(
                    file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (file.isTrashed) {
                        Text("· Recycle Bin",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandAmber)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    file.formattedDeletedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            // Recovery chance badge + checkbox
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(chanceColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        file.recoveryChanceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = chanceColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor   = BrandCyan,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun FileThumbnail(file: RecoverableFile, typeColor: Color) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(typeColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (file.contentUri != null && (file.fileType == FileType.PHOTO || file.fileType == FileType.VIDEO)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(file.contentUri)
                    .crossfade(true)
                    .size(112, 112)
                    .build(),
                contentDescription = file.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val icon = when (file.fileType) {
                FileType.PHOTO    -> Icons.Default.Image
                FileType.VIDEO    -> Icons.Default.Videocam
                FileType.AUDIO    -> Icons.Default.MusicNote
                FileType.DOCUMENT -> Icons.Default.Article
                FileType.UNKNOWN  -> Icons.Default.InsertDriveFile
            }
            Icon(icon, null, tint = typeColor, modifier = Modifier.size(26.dp))
        }
        // Video play overlay
        if (file.fileType == FileType.VIDEO) {
            Box(
                Modifier
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White,
                    modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── Recovery bottom bar ──────────────────────────────────────────────────────

@Composable
private fun RecoveryBottomBar(count: Int, totalSize: Long, onRecover: () -> Unit) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$count files selected", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                Text(formatBytes(totalSize), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onRecover,
                shape   = RoundedCornerShape(12.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = BrandCyan,
                    contentColor = Color(0xFF003333))
            ) {
                Icon(Icons.Default.Restore, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recover", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("No files found for this filter",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> "%.1f GB".format(gb)
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.0f KB".format(kb)
        else    -> "$bytes B"
    }
}

@Composable
private fun Modifier.tabIndicatorOffset(tabPosition: TabPosition): Modifier = this.then(
    Modifier.wrapContentSize(Alignment.BottomStart)
        .offset(x = tabPosition.left)
        .width(tabPosition.width)
)
