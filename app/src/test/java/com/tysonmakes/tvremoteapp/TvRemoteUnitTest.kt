package com.tysonmakes.tvremoteapp

import com.tysonmakes.tvremoteapp.adb.DeviceScanner
import com.tysonmakes.tvremoteapp.model.*
import org.junit.Assert.*
import org.junit.Test

class TvRemoteUnitTest {

    @Test
    fun testKeycodeMapperNavigationAndActionKeys() {
        assertEquals(19, KeycodeMapper.toNumeric(RemoteKeycodes.DPAD_UP))
        assertEquals(20, KeycodeMapper.toNumeric(RemoteKeycodes.DPAD_DOWN))
        assertEquals(21, KeycodeMapper.toNumeric(RemoteKeycodes.DPAD_LEFT))
        assertEquals(22, KeycodeMapper.toNumeric(RemoteKeycodes.DPAD_RIGHT))
        assertEquals(23, KeycodeMapper.toNumeric(RemoteKeycodes.DPAD_CENTER))
        assertEquals(4, KeycodeMapper.toNumeric(RemoteKeycodes.BACK))
        assertEquals(3, KeycodeMapper.toNumeric(RemoteKeycodes.HOME))
        assertEquals(82, KeycodeMapper.toNumeric(RemoteKeycodes.MENU))
        assertEquals(26, KeycodeMapper.toNumeric(RemoteKeycodes.POWER))
        assertEquals(24, KeycodeMapper.toNumeric(RemoteKeycodes.VOLUME_UP))
        assertEquals(25, KeycodeMapper.toNumeric(RemoteKeycodes.VOLUME_DOWN))
        assertEquals(164, KeycodeMapper.toNumeric(RemoteKeycodes.VOLUME_MUTE))
    }

    @Test
    fun testKeycodeMapperMediaAndNumpadKeys() {
        assertEquals(85, KeycodeMapper.toNumeric(RemoteKeycodes.MEDIA_PLAY_PAUSE))
        assertEquals(87, KeycodeMapper.toNumeric(RemoteKeycodes.MEDIA_NEXT))
        assertEquals(88, KeycodeMapper.toNumeric(RemoteKeycodes.MEDIA_PREVIOUS))
        assertEquals(7, KeycodeMapper.toNumeric(RemoteKeycodes.NUM_0))
        assertEquals(8, KeycodeMapper.toNumeric(RemoteKeycodes.NUM_1))
        assertEquals(16, KeycodeMapper.toNumeric(RemoteKeycodes.NUM_9))
        assertEquals(178, KeycodeMapper.toNumeric(RemoteKeycodes.TV_INPUT))
    }

    @Test
    fun testKeycodeMapperNumericFallback() {
        // Direct integer string passed
        assertEquals(66, KeycodeMapper.toNumeric("66"))
        // Unknown keycode falls back safely to DPAD_CENTER (23)
        assertEquals(23, KeycodeMapper.toNumeric("UNKNOWN_SPECIAL_KEY"))
    }

    @Test
    fun testAtvGridToolsHasAll11RequiredTools() {
        val tools = ATV_GRID_TOOLS
        assertEquals(11, tools.size)
        val toolIds = tools.map { it.id }.toSet()
        assertTrue(toolIds.contains("install_apk"))
        assertTrue(toolIds.contains("upload_file"))
        assertTrue(toolIds.contains("file_manager"))
        assertTrue(toolIds.contains("channels"))
        assertTrue(toolIds.contains("screen_mirror"))
        assertTrue(toolIds.contains("gamepad"))
        assertTrue(toolIds.contains("screenshot"))
        assertTrue(toolIds.contains("screen_record"))
        assertTrue(toolIds.contains("clear_cache"))
        assertTrue(toolIds.contains("screensaver"))
        assertTrue(toolIds.contains("power_menu"))
    }

    @Test
    fun testSubnetPrefixFormatting() {
        val prefix = DeviceScanner.getLocalSubnetPrefix()
        assertNotNull(prefix)
        assertTrue("Subnet prefix should end with a dot", prefix.endsWith("."))
        val parts = prefix.removeSuffix(".").split(".")
        assertTrue("Subnet prefix should have 3 segments", parts.size == 3)
    }

    @Test
    fun testTextEscapingForAdbShell() {
        val rawText = "Hello World! It's a test & price is $100"
        val escaped = buildString {
            for (ch in rawText) {
                when (ch) {
                    ' ' -> append("%s")
                    '\\', '"', '\'', '$', '`', '&', '|', ';', '<', '>', '(', ')', '*', '?', '~', '#', '!', '{', '}' -> {
                        append('\\').append(ch)
                    }
                    else -> append(ch)
                }
            }
        }

        assertTrue(escaped.contains("Hello%sWorld"))
        assertTrue(escaped.contains("\\&"))
        assertTrue(escaped.contains("\\$100"))
        assertTrue(escaped.contains("\\'"))
    }

    @Test
    fun testTvDeviceModel() {
        val dev = TvDevice("192.168.1.150", 5555, "Living Room TV")
        assertEquals("192.168.1.150:5555", dev.fullAddress)
        assertEquals("Living Room TV", dev.name)
        assertTrue(dev.isOnline)
    }
}
