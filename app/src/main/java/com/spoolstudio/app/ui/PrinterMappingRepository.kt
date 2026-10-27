package com.spoolstudio.app.ui

import com.spoolstudio.app.data.remote.moonraker.MoonrakerService
import com.spoolstudio.app.data.remote.moonraker.MoonrakerToolheadStatus

data class PrinterMappingSnapshot(
    val toolhead1SpoolId: Int?,
    val toolhead2SpoolId: Int?,
    val toolhead3SpoolId: Int?,
    val toolhead4SpoolId: Int?,
    val activeSpoolId: Int?,
    val integrationMode: ResolvedPrinterIntegrationMode,
    val toolheadStatuses: List<PrinterToolheadLiveStatus> = emptyList()
)

data class PrinterToolheadLiveStatus(
    val toolheadIndex: Int,
    val spoolId: Int?,
    val isLoaded: Boolean,
    val material: String?,
    val variant: String?,
    val brand: String?,
    val filamentName: String?,
    val colorHex: String?
) {
    val displayTitle: String
        get() = filamentName
            ?: listOfNotNull(brand, material, variant)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Filament loaded" }

    val displaySubtitle: String
        get() = listOfNotNull(brand, material, variant)
            .filter { it.isNotBlank() }
            .joinToString(" / ")
}

data class PrinterMappingLoadResult(
    val snapshot: PrinterMappingSnapshot,
    val activeSpoolAvailable: Boolean
)

class PrinterMappingRepository(
    private val serviceFactory: (String) -> MoonrakerService = ::MoonrakerService
) {
    suspend fun load(
        baseUrl: String,
        printerIntegrationMode: PrinterIntegrationMode,
        toolheadCount: Int = 1
    ): PrinterMappingLoadResult {
        val service = serviceFactory(baseUrl)
        ensureSpoolLinkStatusReadable(service)
        val toolheadStatuses = service.getSpoolLinkToolheadStatuses(toolheadCount)
        val activeSpoolId = runCatching { service.getActiveSpoolId() }.getOrNull()
        val resolvedMode = when (printerIntegrationMode) {
            PrinterIntegrationMode.STANDARD_KLIPPER -> ResolvedPrinterIntegrationMode.STANDARD_KLIPPER
            PrinterIntegrationMode.PAXX12_SPOOL_LINK -> ResolvedPrinterIntegrationMode.PAXX12_SPOOL_LINK
        }

        return PrinterMappingLoadResult(
            snapshot = toolheadStatuses.toPrinterMappingSnapshot(
                activeSpoolId = activeSpoolId,
                integrationMode = resolvedMode
            ),
            activeSpoolAvailable = activeSpoolId != null
        )
    }

    suspend fun save(
        baseUrl: String,
        toolhead1SpoolId: Int?,
        toolhead2SpoolId: Int?,
        toolhead3SpoolId: Int?,
        toolhead4SpoolId: Int?,
        activeSpoolId: Int?,
        printerIntegrationMode: PrinterIntegrationMode,
        toolheadCount: Int = 1
    ): PrinterMappingSnapshot {
        val service = serviceFactory(baseUrl)
        ensureSpoolLinkStatusReadable(service)

        val targetSpool = activeSpoolId ?: toolhead1SpoolId
        service.setSpoolLinkToolSpool(0, targetSpool)
        if (toolhead2SpoolId != null) runCatching { service.setSpoolLinkToolSpool(1, toolhead2SpoolId) }
        if (toolhead3SpoolId != null) runCatching { service.setSpoolLinkToolSpool(2, toolhead3SpoolId) }
        if (toolhead4SpoolId != null) runCatching { service.setSpoolLinkToolSpool(3, toolhead4SpoolId) }

        val currentActiveSpoolId = runCatching { service.getActiveSpoolId() }.getOrNull()
        val resolvedMode = when (printerIntegrationMode) {
            PrinterIntegrationMode.STANDARD_KLIPPER -> ResolvedPrinterIntegrationMode.STANDARD_KLIPPER
            PrinterIntegrationMode.PAXX12_SPOOL_LINK -> ResolvedPrinterIntegrationMode.PAXX12_SPOOL_LINK
        }
        return service.getSpoolLinkToolheadStatuses(toolheadCount)
            .toPrinterMappingSnapshot(
                activeSpoolId = currentActiveSpoolId,
                integrationMode = resolvedMode
            )
    }

    suspend fun assignToolhead(
        baseUrl: String,
        toolheadIndex: Int,
        spoolId: Int?,
        printerIntegrationMode: PrinterIntegrationMode,
        toolheadCount: Int = 1
    ): PrinterMappingSnapshot {
        val service = serviceFactory(baseUrl)
        ensureSpoolLinkStatusReadable(service)

        val resolvedMode = when (printerIntegrationMode) {
            PrinterIntegrationMode.STANDARD_KLIPPER -> ResolvedPrinterIntegrationMode.STANDARD_KLIPPER
            PrinterIntegrationMode.PAXX12_SPOOL_LINK -> ResolvedPrinterIntegrationMode.PAXX12_SPOOL_LINK
        }

        if (printerIntegrationMode == PrinterIntegrationMode.STANDARD_KLIPPER) {
            // Use the synchronous REST endpoint so the status is immediately consistent
            // after the call returns, avoiding the "assignment not applied" false error
            // that occurs when the async GCode command has not yet propagated to the status API.
            service.setActiveSpoolIdViaRest(spoolId)
        } else {
            // PAXX12 SpoolLink uses the GCode command + polling to verify all toolheads.
            service.setSpoolLinkToolSpool(toolheadIndex, spoolId)
        }

        val currentActiveSpoolId = runCatching { service.getActiveSpoolId() }.getOrNull()
        return service.getSpoolLinkToolheadStatuses(toolheadCount)
            .toPrinterMappingSnapshot(
                activeSpoolId = currentActiveSpoolId,
                integrationMode = resolvedMode
            )
    }

    private suspend fun ensureSpoolLinkStatusReadable(service: MoonrakerService) {
        val hasSpoolmanStatus = runCatching {
            service.getActiveSpoolId()
            true
        }.getOrDefault(false)

        if (hasSpoolmanStatus) {
            return
        }

        val printerInfo = service.getPrinterInfo()
        if (!printerInfo.supportsSpoolLink) {
            val version = printerInfo.displayVersion ?: "unknown"
            throw IllegalStateException(
                "Paxx12 SpoolLink requires firmware 1.5.0 or newer. Detected firmware: $version. Use Spool Studio v2 for the old Paxx12 workflow."
            )
        }
        if (printerInfo.hasSpoolmanComponent == false || printerInfo.hasSpoolLinkComponent == false) {
            throw IllegalStateException(
                "Printer Spoolman integration is not ready. Enable Spoolman Integration in the printer config, then restart Klipper/Moonraker."
            )
        }
    }
}

