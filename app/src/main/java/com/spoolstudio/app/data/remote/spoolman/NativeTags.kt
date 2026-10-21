package com.spoolstudio.app.data.remote.spoolman

import retrofit2.Response

data class SpoolmanInfo(val version: String? = null)
data class NativeTag(val uid: String, val format: String? = null)
data class TagLinkRequest(val uid: String)

fun canonicalTagUid(value: String): String {
    val uid = value.replace(Regex("[\\s:_.-]+"), "").uppercase().removePrefix("0X")
    require(uid.matches(Regex("[0-9A-F]{1,64}"))) { "Invalid hardware UID: $value" }
    return uid
}

class FilamentTagException(id: Int, name: String) : IllegalStateException(
    "This tag identifies filament #$id ($name), not a specific spool. Select a spool explicitly."
)

fun supportsNativeTags(version: String?): Boolean {
    val match = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+].*)?$").matchEntire(version.orEmpty().trim()) ?: return false
    val (major, minor, patch) = match.destructured
    val numbers = listOf(major, minor, patch).map { it.toIntOrNull() ?: return false }
    return numbers[0] > 0 || numbers[1] >= 27
}

internal fun <T> Response<T>.checked(operation: String): T? {
    check(isSuccessful) { "$operation failed (HTTP ${code()}): ${errorBody()?.string().orEmpty()}" }
    return body()
}

data class TagMigrationReport(
    val linked: Int,
    val cleared: Int,
    val issues: List<String>
) {
    val summary: String get() = "$linked native links confirmed; $cleared legacy links cleared." +
        if (issues.isEmpty()) "" else "\nEntries requiring attention:\n" + issues.joinToString("\n")
}
