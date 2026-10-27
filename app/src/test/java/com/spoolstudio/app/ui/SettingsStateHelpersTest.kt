package com.spoolstudio.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsStateHelpersTest {
    @Test
    fun oldPrinterModeSettingsResolveToStandardKlipper() {
        assertEquals(PrinterIntegrationMode.STANDARD_KLIPPER, PrinterIntegrationMode.fromStoredValue("auto_detect"))
        assertEquals(PrinterIntegrationMode.STANDARD_KLIPPER, PrinterIntegrationMode.fromStoredValue("legacy_paxx12"))
        assertEquals(PrinterIntegrationMode.STANDARD_KLIPPER, PrinterIntegrationMode.fromStoredValue(null))
        assertEquals(PrinterIntegrationMode.PAXX12_SPOOL_LINK, PrinterIntegrationMode.fromStoredValue("paxx12_spoollink"))
    }

    @Test
    fun loadStateCopiesStoredSettingsForViewModel() {
        val state = buildSettingsLoadState(
            AppSettings(
                connectionProfiles = listOf(
                    ConnectionProfile(
                        id = "default",
                        name = "Default",
                        spoolmanUrl = "http://spoolman.local",
                        moonrakerUrl = "http://printer.local"
                    )
                ),
                activeConnectionProfileId = "default",
                spoolmanUrl = "http://spoolman.local",
                moonrakerUrl = "http://printer.local",
                printerIntegrationMode = PrinterIntegrationMode.PAXX12_SPOOL_LINK,
                spoolmanSortBy = "last_used:desc",
                bambuMasterKey = "ABCD1234",
                showLotNumber = true,
                showCommentField = true,
                showEmptySpoolWeight = true,
                materialModifierFieldDeclined = true
            )
        )

        assertEquals(true, state.showLotNumber)
        assertEquals(true, state.showCommentField)
        assertEquals(true, state.showEmptySpoolWeight)
        assertEquals("http://spoolman.local", state.spoolmanUrl)
        assertEquals("last_used:desc", state.spoolmanSortBy)
        assertEquals("http://printer.local", state.moonrakerUrl)
        assertEquals(PrinterIntegrationMode.PAXX12_SPOOL_LINK, state.printerIntegrationMode)
        assertEquals("ABCD1234", state.bambuMasterKey)
        assertEquals(true, state.materialModifierFieldDeclined)
    }

    @Test
    fun saveStateNormalizesUrlsSortAndBambuKey() {
        val state = buildSettingsSaveState(
            SettingsSaveInput(
                spoolmanUrl = " http://spoolman.local/ ",
                moonrakerUrl = " http://printer.local/ ",
                connectionProfiles = emptyList(),
                activeConnectionProfileId = "",
                printerIntegrationMode = PrinterIntegrationMode.PAXX12_SPOOL_LINK,
                spoolmanSortBy = "name:asc",
                bambuMasterKey = " abcd1234 ",
                showCommentField = true
            )
        )

        assertEquals("http://spoolman.local", state.spoolmanUrl)
        assertEquals("http://printer.local", state.moonrakerUrl)
        assertEquals(PrinterIntegrationMode.PAXX12_SPOOL_LINK, state.printerIntegrationMode)
        assertEquals("name:asc", state.spoolmanSortBy)
        assertEquals("ABCD1234", state.bambuMasterKey)
        assertEquals(true, state.showCommentField)
    }

    @Test
    fun saveStateKeepsBlankSortAsEmptyString() {
        val state = buildSettingsSaveState(
            SettingsSaveInput(
                spoolmanUrl = "",
                moonrakerUrl = "",
                connectionProfiles = emptyList(),
                activeConnectionProfileId = "",
                printerIntegrationMode = PrinterIntegrationMode.PAXX12_SPOOL_LINK,
                spoolmanSortBy = " ",
                bambuMasterKey = "",
                showCommentField = false
            )
        )

        assertEquals("", state.spoolmanSortBy)
        assertFalse(state.showCommentField)
    }
}
