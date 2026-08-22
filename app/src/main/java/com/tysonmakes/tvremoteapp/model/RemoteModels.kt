package com.tysonmakes.tvremoteapp.model

data class TvDevice(
    val ip: String,
    val port: Int = 5555,
    val name: String = "Android TV",
    val isOnline: Boolean = true,
    val lastConnected: Long = System.currentTimeMillis()
) {
    val fullAddress: String get() = "$ip:$port"
}

enum class RemoteTab(val title: String) {
    CONTROLS("Remote"),
    TRACKPAD("Touchpad"),
    APPS("Apps"),
    NUMPAD("Numpad"),
    TOOLS("TV Tools"),
    TERMINAL("Console")
}

enum class HapticIntensity(val label: String) {
    OFF("Off"),
    SUBTLE("Light"),
    MEDIUM("Medium"),
    STRONG("Strong")
}

enum class ResponseMode(val label: String, val description: String) {
    TURBO_STREAM("⚡ Turbo Stream (<15ms)", "Interactive persistent shell stream for instant response"),
    STANDARD("Standard ADB (50-100ms)", "Standard individual command socket execution")
}

enum class ThemeAccent(val label: String, val primaryHex: Long) {
    CYAN("Electric Teal", 0xFF00E5FF),
    SAGE("Emerald Sage", 0xFF385C41),
    AMBER("Fire Amber", 0xFFFF9100),
    PURPLE("Neon Violet", 0xFFA855F7),
    CRIMSON("Ruby Red", 0xFFEF4444)
}

data class RemoteSettings(
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val responseMode: ResponseMode = ResponseMode.TURBO_STREAM,
    val repeatSpeedMs: Long = 90L,
    val autoConnectLastDevice: Boolean = true,
    val themeAccent: ThemeAccent = ThemeAccent.CYAN
)

data class AppShortcut(
    val id: String,
    val name: String,
    val packageName: String,
    val category: String,
    val badgeColorHex: Long
)

data class TvToolAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val command: String,
    val colorHex: Long
)

object RemoteKeycodes {
    const val DPAD_UP = "KEYCODE_DPAD_UP"
    const val DPAD_DOWN = "KEYCODE_DPAD_DOWN"
    const val DPAD_LEFT = "KEYCODE_DPAD_LEFT"
    const val DPAD_RIGHT = "KEYCODE_DPAD_RIGHT"
    const val DPAD_CENTER = "KEYCODE_DPAD_CENTER"
    const val BACK = "KEYCODE_BACK"
    const val HOME = "KEYCODE_HOME"
    const val MENU = "KEYCODE_MENU"
    const val POWER = "KEYCODE_POWER"
    const val SLEEP = "KEYCODE_SLEEP"
    const val WAKEUP = "KEYCODE_WAKEUP"
    const val VOLUME_UP = "KEYCODE_VOLUME_UP"
    const val VOLUME_DOWN = "KEYCODE_VOLUME_DOWN"
    const val VOLUME_MUTE = "KEYCODE_VOLUME_MUTE"
    const val MEDIA_PLAY_PAUSE = "KEYCODE_MEDIA_PLAY_PAUSE"
    const val MEDIA_PLAY = "KEYCODE_MEDIA_PLAY"
    const val MEDIA_PAUSE = "KEYCODE_MEDIA_PAUSE"
    const val MEDIA_STOP = "KEYCODE_MEDIA_STOP"
    const val MEDIA_NEXT = "KEYCODE_MEDIA_NEXT"
    const val MEDIA_PREVIOUS = "KEYCODE_MEDIA_PREVIOUS"
    const val MEDIA_FAST_FORWARD = "KEYCODE_MEDIA_FAST_FORWARD"
    const val MEDIA_REWIND = "KEYCODE_MEDIA_REWIND"
    const val CHANNEL_UP = "KEYCODE_CHANNEL_UP"
    const val CHANNEL_DOWN = "KEYCODE_CHANNEL_DOWN"
    const val TV_INPUT = "KEYCODE_TV_INPUT"
    const val SETTINGS = "KEYCODE_SETTINGS"
    const val SEARCH = "KEYCODE_SEARCH"
    const val VOICE_ASSIST = "KEYCODE_VOICE_ASSIST"
    const val NOTIFICATION = "KEYCODE_NOTIFICATION"
    const val APP_SWITCH = "KEYCODE_APP_SWITCH"

