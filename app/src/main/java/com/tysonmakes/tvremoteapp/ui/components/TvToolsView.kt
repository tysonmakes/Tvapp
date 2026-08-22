package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.TV_TOOL_ACTIONS
import com.tysonmakes.tvremoteapp.model.TvToolAction
import com.tysonmakes.tvremoteapp.ui.theme.*

@Composable
fun TvToolsView(
    onExecuteTool: (TvToolAction) -> Unit,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ Android TV Quick Utilities",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            if (isExecuting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AccentCyan,
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(TV_TOOL_ACTIONS) { tool ->
                TvToolCard(
                    tool = tool,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onExecuteTool(tool)
                    }
                )
            }
        }
    }
}

@Composable
private fun TvToolCard(
    tool: TvToolAction,
    onClick: () -> Unit
) {
    val iconVector: ImageVector = when (tool.icon) {
        "cleaning_services" -> Icons.Default.CleaningServices
        "bedtime" -> Icons.Default.Bedtime
        "wb_sunny" -> Icons.Default.WbSunny
        "settings" -> Icons.Default.Settings
        "code" -> Icons.Default.Code
        "restart_alt" -> Icons.Default.RestartAlt
        "power_settings_new" -> Icons.Default.PowerSettingsNew
        "camera_alt" -> Icons.Default.CameraAlt
        else -> Icons.Default.FlashOn
    }
    val toolColor = Color(tool.colorHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = toolColor.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(14.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkSurfaceRaised, DarkSurface)
                )
            )
            .border(1.dp, DpadBorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("tool_${tool.id}"),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(toolColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = tool.title,
                        tint = toolColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = tool.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text(
                    text = tool.description,
                    color = TextMuted,
                    fontSize = 9.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}