private fun Map<String, Int?>.toPrinterMappingSnapshot(
    activeSpoolId: Int?,
    integrationMode: ResolvedPrinterIntegrationMode
): PrinterMappingSnapshot =
    PrinterMappingSnapshot(
        toolhead1SpoolId = this["T0"]?.takeIf { it > 0 },
        toolhead2SpoolId = this["T1"]?.takeIf { it > 0 },
        toolhead3SpoolId = this["T2"]?.takeIf { it > 0 },
        toolhead4SpoolId = this["T3"]?.takeIf { it > 0 },
        activeSpoolId = activeSpoolId,
        integrationMode = integrationMode
    )

private fun List<MoonrakerToolheadStatus>.toPrinterMappingSnapshot(
    activeSpoolId: Int?,
    integrationMode: ResolvedPrinterIntegrationMode
): PrinterMappingSnapshot {
    val byIndex = associateBy { it.toolheadIndex }
    val liveStatuses = map { status ->
        PrinterToolheadLiveStatus(
            toolheadIndex = status.toolheadIndex,
            spoolId = status.spoolId,
            isLoaded = status.isLoaded,
            material = status.material,
            variant = status.variant,
            brand = status.brand,
            filamentName = status.filamentName,
            colorHex = status.colorHex
        )
    }

    return PrinterMappingSnapshot(
        toolhead1SpoolId = byIndex[0]?.spoolId?.takeIf { it > 0 },
        toolhead2SpoolId = byIndex[1]?.spoolId?.takeIf { it > 0 },
        toolhead3SpoolId = byIndex[2]?.spoolId?.takeIf { it > 0 },
        toolhead4SpoolId = byIndex[3]?.spoolId?.takeIf { it > 0 },
        activeSpoolId = activeSpoolId,
        integrationMode = integrationMode,
        toolheadStatuses = liveStatuses
    )
}
