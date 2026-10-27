package com.spoolstudio.app.ui

data class SettingsSaveInput(
    val spoolmanUrl: String,
    val moonrakerUrl: String,
    val connectionProfiles: List<ConnectionProfile>,
    val activeConnectionProfileId: String,
    val printerIntegrationMode: PrinterIntegrationMode,
    val toolheadCount: Int = 1,
    val spoolmanSortBy: String,
    val bambuMasterKey: String,
    val showCommentField: Boolean
)

data class SettingsSaveState(
    val connectionProfiles: List<ConnectionProfile>,
    val activeConnectionProfileId: String,
    val spoolmanUrl: String,
    val moonrakerUrl: String,
    val printerIntegrationMode: PrinterIntegrationMode,
    val toolheadCount: Int = 1,
    val spoolmanSortBy: String,
    val bambuMasterKey: String,
    val showCommentField: Boolean
)

data class SettingsLoadState(
    val showLotNumber: Boolean,
    val showCommentField: Boolean,
    val showEmptySpoolWeight: Boolean,
    val connectionProfiles: List<ConnectionProfile>,
    val activeConnectionProfileId: String,
    val spoolmanUrl: String,
    val spoolmanSortBy: String,
    val moonrakerUrl: String,
    val printerIntegrationMode: PrinterIntegrationMode,
    val toolheadCount: Int = 1,
    val bambuMasterKey: String,
    val materialModifierFieldDeclined: Boolean
)

fun buildSettingsLoadState(settings: AppSettings): SettingsLoadState =
    SettingsLoadState(
        showLotNumber = settings.showLotNumber,
        showCommentField = settings.showCommentField,
        showEmptySpoolWeight = settings.showEmptySpoolWeight,
        connectionProfiles = settings.connectionProfiles,
        activeConnectionProfileId = settings.activeConnectionProfileId,
        spoolmanUrl = settings.spoolmanUrl,
        spoolmanSortBy = settings.spoolmanSortBy,
        moonrakerUrl = settings.moonrakerUrl,
        printerIntegrationMode = settings.printerIntegrationMode,
        toolheadCount = settings.toolheadCount,
        bambuMasterKey = settings.bambuMasterKey,
        materialModifierFieldDeclined = settings.materialModifierFieldDeclined
    )

fun buildSettingsSaveState(input: SettingsSaveInput): SettingsSaveState {
    val normalizedProfiles = input.connectionProfiles
        .filter { it.id.isNotBlank() }
        .mapIndexed { index, profile ->
            profile.copy(
                name = profile.name.ifBlank { "Profile ${index + 1}" },
                spoolmanUrl = normalizeConnectionUrl(profile.spoolmanUrl),
                moonrakerUrl = normalizeConnectionUrl(profile.moonrakerUrl)
            )
        }
        .distinctBy { it.id }
        .ifEmpty {
            listOf(
                ConnectionProfile(
                    id = "default",
                    name = "Default",
                    spoolmanUrl = normalizeConnectionUrl(input.spoolmanUrl),
                    moonrakerUrl = normalizeConnectionUrl(input.moonrakerUrl)
                )
            )
        }
    val activeProfileId = input.activeConnectionProfileId
        .takeIf { id -> normalizedProfiles.any { it.id == id } }
        ?: normalizedProfiles.first().id
    val activeProfile = normalizedProfiles.first { it.id == activeProfileId }

    return SettingsSaveState(
        connectionProfiles = normalizedProfiles,
        activeConnectionProfileId = activeProfileId,
        spoolmanUrl = activeProfile.spoolmanUrl,
        moonrakerUrl = activeProfile.moonrakerUrl,
        printerIntegrationMode = input.printerIntegrationMode,
        toolheadCount = input.toolheadCount.coerceIn(1, 12),
        spoolmanSortBy = input.spoolmanSortBy.ifBlank { "" },
        bambuMasterKey = input.bambuMasterKey.trim().uppercase(),
        showCommentField = input.showCommentField
    )
}
