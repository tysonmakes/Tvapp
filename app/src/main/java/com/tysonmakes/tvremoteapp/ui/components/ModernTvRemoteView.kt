package com.tysonmakes.tvremoteapp.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tysonmakes.tvremoteapp.model.RemoteKeycodes
import com.tysonmakes.tvremoteapp.ui.theme.*
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernTvRemoteView(
    onKeySend: (String) -> Unit,
    onOpenNumpad: () -> Unit,
    onOpenTextInput: () -> Unit,
    onOpenPowerMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isMouseMode by remember { mutableStateOf(false) }

    fun clickKey(code: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onKeySend(code)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(AtvSheetDark)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Drag Pill Handle
        Box(
            modifier = Modifier
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF5A606D))
        )

        // -------------------------------------------------------------
        // TOP MEDIA & UTILITY BUTTONS (2 Rows x 4 Columns = 8 Buttons)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Prev, Play/Pause, Next, Power (Red)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RemoteMediaIconButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous / Rewind",
                    onClick = { clickKey(RemoteKeycodes.MEDIA_PREVIOUS) },
                    modifier = Modifier.weight(1f)
                )
                RemoteMediaIconButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play / Pause",
                    onClick = { clickKey(RemoteKeycodes.MEDIA_PLAY_PAUSE) },
                    modifier = Modifier.weight(1f)
                )
                RemoteMediaIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next / Fast Forward",
                    onClick = { clickKey(RemoteKeycodes.MEDIA_NEXT) },
                    modifier = Modifier.weight(1f)
                )
                RemoteMediaIconButton(
                    icon = Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = AtvPowerRed,
                    onClick = { clickKey(RemoteKeycodes.POWER) },
                    onLongClick = onOpenPowerMenu,
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 2: Mute, Vol -, Vol +, Mic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RemoteMediaIconButton(
                    icon = Icons.AutoMirrored.Filled.VolumeMute,
                    contentDescription = "Mute",
                    onClick = { clickKey(RemoteKeycodes.VOLUME_MUTE) },
                    modifier = Modifier.weight(1f)
                )
                RemoteMediaIconButton(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = "Volume Down",
                    onClick = { clickKey(RemoteKeycodes.VOLUME_DOWN) },
                    modifier = Modifier.weight(1f)
                )
                RemoteMediaIconButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Volume Up",
                    onClick = { clickKey(RemoteKeycodes.VOLUME_UP) },
                    modifier = Modifier.weight(1f)
                )
                RemoteMediaIconButton(
                    icon = Icons.Default.Mic,
                    contentDescription = "Voice Assist",
                    onClick = { clickKey(RemoteKeycodes.VOICE_ASSIST) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // -------------------------------------------------------------
        // CENTER CONTROLLER: D-PAD OR MOUSE TRACKPAD
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isMouseMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "RemoteCenterController"
            ) { mouseActive ->
                if (!mouseActive) {
                    // Authentic atvTools D-Pad: Large circle with cross quadrants and pure solid white center
                    AtvCircularDpad(
                        onUp = { clickKey(RemoteKeycodes.DPAD_UP) },
                        onDown = { clickKey(RemoteKeycodes.DPAD_DOWN) },
                        onLeft = { clickKey(RemoteKeycodes.DPAD_LEFT) },
                        onRight = { clickKey(RemoteKeycodes.DPAD_RIGHT) },
                        onCenter = { clickKey(RemoteKeycodes.DPAD_CENTER) },
                        modifier = Modifier.size(260.dp)
                    )
                } else {
                    // Authentic atvTools Mouse Trackpad
                    AtvMouseTrackpad(
                        onKeySend = { clickKey(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // MODE TOGGLE ROW (Mouse icon on Left, D-pad icon on Right)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isMouseMode = true
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isMouseMode) AtvButtonDark else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.Mouse,
                    contentDescription = "Mouse Trackpad Mode",
                    tint = if (isMouseMode) AtvAccentBlue else AtvTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isMouseMode = false
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isMouseMode) AtvButtonDark else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenWith,
                    contentDescription = "D-Pad Compass Mode",
                    tint = if (!isMouseMode) AtvAccentBlue else AtvTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // -------------------------------------------------------------
        // BOTTOM 5 ACTION BUTTONS (Numpad, Text, Menu, Home, Back)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RemoteMediaIconButton(
                icon = Icons.Default.Apps,
                contentDescription = "Numpad & Channels",
                onClick = onOpenNumpad,
                modifier = Modifier.weight(1f)
            )
            RemoteMediaIconButton(
                icon = Icons.Default.ChatBubble,
                contentDescription = "Text Keyboard Input",
                onClick = onOpenTextInput,
                modifier = Modifier.weight(1f)
            )
            RemoteMediaIconButton(
                icon = Icons.Default.Menu,
                contentDescription = "Menu",
                onClick = { clickKey(RemoteKeycodes.MENU) },
                modifier = Modifier.weight(1f)
            )
            RemoteMediaIconButton(
                icon = Icons.Default.Home,
                contentDescription = "Home",
                onClick = { clickKey(RemoteKeycodes.HOME) },
                modifier = Modifier.weight(1f)
            )
            RemoteMediaIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = { clickKey(RemoteKeycodes.BACK) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteMediaIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AtvTextPrimary,
    onLongClick: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            )
            .testTag("remote_btn_$contentDescription"),
        shape = RoundedCornerShape(14.dp),
        color = AtvButtonDark,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AtvCircularDpad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(AtvButtonDark)
            .testTag("atv_circular_dpad"),
        contentAlignment = Alignment.Center
    ) {
        // Quadrant divider lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.5.dp.toPx()
            val center = size.width / 2f
            // Horizontal divider line
            drawLine(
                color = AtvDividerLine,
                start = Offset(0f, center),
                end = Offset(size.width, center),
                strokeWidth = strokeWidth
            )
            // Vertical divider line
            drawLine(
                color = AtvDividerLine,
                start = Offset(center, 0f),
                end = Offset(center, size.height),
                strokeWidth = strokeWidth
            )
        }

        // Directional touch sectors: UP
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onUp()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Up",
                tint = AtvTextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Directional touch sectors: DOWN
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDown()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Down",
                tint = AtvTextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Directional touch sectors: LEFT
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLeft()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                tint = AtvTextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // Directional touch sectors: RIGHT
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.38f)
                .fillMaxHeight()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onRight()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                tint = AtvTextPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        // CENTER BUTTON: Solid Pure White Circle (as shown in Screenshot 1!)
        Surface(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCenter()
                }
                .testTag("dpad_center_white_ok"),
            shape = CircleShape,
            color = AtvCenterOkWhite,
            shadowElevation = 6.dp
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                // Pure clean white circle surface matching atvTools
            }
        }
    }
}

