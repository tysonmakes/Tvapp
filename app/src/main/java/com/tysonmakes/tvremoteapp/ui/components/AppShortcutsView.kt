package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.AppShortcut
import com.tysonmakes.tvremoteapp.model.DEFAULT_APP_SHORTCUTS
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun AppShortcutsView(
    onLaunchApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var customPackage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = "Instant App Launchers",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(260.dp)
        ) {
            items(DEFAULT_APP_SHORTCUTS) { app ->
                AppCardItem(
                    app = app,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLaunchApp(app.packageName)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Custom package launcher field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceRaised)
                .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customPackage,
                onValueChange = { customPackage = it },
                placeholder = { Text("Launch package (e.g. com.spotify.tv)", color = TextMuted, fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_package_input")
            )

            IconButton(
                onClick = {
                    if (customPackage.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLaunchApp(customPackage.trim())
                    }
                },
                modifier = Modifier.testTag("launch_custom_package_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Launch Package",
                    tint = AccentCyan
                )
            }
        }
    }
}

@Composable
private fun AppCardItem(
    app: AppShortcut,
    onClick: () -> Unit
) {
    val (iconVector, badgeColor) = when (app.id) {
        "youtube" -> Pair(Icons.Default.PlayCircleFilled, Color(0xFFFF0000))
        "netflix" -> Pair(Icons.Default.Movie, Color(0xFFE50914))
        "prime" -> Pair(Icons.Default.Tv, Color(0xFF00A8E1))
        "disney" -> Pair(Icons.Default.Star, Color(0xFF113CCF))
        "spotify" -> Pair(Icons.Default.MusicNote, Color(0xFF1DB954))
        "twitch" -> Pair(Icons.Default.LiveTv, Color(0xFF9146FF))
        "plex" -> Pair(Icons.Default.VideoLibrary, Color(0xFFE5A00D))
        "kodi" -> Pair(Icons.Default.Subscriptions, Color(0xFF17B2E7))
        "settings" -> Pair(Icons.Default.Settings, Color(0xFF78909C))
        else -> Pair(Icons.Default.Shop, Color(0xFF00E676))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(DarkSurfaceRaised, DarkSurfaceVariant)
                )
            )
            .border(1.dp, DpadBorderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = app.name,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = app.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = app.category,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
