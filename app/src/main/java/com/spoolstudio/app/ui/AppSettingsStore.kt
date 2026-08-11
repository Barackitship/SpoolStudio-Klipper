package com.spoolstudio.app.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConnectionProfile(
    val id: String,
    val name: String,
    val spoolmanUrl: String,
    val moonrakerUrl: String
)

data class AppSettings(
    val connectionProfiles: List<ConnectionProfile>,
    val activeConnectionProfileId: String,
    val spoolmanUrl: String,
    val moonrakerUrl: String,
    val printerIntegrationMode: PrinterIntegrationMode,
    val spoolmanSortBy: String,
    val bambuMasterKey: String,
    val showLotNumber: Boolean,
    val showCommentField: Boolean,
    val showEmptySpoolWeight: Boolean,
    val materialModifierFieldDeclined: Boolean
)

object AppSettingsStore {
    private const val PREFS_NAME = "spoolstudio_prefs"
    private const val SPOOLMAN_URL_KEY = "spoolman_url"
    private const val SPOOLMAN_SORT_KEY = "spoolman_sort"
    private const val DEFAULT_URL = ""
    private const val MOONRAKER_URL_KEY = "moonraker_url"
    private const val PRINTER_INTEGRATION_MODE_KEY = "printer_integration_mode"
    private const val SHOW_LOT_NUMBER_KEY = "show_lot_number"
    private const val SHOW_COMMENT_FIELD = "show_comment_field"
    private const val SHOW_EMPTY_SPOOL_WEIGHT = "show_empty_spool_weight"
    private const val MATERIAL_MODIFIER_FIELD_DECLINED = "material_modifier_field_declined"
    private const val DEFAULT_MOONRAKER_URL = ""
    private const val BAMBU_MASTER_KEY = "bambu_master_key"
    private const val CONNECTION_PROFILES_KEY = "connection_profiles"
    private const val ACTIVE_CONNECTION_PROFILE_ID_KEY = "active_connection_profile_id"

    fun load(context: Context): AppSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacySpoolmanUrl = normalizeUrl(prefs.getString(SPOOLMAN_URL_KEY, DEFAULT_URL) ?: DEFAULT_URL)
        val legacyMoonrakerUrl = normalizeUrl(
            prefs.getString(MOONRAKER_URL_KEY, DEFAULT_MOONRAKER_URL)
                ?: DEFAULT_MOONRAKER_URL
        )
        val profiles = loadConnectionProfiles(
            storedProfiles = prefs.getString(CONNECTION_PROFILES_KEY, null),
            legacySpoolmanUrl = legacySpoolmanUrl,
            legacyMoonrakerUrl = legacyMoonrakerUrl
        )
        val activeProfileId = prefs.getString(ACTIVE_CONNECTION_PROFILE_ID_KEY, null)
            ?.takeIf { id -> profiles.any { it.id == id } }
            ?: profiles.first().id
        val activeProfile = profiles.first { it.id == activeProfileId }

        if (!prefs.contains(CONNECTION_PROFILES_KEY) || !prefs.contains(ACTIVE_CONNECTION_PROFILE_ID_KEY)) {
            saveProfiles(context, profiles, activeProfileId)
        }

