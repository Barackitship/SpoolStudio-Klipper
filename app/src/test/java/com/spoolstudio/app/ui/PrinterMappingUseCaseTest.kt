package com.spoolstudio.app.ui

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrinterMappingUseCaseTest {
    @Test
    fun loadReturnsSpoolLinkMessage() = runBlocking {
        val snapshot = PrinterMappingSnapshot(1, 2, null, null, null, ResolvedPrinterIntegrationMode.STANDARD_KLIPPER)
        val useCase = PrinterMappingUseCase(
            loadMapping = { _, _, _ -> PrinterMappingLoadResult(snapshot, activeSpoolAvailable = true) },
            saveMapping = { _, _, _, _, _, _, _, _ -> throw AssertionError("save must not be called") }
        )

        val result = useCase.load("http://printer.local", PrinterIntegrationMode.STANDARD_KLIPPER)

        assertTrue(result is PrinterMappingOperationResult.Loaded)
        assertEquals(
            "Toolhead status loaded",
            (result as PrinterMappingOperationResult.Loaded).message
        )
        assertEquals(snapshot, result.snapshot)
    }

    @Test
    fun saveReturnsFriendlyFailureMessage() = runBlocking {
        val useCase = PrinterMappingUseCase(
            loadMapping = { _, _, _ -> throw AssertionError("load must not be called") },
            saveMapping = { _, _, _, _, _, _, _, _ -> throw IllegalStateException("timeout after 10 seconds") }
        )

        val result = useCase.save(
            baseUrl = "http://printer.local",
            printerIntegrationMode = PrinterIntegrationMode.PAXX12_SPOOL_LINK,
            toolhead1SpoolId = 1,
            toolhead2SpoolId = null,
            toolhead3SpoolId = null,
            toolhead4SpoolId = null,
            activeSpoolId = 1
        )

        assertTrue(result is PrinterMappingOperationResult.Failed)
        assertEquals(
            "Save failed: Printer script did not finish in time. Check the printer macro or active spool script.",
            (result as PrinterMappingOperationResult.Failed).message
        )
    }
}
