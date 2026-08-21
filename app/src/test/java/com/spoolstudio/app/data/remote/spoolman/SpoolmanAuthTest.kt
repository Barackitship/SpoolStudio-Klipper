package com.spoolstudio.app.data.remote.spoolman

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpoolmanAuthTest {
    @Test
    fun `connection config keeps plain URL without auth header`() {
        val config = spoolmanConnectionConfig("http://10.201.0.1:8000")

        assertEquals("http://10.201.0.1:8000/", config.retrofitBaseUrl)
        assertNull(config.authorizationHeader)
    }

    @Test
    fun `connection config strips credentials and creates basic auth header`() {
        val config = spoolmanConnectionConfig("https://username:password@spoolman.example.com")

        assertEquals("https://spoolman.example.com/", config.retrofitBaseUrl)
        assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", config.authorizationHeader)
    }

    @Test
    fun `connection config preserves path while stripping credentials`() {
        val config = spoolmanConnectionConfig("https://user:secret@spoolman.example.com/proxy")

        assertEquals("https://spoolman.example.com/proxy/", config.retrofitBaseUrl)
        assertEquals("Basic dXNlcjpzZWNyZXQ=", config.authorizationHeader)
    }
}
