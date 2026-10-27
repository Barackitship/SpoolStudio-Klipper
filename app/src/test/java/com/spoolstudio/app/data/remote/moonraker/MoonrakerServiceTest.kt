package com.spoolstudio.app.data.remote.moonraker

import com.spoolstudio.app.ui.friendlyPrinterMappingError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonrakerServiceTest {
    @Test
    fun versionGateKeepsPaxx12Firmware141InLegacyMode() {
        assertFalse(moonrakerVersionAtLeast("1.4.1.6_20260608141446", 1, 5, 0))
    }

    @Test
    fun versionGateAllowsPaxx12Firmware15AndNewer() {
        assertTrue(moonrakerVersionAtLeast("1.5.0", 1, 5, 0))
        assertTrue(moonrakerVersionAtLeast("1.5.1_20260730", 1, 5, 0))
    }

    @Test
    fun componentsDetectEnabledSpoolmanIntegration() {
        assertTrue(
            moonrakerComponentsHaveSpoolmanIntegration(
                listOf("database", "spoolman", "spoollink")
            ) == true
        )
    }

    @Test
    fun componentsDetectDisabledSpoolmanIntegration() {
        assertFalse(
            moonrakerComponentsHaveSpoolmanIntegration(
                listOf("database", "mqtt", "extensions")
            ) == true
        )
    }

    @Test
    fun missingAssignmentCommandReturnsPaxx12PrerequisiteMessage() {
        assertEquals(
            "No Paxx12 toolhead assignment command is available on the printer. Enable the printer Spoolman integration, restart Klipper/Moonraker, then try again.",
            friendlyPrinterMappingError(
                IllegalStateException(
                    "No Paxx12 toolhead assignment command is available on the printer. Enable the printer Spoolman integration and restart Klipper/Moonraker."
                )
            )
        )
    }

    @Test
    fun componentsDetectMoonrakerSpoolmanIntegration() {
        val state = moonrakerComponentState(listOf("database", "spoolman"))
        assertTrue(state?.hasSpoolman == true)
        assertFalse(state?.hasSpoolLink == true)
    }
}
