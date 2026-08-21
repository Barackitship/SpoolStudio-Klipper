package com.spoolstudio.app.data.remote.spoolman

import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl

internal data class SpoolmanConnectionConfig(
    val retrofitBaseUrl: String,
    val authorizationHeader: String?
)

internal fun spoolmanConnectionConfig(rawBaseUrl: String): SpoolmanConnectionConfig {
    val normalizedBaseUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"
    val httpUrl = normalizedBaseUrl.toHttpUrl()
    val username = httpUrl.username
    val password = httpUrl.password

    if (username.isBlank()) {
        return SpoolmanConnectionConfig(
            retrofitBaseUrl = httpUrl.toString(),
            authorizationHeader = null
        )
    }

    val sanitizedBaseUrl = httpUrl.newBuilder()
        .username("")
        .password("")
        .build()
        .toString()

    return SpoolmanConnectionConfig(
        retrofitBaseUrl = sanitizedBaseUrl,
        authorizationHeader = Credentials.basic(username, password)
    )
}