    const val NUM_0 = "KEYCODE_0"
    const val NUM_1 = "KEYCODE_1"
    const val NUM_2 = "KEYCODE_2"
    const val NUM_3 = "KEYCODE_3"
    const val NUM_4 = "KEYCODE_4"
    const val NUM_5 = "KEYCODE_5"
    const val NUM_6 = "KEYCODE_6"
    const val NUM_7 = "KEYCODE_7"
    const val NUM_8 = "KEYCODE_8"
    const val NUM_9 = "KEYCODE_9"

    const val PROG_RED = "KEYCODE_PROG_RED"
    const val PROG_GREEN = "KEYCODE_PROG_GREEN"
    const val PROG_YELLOW = "KEYCODE_PROG_YELLOW"
    const val PROG_BLUE = "KEYCODE_PROG_BLUE"
}

val DEFAULT_APP_SHORTCUTS = listOf(
    AppShortcut("youtube", "YouTube", "com.google.android.youtube.tv", "Streaming", 0xFFFF0000),
    AppShortcut("netflix", "Netflix", "com.netflix.ninja", "Streaming", 0xFFE50914),
    AppShortcut("prime", "Prime Video", "com.amazon.amazonvideo.livingroom", "Streaming", 0xFF00A8E1),
    AppShortcut("disney", "Disney+ / Hotstar", "in.startv.hotstar", "Streaming", 0xFF113CCF),
    AppShortcut("spotify", "Spotify", "com.spotify.tv.android", "Music", 0xFF1DB954),
    AppShortcut("apple", "Apple TV", "com.apple.atve.androidtv.appletv", "Streaming", 0xFF2A2A2A),
    AppShortcut("twitch", "Twitch", "tv.twitch.android.app", "Streaming", 0xFF9146FF),
    AppShortcut("plex", "Plex", "com.plexapp.android", "Media", 0xFFE5A00D),
    AppShortcut("kodi", "Kodi", "org.xbmc.kodi", "Media", 0xFF17B2E7),
    AppShortcut("jiocinema", "JioCinema", "com.jio.media.ondemand.tv", "Streaming", 0xFFD81B60),
    AppShortcut("sonyliv", "Sony LIV", "com.sonyliv", "Streaming", 0xFF00C853),
    AppShortcut("vlc", "VLC for TV", "org.videolan.vlc", "Player", 0xFFFF9800),
    AppShortcut("settings", "TV Settings", "com.android.tv.settings", "System", 0xFF607D8B),
    AppShortcut("playstore", "Play Store", "com.android.vending", "System", 0xFF00C853)
)

val TV_TOOL_ACTIONS = listOf(
    TvToolAction(
        id = "clear_cache",
        title = "Boost & Clear Cache",
        description = "Trims background memory and cleans cache",
        icon = "cleaning_services",
        command = "sync && echo 3 > /proc/sys/vm/drop_caches || am kill-all",
        colorHex = 0xFF00E5FF
    ),
    TvToolAction(
        id = "sleep_tv",
        title = "Sleep Display",
        description = "Puts TV display into instant standby",
        icon = "bedtime",
        command = "input keyevent KEYCODE_SLEEP",
        colorHex = 0xFF818CF8
    ),
    TvToolAction(
        id = "wake_tv",
        title = "Wake Up TV",
        description = "Wakes up screen and powers on HDMI-CEC",
        icon = "wb_sunny",
        command = "input keyevent KEYCODE_WAKEUP",
        colorHex = 0xFFFBBF24
    ),
    TvToolAction(
        id = "open_settings",
        title = "Device Settings",
        description = "Opens Android TV settings menu directly",
        icon = "settings",
        command = "am start -a android.settings.SETTINGS",
        colorHex = 0xFF34D399
    ),
    TvToolAction(
        id = "developer_options",
        title = "Developer Options",
        description = "Opens TV developer debug preferences",
        icon = "code",
        command = "am start -a com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS",
        colorHex = 0xFFA78BFA
    ),
    TvToolAction(
        id = "soft_reboot",
        title = "Fast Soft Reboot",
        description = "Restarts TV system UI without full boot cycle",
        icon = "restart_alt",
        command = "setprop ctl.restart zygote || am restart",
        colorHex = 0xFFF97316
    ),
    TvToolAction(
        id = "full_reboot",
        title = "Full TV Reboot",
        description = "Complete system restart of Android TV device",
        icon = "power_settings_new",
        command = "reboot",
        colorHex = 0xFFEF4444
    ),
    TvToolAction(
        id = "take_screenshot",
        title = "Capture Screenshot",
        description = "Takes screenshot and saves to /sdcard/tv_shot.png",
        icon = "camera_alt",
        command = "screencap -p /sdcard/tv_shot.png",
        colorHex = 0xFF06B6D4
    )
)