@Composable
fun AtvMouseTrackpad(
    onKeySend: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    val sensitivity = 36f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(AtvButtonDark)
            .border(1.dp, AtvDividerLine, RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchPosition = offset
                        tryAwaitRelease()
                        touchPosition = null
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onKeySend(RemoteKeycodes.DPAD_CENTER)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchPosition = offset
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = { touchPosition = null },
                    onDragCancel = { touchPosition = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        touchPosition = change.position
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (abs(totalDragX) > sensitivity || abs(totalDragY) > sensitivity) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (abs(totalDragX) > abs(totalDragY)) {
                                if (totalDragX > 0) {
                                    onKeySend(RemoteKeycodes.DPAD_RIGHT)
                                } else {
                                    onKeySend(RemoteKeycodes.DPAD_LEFT)
                                }
                            } else {
                                if (totalDragY > 0) {
                                    onKeySend(RemoteKeycodes.DPAD_DOWN)
                                } else {
                                    onKeySend(RemoteKeycodes.DPAD_UP)
                                }
                            }
                            totalDragX = 0f
                            totalDragY = 0f
                        }
                    }
                )
            }
            .testTag("atv_mouse_trackpad"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            touchPosition?.let { pos ->
                drawCircle(
                    color = AtvAccentBlue.copy(alpha = 0.25f),
                    radius = 42.dp.toPx(),
                    center = pos
                )
                drawCircle(
                    color = AtvAccentBlue,
                    radius = 8.dp.toPx(),
                    center = pos
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PanToolAlt,
                contentDescription = null,
                tint = AtvTextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Touchpad Mode",
                color = AtvTextSecondary,
                fontSize = 13.sp
            )
        }
    }
}
