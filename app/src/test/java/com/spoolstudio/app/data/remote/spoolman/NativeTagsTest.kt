package com.spoolstudio.app.data.remote.spoolman

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NativeTagsTest {
    private lateinit var server: MockWebServer
    private lateinit var service: SpoolmanService
    @Before fun setup() {
        server = MockWebServer()
        server.start()
        service = SpoolmanService(server.url("/").toString())
    }
    @After fun cleanup() { server.shutdown() }
    private fun reply(body: String = "{}", status: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body))
    }
    private fun info(version: String = "0.27.0") = reply("""{"version":"$version"}""")
    private fun spool(id: Int, uid: String = "AABB", native: Boolean = false): String = """
        {"id":$id,"filament":{"id":1,"name":"White","material":"PETG","extra":{"material_modifier":"\"2.0\""}},
         "extra":{"card_uids":"\"$uid\"","other":"\"preserve\""},"tags":${if (native) "[{\"uid\":\"$uid\"}]" else "[]"}}
    """.trimIndent()

    @Test fun versionDetectionUsesNumericComponents() {
        assertFalse(supportsNativeTags("0.26.9"))
        assertFalse(supportsNativeTags("unknown"))
        assertFalse(supportsNativeTags(null))
        assertTrue(supportsNativeTags("0.27.0"))
        assertTrue(supportsNativeTags("v0.28.0+build"))
        assertTrue(supportsNativeTags("1.0.0"))
    }
    @Test fun nativeSpoolWinsWithoutLookingAtLegacySpools() = runBlocking {
        info(); reply("[${spool(2, native = true)}]")
        assertEquals(2, service.findSpoolByCardUid("aa:bb")?.id)
        server.takeRequest()
        assertEquals("/api/v1/spool?limit=100&offset=0&allow_archived=true&tag=AABB", server.takeRequest().path)
        assertEquals(2, server.requestCount)
    }
    @Test fun filamentTagNeverFallsBackToLegacySpool() = runBlocking {
        info(); reply("[]"); reply("""[{"id":8,"name":"PETG swatch","material":"PETG","tags":[{"uid":"AABB"}]}]""")
        try { service.findSpoolByCardUid("AABB"); fail("Must not choose an arbitrary spool") }
        catch (e: FilamentTagException) { assertTrue(e.message!!.contains("#8")) }
        assertEquals(3, server.requestCount)
    }
    @Test fun olderServerUsesLegacyStore() = runBlocking {
        info("0.26.1"); reply("[${spool(3)}]")
        assertEquals(3, service.findSpoolByCardUid("AABB")?.id)
        server.takeRequest()
        assertFalse(server.takeRequest().path!!.contains("tag="))
    }
    @Test fun nativeLookupFailureDoesNotSilentlySelectLegacyOwner() = runBlocking {
        info(); reply("{}", 503)
        try { service.findSpoolByCardUid("AABB"); fail("Expected failure") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("503")) }
        assertEquals(2, server.requestCount)
    }
    @Test fun nativeOnlyDoesNotReadLegacyFallback() = runBlocking {
        service.nativeOnly = true
        info(); reply("[]"); reply("[]")
        assertNull(service.findSpoolByCardUid("AABB"))
        assertEquals(3, server.requestCount)
    }
    @Test fun nativeOnlyDoesNotWriteLegacyFields() = runBlocking {
        service.nativeOnly = true
        info(); reply()
        service.assignCardUidToSpool(2, "AABB")
        server.takeRequest()
        assertEquals("/api/v1/spool/2/tag", server.takeRequest().path)
        assertEquals(2, server.requestCount)
    }
    @Test fun conflictDoesNotStealTagWithoutExplicitMove() = runBlocking {
        service.nativeOnly = true
        info(); reply("""{"spool_id":1}""", 409)
        try { service.assignCardUidToSpool(2, "AABB"); fail("Expected conflict") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("spool #1")) }
        assertEquals(2, server.requestCount)
    }
    @Test fun explicitMoveUnlinksOldOwnerAndLinksNewOwner() = runBlocking {
        service.nativeOnly = true
        info(); reply("""{"spool_id":1}""", 409); reply(); reply()
        service.assignCardUidToSpool(2, "AABB", moveExisting = true)
        server.takeRequest(); server.takeRequest()
        assertEquals("DELETE", server.takeRequest().method)
        assertEquals("/api/v1/spool/2/tag", server.takeRequest().path)
    }
    @Test fun nativeOnlyOnOlderServerFailsBeforeWriting() = runBlocking {
        service.nativeOnly = true; info("0.26.1")
        try { service.assignCardUidToSpool(2, "AABB"); fail("Expected unsupported server") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("0.27.0")) }
        assertEquals(1, server.requestCount)
    }
    @Test fun migrationClearsOnlyConfirmedLinksAndPreservesOtherExtraFields() = runBlocking {
        info(); reply("[${spool(1, "AABB,INVALID") }]"); reply()
        reply(spool(1, "AABB,INVALID").replace("\"tags\":[]", "\"tags\":[{\"uid\":\"AABB\"}]"))
        reply(spool(1, "INVALID"))
        val report = service.migrateLegacyTags(clearLegacy = true)
        assertEquals(1, report.linked); assertEquals(1, report.cleared); assertEquals(1, report.issues.size)
        repeat(4) { server.takeRequest() }
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        val body = patch.body.readUtf8()
        assertTrue(body.contains("INVALID")); assertTrue(body.contains("preserve")); assertFalse(body.contains("AABB"))
    }
    @Test fun migrationLeavesDuplicatesAndNativeConflictsUntouched() = runBlocking {
        info(); reply("[${spool(1)},${spool(2)},${spool(3, "CCDD")}]")
        reply("""{"spool_id":9}""", 409)
        val report = service.migrateLegacyTags(clearLegacy = true)
        assertEquals(0, report.linked); assertEquals(0, report.cleared); assertEquals(3, report.issues.size)
        assertEquals(3, server.requestCount)
    }
    @Test fun migrationAbortsOnIncompleteCatalog() = runBlocking {
        info(); reply("{}", 500)
        try { service.migrateLegacyTags(true); fail("Expected failure") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("500")) }
        assertEquals(2, server.requestCount)
    }
    @Test fun copyMigrationKeepsLegacyFields() = runBlocking {
        info(); reply("[${spool(1)}]"); reply()
        val report = service.migrateLegacyTags(false)
        assertEquals(1, report.linked); assertEquals(0, report.cleared)
        assertEquals(3, server.requestCount)
    }

    @Test fun compatibilityModeWritesNativeAndLegacyStore() = runBlocking {
        info(); reply()
        reply("""[{"key":"card_uids","name":"Card UIDs","field_type":"text"}]""")
        reply("""[{"key":"variant","name":"Variant","field_type":"text"}]""")
        reply(spool(2, "")); reply("[${spool(2, "")}]"); reply(spool(2))
        service.assignCardUidToSpool(2, "AABB")
        val requests = (1..7).map { server.takeRequest() }
        assertEquals("/api/v1/spool/2/tag", requests[1].path)
        assertEquals("PATCH", requests.last().method)
        assertTrue(requests.last().body.readUtf8().contains("AABB"))
    }

    @Test fun failedMoveAttemptsToRestorePreviousOwner() = runBlocking {
        service.nativeOnly = true
        info(); reply("""{"spool_id":1}""", 409); reply(); reply("{}", 503); reply()
        try { service.assignCardUidToSpool(2, "AABB", moveExisting = true); fail("Expected failure") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("previous link restored")) }
        repeat(4) { server.takeRequest() }
        assertEquals("/api/v1/spool/1/tag", server.takeRequest().path)
    }

    @Test fun migrationRetainsLegacyIfNativeOwnerChangesBeforeCleanup() = runBlocking {
        info(); reply("[${spool(1)}]"); reply(); reply(spool(1))
        val report = service.migrateLegacyTags(true)
        assertEquals(1, report.linked); assertEquals(0, report.cleared)
        assertTrue(report.issues.single().contains("changed before cleanup"))
        assertEquals(4, server.requestCount)
    }

    @Test fun missingInfoEndpointKeepsCompatibilityForOlderServers() = runBlocking {
        reply("{}", 404); reply("[${spool(1)}]")
        assertEquals(1, service.findSpoolByCardUid("AABB")?.id)
    }

    @Test fun serverIgnoringTagFilterCannotSelectWrongSpool() = runBlocking {
        info(); reply("[${spool(1, native = false)}]")
        try { service.findSpoolByCardUid("AABB"); fail("Expected invalid result") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("invalid native tag result")) }
        assertEquals(2, server.requestCount)
    }

    @Test fun migrationIsRepeatableAfterLegacyLinksAreCleared() = runBlocking {
        info(); reply("[${spool(1)}]"); reply(); reply(spool(1, native = true)); reply(spool(1, ""))
        assertEquals(1, service.migrateLegacyTags(true).cleared)
        reply("[${spool(1, "")}]")
        val again = service.migrateLegacyTags(true)
        assertEquals(0, again.linked); assertEquals(0, again.cleared)
        assertEquals(6, server.requestCount)
    }

    @Test fun migrationRejectsAmbiguousUidSpellings() = runBlocking {
        info(); reply("[${spool(1, "0xAABB")},${spool(2, "AA:BB")}]")
        val report = service.migrateLegacyTags(true)
        assertEquals(0, report.linked); assertEquals(2, report.issues.size)
        assertEquals(2, server.requestCount)
    }

    @Test fun unlinkRemovesNativeTagAndCompatibilityMirror() = runBlocking {
        info(); reply(); reply(spool(1)); reply(spool(1, ""))
        service.unlinkCardUid(1, "AABB")
        server.takeRequest()
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method); assertEquals("/api/v1/spool/1/tag/AABB", delete.path)
        server.takeRequest()
        assertTrue(server.takeRequest().body.readUtf8().contains("card_uids"))
    }
}
