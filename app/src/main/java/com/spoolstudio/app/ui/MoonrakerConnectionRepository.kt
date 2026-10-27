package com.spoolstudio.app.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class MoonrakerConnectionResult(
    val reachable: Boolean,
    val status: String? = null,
    val error: String? = null,
    val firmwareVersion: String? = null,
    val moonrakerVersion: String? = null,
    val supportsSpoolLink: Boolean? = null,
    val hasSpoolmanComponent: Boolean? = null,
    val hasSpoolLinkComponent: Boolean? = null,
    val spoolmanIntegrationEnabled: Boolean? = null,
    val detectedModeLabel: String? = null
)

class MoonrakerConnectionRepository {
    suspend fun test(baseUrl: String): MoonrakerConnectionResult {
        val normalizedBase = baseUrl.trimEnd('/')

        // Primary health check: Standard Moonraker /server/spoolman/status
        val spoolmanStatusUrl = "$normalizedBase/server/spoolman/status"
        val spoolmanConnection = withContext(Dispatchers.IO) {
            runCatching {
                (URL(spoolmanStatusUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    requestMethod = "GET"
                }
            }.getOrNull()
        }

        if (spoolmanConnection != null) {
            try {
                val responseCode = withContext(Dispatchers.IO) { spoolmanConnection.responseCode }
                val responseText = withContext(Dispatchers.IO) {
                    if (responseCode in 200..299) {
                        spoolmanConnection.inputStream.bufferedReader().use { it.readText() }
                    } else {
                        spoolmanConnection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    }
                }

                if (responseCode in 200..299 && responseText.trim().startsWith("{")) {
                    val printerInfo = runCatching {
                        com.spoolstudio.app.data.remote.moonraker.MoonrakerService(normalizedBase).getPrinterInfo()
                    }.getOrNull()

                    return MoonrakerConnectionResult(
                        reachable = true,
                        status = "Moonraker reachable",
                        firmwareVersion = printerInfo?.displayVersion,
                        moonrakerVersion = printerInfo?.moonrakerVersion,
                        supportsSpoolLink = true,
                        hasSpoolmanComponent = true,
                        hasSpoolLinkComponent = true,
                        spoolmanIntegrationEnabled = true,
                        detectedModeLabel = "Firmware Spoolman integration"
                    )
                }
            } catch (_: Exception) {
                // Fall back to /printer/info if /server/spoolman/status fails
            } finally {
                spoolmanConnection.disconnect()
            }
        }

        // Fallback health check: /printer/info
        val testUrl = "$normalizedBase/printer/info"
        val connection = withContext(Dispatchers.IO) {
            (URL(testUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
        }

        try {
            val responseCode = withContext(Dispatchers.IO) { connection.responseCode }

            val responseText = withContext(Dispatchers.IO) {
                if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
            }

            return if (responseCode in 200..299 && responseText.trim().startsWith("{")) {
                val printerInfo = runCatching {
                    com.spoolstudio.app.data.remote.moonraker.MoonrakerService(normalizedBase).getPrinterInfo()
                }.getOrNull()
                val supportsSpoolLink = printerInfo?.supportsSpoolLink

                MoonrakerConnectionResult(
                    reachable = true,
                    status = "Moonraker reachable",
                    firmwareVersion = printerInfo?.displayVersion,
                    moonrakerVersion = printerInfo?.moonrakerVersion,
                    supportsSpoolLink = supportsSpoolLink,
                    hasSpoolmanComponent = printerInfo?.hasSpoolmanComponent,
                    hasSpoolLinkComponent = printerInfo?.hasSpoolLinkComponent,
                    spoolmanIntegrationEnabled = printerInfo?.spoolmanIntegrationEnabled,
                    detectedModeLabel = when (supportsSpoolLink) {
                        true -> "Firmware Spoolman integration"
                        false -> "Paxx12 1.5.0+ required"
                        null -> null
                    }
                )
            } else {
                MoonrakerConnectionResult(
                    reachable = false,
                    error = if (responseText.contains("<html", ignoreCase = true)) {
                        "Not a direct Moonraker endpoint"
                    } else {
                        "HTTP $responseCode"
                    }
                )
            }
        } finally {
            connection.disconnect()
        }
    }
}
