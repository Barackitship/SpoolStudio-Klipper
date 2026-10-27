package com.spoolstudio.app.ui

enum class PrinterIntegrationMode(
    val storedValue: String,
    val label: String
) {
    STANDARD_KLIPPER("standard_klipper", "Standard Klipper (Moonraker)"),
    PAXX12_SPOOL_LINK("paxx12_spoollink", "Legacy Paxx12 / Snapmaker U1");

    companion object {
        fun fromStoredValue(value: String?): PrinterIntegrationMode =
            entries.firstOrNull { it.storedValue == value } ?: STANDARD_KLIPPER
    }
}

enum class ResolvedPrinterIntegrationMode(
    val label: String
) {
    STANDARD_KLIPPER("Standard Klipper"),
    PAXX12_SPOOL_LINK("Legacy Paxx12 SpoolLink")
}