        return AppSettings(
            connectionProfiles = profiles,
            activeConnectionProfileId = activeProfileId,
            spoolmanUrl = activeProfile.spoolmanUrl,
            moonrakerUrl = activeProfile.moonrakerUrl,
            printerIntegrationMode = PrinterIntegrationMode.fromStoredValue(
                prefs.getString(PRINTER_INTEGRATION_MODE_KEY, PrinterIntegrationMode.PAXX12_SPOOL_LINK.storedValue)
            ),
            spoolmanSortBy = prefs.getString(SPOOLMAN_SORT_KEY, "")?.ifBlank { "" } ?: "",
            bambuMasterKey = prefs.getString(BAMBU_MASTER_KEY, "")?.trim()?.uppercase() ?: "",
            showLotNumber = prefs.getBoolean(SHOW_LOT_NUMBER_KEY, false),
            showCommentField = prefs.getBoolean(SHOW_COMMENT_FIELD, false),
            showEmptySpoolWeight = prefs.getBoolean(SHOW_EMPTY_SPOOL_WEIGHT, false),
            materialModifierFieldDeclined = prefs.getBoolean(MATERIAL_MODIFIER_FIELD_DECLINED, false)
        )
    }

    fun saveConnectionSettings(
        context: Context,
        spoolmanUrl: String,
        moonrakerUrl: String,
        connectionProfiles: List<ConnectionProfile>,
        activeConnectionProfileId: String,
        printerIntegrationMode: PrinterIntegrationMode,
        spoolmanSortBy: String,
        bambuMasterKey: String,
        showCommentField: Boolean
    ) {
        val normalizedProfiles = normalizeProfiles(
            connectionProfiles = connectionProfiles,
            fallbackSpoolmanUrl = spoolmanUrl,
            fallbackMoonrakerUrl = moonrakerUrl
        )
        val normalizedActiveProfileId = activeConnectionProfileId
            .takeIf { id -> normalizedProfiles.any { it.id == id } }
            ?: normalizedProfiles.first().id
        val activeProfile = normalizedProfiles.first { it.id == normalizedActiveProfileId }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SPOOLMAN_URL_KEY, activeProfile.spoolmanUrl)
            .putString(MOONRAKER_URL_KEY, activeProfile.moonrakerUrl)
            .putString(CONNECTION_PROFILES_KEY, serializeConnectionProfiles(normalizedProfiles))
            .putString(ACTIVE_CONNECTION_PROFILE_ID_KEY, normalizedActiveProfileId)
            .putString(PRINTER_INTEGRATION_MODE_KEY, printerIntegrationMode.storedValue)
            .putString(SPOOLMAN_SORT_KEY, spoolmanSortBy.ifBlank { "" })
            .putString(BAMBU_MASTER_KEY, bambuMasterKey.trim().uppercase())
            .putBoolean(SHOW_COMMENT_FIELD, showCommentField)
            .apply()
    }

    fun saveMaterialModifierFieldDeclined(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(MATERIAL_MODIFIER_FIELD_DECLINED, value)
            .apply()
    }

    fun saveShowLotNumber(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOW_LOT_NUMBER_KEY, value)
            .apply()
    }

    fun saveShowEmptySpoolWeight(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SHOW_EMPTY_SPOOL_WEIGHT, value)
            .apply()
    }

    private fun saveProfiles(
        context: Context,
        profiles: List<ConnectionProfile>,
        activeProfileId: String
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CONNECTION_PROFILES_KEY, serializeConnectionProfiles(profiles))
            .putString(ACTIVE_CONNECTION_PROFILE_ID_KEY, activeProfileId)
            .apply()
    }

    private fun loadConnectionProfiles(
        storedProfiles: String?,
        legacySpoolmanUrl: String,
        legacyMoonrakerUrl: String
    ): List<ConnectionProfile> {
        val parsedProfiles = runCatching {
            if (storedProfiles.isNullOrBlank()) return@runCatching emptyList()

            val array = JSONArray(storedProfiles)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val id = item.optString("id").ifBlank { "profile-${index + 1}" }
                    val name = item.optString("name").ifBlank { "Profile ${index + 1}" }
                    add(
                        ConnectionProfile(
                            id = id,
                            name = name,
                            spoolmanUrl = normalizeUrl(item.optString("spoolmanUrl")),
                            moonrakerUrl = normalizeUrl(item.optString("moonrakerUrl"))
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        return normalizeProfiles(
            connectionProfiles = parsedProfiles,
            fallbackSpoolmanUrl = legacySpoolmanUrl,
            fallbackMoonrakerUrl = legacyMoonrakerUrl
        )
    }

    private fun normalizeProfiles(
        connectionProfiles: List<ConnectionProfile>,
        fallbackSpoolmanUrl: String,
        fallbackMoonrakerUrl: String
    ): List<ConnectionProfile> {
        val normalized = connectionProfiles
            .filter { it.id.isNotBlank() }
            .mapIndexed { index, profile ->
                profile.copy(
                    name = profile.name.ifBlank { "Profile ${index + 1}" },
                    spoolmanUrl = normalizeUrl(profile.spoolmanUrl),
                    moonrakerUrl = normalizeUrl(profile.moonrakerUrl)
                )
            }
            .distinctBy { it.id }

        return normalized.ifEmpty {
            listOf(
                ConnectionProfile(
                    id = "default",
                    name = "Default",
                    spoolmanUrl = normalizeUrl(fallbackSpoolmanUrl),
                    moonrakerUrl = normalizeUrl(fallbackMoonrakerUrl)
                )
            )
        }
    }

    private fun serializeConnectionProfiles(profiles: List<ConnectionProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("spoolmanUrl", normalizeUrl(profile.spoolmanUrl))
                    .put("moonrakerUrl", normalizeUrl(profile.moonrakerUrl))
            )
        }
        return array.toString()
    }

    private fun normalizeUrl(url: String): String = url.trim().removeSuffix("/")
}
