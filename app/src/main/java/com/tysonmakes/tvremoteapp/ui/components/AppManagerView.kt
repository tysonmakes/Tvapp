package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.AppCategoryFilter
import com.tysonmakes.tvremoteapp.model.InstalledApp
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun AppManagerView(
    apps: List<InstalledApp>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onForceStop: (String) -> Unit,
    onClearData: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onExtractApk: (InstalledApp) -> Unit,
    onOpenPlayStore: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AppCategoryFilter.USER) }
    var selectedAppForMenu by remember { mutableStateOf<InstalledApp?>(null) }
    val clipboardManager = LocalClipboardManager.current

    val userAppsCount = remember(apps) { apps.count { !it.isSystemApp } }
    val systemAppsCount = remember(apps) { apps.count { it.isSystemApp } }

    val filteredApps = remember(apps, searchQuery, selectedFilter) {
        apps.filter { app ->
            val matchesCategory = when (selectedFilter) {
                AppCategoryFilter.USER -> !app.isSystemApp
                AppCategoryFilter.SYSTEM -> app.isSystemApp
                AppCategoryFilter.ALL -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // 1. Search Bar & Refresh Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("Search installed TV apps...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = DpadBorderColor,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentCyan, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh apps", tint = AccentCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. Filter Category Pills (User / System / All)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppFilterPill(
                label = "User (${userAppsCount})",
                isSelected = selectedFilter == AppCategoryFilter.USER,
                onClick = { selectedFilter = AppCategoryFilter.USER }
            )
            AppFilterPill(
                label = "System (${systemAppsCount})",
                isSelected = selectedFilter == AppCategoryFilter.SYSTEM,
                onClick = { selectedFilter = AppCategoryFilter.SYSTEM }
            )
            AppFilterPill(
                label = "All (${apps.size})",
                isSelected = selectedFilter == AppCategoryFilter.ALL,
                onClick = { selectedFilter = AppCategoryFilter.ALL }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. App List Area
        if (filteredApps.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No apps matching '$searchQuery'" else "No apps found in this category",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceRaised),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reload Apps List", color = AccentCyan, fontSize = 13.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    InstalledAppItem(
                        app = app,
                        onLaunchClick = { onLaunchApp(app.packageName) },
                        onMenuClick = { selectedAppForMenu = app }
                    )
                }
            }
        }
    }

    // App Detail Action Bottom Sheet / Alert Dialog
    selectedAppForMenu?.let { app ->
        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
            containerColor = DarkSurfaceRaised,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (app.isSystemApp) Color(0xFF6B7280).copy(alpha = 0.2f) else AccentCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (app.isSystemApp) Icons.Default.SettingsApplications else Icons.Default.Tv,
                            contentDescription = null,
                            tint = if (app.isSystemApp) TextMuted else AccentCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(app.appName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(app.packageName, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Open App on TV
                    AppActionItem(
                        icon = Icons.Default.PlayArrow,
                        title = "Launch App on TV",
                        subtitle = "Opens application immediately on TV",
                        color = AccentCyan,
                        onClick = {
                            onLaunchApp(app.packageName)
                            selectedAppForMenu = null
                        }
                    )

                    // Force Stop
                    AppActionItem(
                        icon = Icons.Default.Close,
                        title = "Force Stop",
                        subtitle = "Kills running background instance",
                        color = Color(0xFFF59E0B),
                        onClick = {
                            onForceStop(app.packageName)
                            selectedAppForMenu = null
                        }
                    )

                    // Clear Data
                    AppActionItem(
                        icon = Icons.Default.CleaningServices,
                        title = "Clear Data & Cache",
                        subtitle = "Resets application storage & cache",
                        color = Color(0xFFF97316),
                        onClick = {
                            onClearData(app.packageName)
                            selectedAppForMenu = null
                        }
                    )

                    // Extract APK
                    AppActionItem(
                        icon = Icons.Default.FileDownload,
                        title = "Extract APK to TV",
                        subtitle = "Copies APK to /sdcard/Download",
                        color = Color(0xFF38BDF8),
                        onClick = {
                            onExtractApk(app)
                            selectedAppForMenu = null
                        }
                    )

                    // Open in Play Store
                    AppActionItem(
                        icon = Icons.Default.Shop,
                        title = "Open in Play Store on TV",
                        subtitle = "Shows TV Play Store listing",
                        color = Color(0xFF10B981),
                        onClick = {
                            onOpenPlayStore(app.packageName)
                            selectedAppForMenu = null
                        }
                    )

                    // Copy Package Name
                    AppActionItem(
                        icon = Icons.Default.ContentCopy,
                        title = "Copy Package Name",
                        subtitle = app.packageName,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(app.packageName))
                            selectedAppForMenu = null
                        }
                    )

                    // Uninstall (or Disable)
                    AppActionItem(
                        icon = Icons.Default.Delete,
                        title = if (app.isSystemApp) "Uninstall System App" else "Uninstall App",
                        subtitle = if (app.isSystemApp) "Removes system app for current user" else "Completely uninstalls app from TV",
                        color = Color(0xFFEF4444),
                        onClick = {
                            onUninstall(app.packageName)
                            selectedAppForMenu = null
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
                    Text("Close", color = TextMuted)
                }
            }
        )
    }
}

@Composable
private fun AppFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) AccentCyan else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) AccentCyan else DpadBorderColor)
    ) {
        Text(
            text = label,
            color = if (isSelected) DarkBackground else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun InstalledAppItem(
    app: InstalledApp,
    onLaunchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val isSystem = app.isSystemApp
    val accentColor = if (isSystem) Color(0xFF94A3B8) else AccentCyan

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onMenuClick)
            .testTag("app_item_${app.packageName}"),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DpadBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSystem) Icons.Default.SettingsApplications else Icons.Default.Tv,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.appName,
                            color = TextPrimary,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        if (isSystem) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF374151)
                            ) {
                                Text(
                                    text = "System",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = app.packageName,
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "v${app.versionName} • ${app.sizeString}",
                        color = TextMuted.copy(alpha = 0.8f),
                        fontSize = 10.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Launch Button
                IconButton(
                    onClick = onLaunchClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Launch",
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Options Menu Button
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun AppActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = color, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextMuted, fontSize = 10.5.sp)
            }
        }
    }
}
