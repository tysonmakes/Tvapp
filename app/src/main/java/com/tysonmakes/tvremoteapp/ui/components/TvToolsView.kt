package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.ATV_GRID_TOOLS
import com.tysonmakes.tvremoteapp.model.TvToolAction
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun TvToolsView(
    onToolClick: (TvToolAction) -> Unit,
    onInstallApkClick: () -> Unit,
    onUploadFileClick: () -> Unit,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Advanced TV Tools & Controls",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(ATV_GRID_TOOLS, key = { it.id }) { tool ->
                TvGridToolCard(
                    tool = tool,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (tool.id) {
                            "install_apk" -> onInstallApkClick()
                            "upload_file" -> onUploadFileClick()
                            else -> onToolClick(tool)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TvGridToolCard(
    tool: TvToolAction,
    onClick: () -> Unit
) {
    val toolColor = Color(tool.colorHex)
    val iconVector: ImageVector = when (tool.icon) {
        "install" -> Icons.Default.DownloadForOffline
        "upload" -> Icons.Default.UploadFile
        "folder" -> Icons.Default.Folder
        "live_tv" -> Icons.Default.LiveTv
        "cast" -> Icons.Default.Cast
        "gamepad" -> Icons.Default.SportsEsports
        "screenshot" -> Icons.Default.CameraAlt
        "videocam" -> Icons.Default.Videocam
        "delete" -> Icons.Default.DeleteOutline
        "auto_awesome" -> Icons.Default.AutoAwesome
        "power_settings_new" -> Icons.Default.PowerSettingsNew
        else -> Icons.Default.Build
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp), spotColor = toolColor.copy(alpha = 0.2f))
            .clickable(onClick = onClick)
            .testTag("grid_tool_${tool.id}"),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DpadBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(toolColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = tool.title,
                    tint = toolColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tool.title,
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    color = TextMuted,
                    fontSize = 10.5.sp,
                    maxLines = 2,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
