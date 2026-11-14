package com.spoolstudio.app.ui.components

import com.spoolstudio.app.domain.models.FilamentSpool
import org.junit.Assert.assertEquals
import org.junit.Test

class SpoolmanDropdownHelpersTest {
    @Test
    fun spoolmanDropdownLabelUsesBrandNameAndMaterial() {
        val spool = testSpool(
            id = 12,
            brand = "Generic",
            spoolmanName = "White",
            material = "PLA"
        )

        assertEquals("Generic - White - PLA", spoolmanDropdownLabel(spool))
    }

    @Test
    fun filterSpoolmanDropdownFilamentsMatchesCommonFields() {
        val whitePla = testSpool(
            id = 1,
            brand = "Generic",
            spoolmanName = "White",
            material = "PLA",
            location = "AMS 1",
            lotNr = "LOT-123"
        )
        val blackPetg = testSpool(
            id = 2,
            brand = "GST 3D",
            spoolmanName = "Black",
            material = "PETG",
            location = "Shelf B",
            lotNr = "LOT-999"
        )
        val spools = listOf(whitePla, blackPetg)

        assertEquals(listOf(whitePla), filterSpoolmanDropdownFilaments(spools, "ams"))
        assertEquals(listOf(blackPetg), filterSpoolmanDropdownFilaments(spools, "gst"))
        assertEquals(listOf(whitePla), filterSpoolmanDropdownFilaments(spools, "LOT-123"))
        assertEquals(spools, filterSpoolmanDropdownFilaments(spools, ""))
    }

    @Test
    fun filterSpoolmanDropdownFilamentsHandlesArchivedSpools() {
        val activePla = testSpool(
            id = 1,
            brand = "Generic",
            spoolmanName = "White",
            material = "PLA",
            archived = false
        )
        val archivedPetg = testSpool(
            id = 2,
            brand = "Generic",
            spoolmanName = "Black",
            material = "PETG",
            archived = true
        )
        val spools = listOf(activePla, archivedPetg)

        // When showArchivedSpools = false, archived spools must NOT appear
        assertEquals(listOf(activePla), filterSpoolmanDropdownFilaments(spools, "", showArchivedSpools = false))
        assertEquals(emptyList<FilamentSpool>(), filterSpoolmanDropdownFilaments(spools, "Black", showArchivedSpools = false))
        assertEquals(listOf(activePla), filterSpoolmanDropdownFilaments(spools, "Generic", showArchivedSpools = false))

        // When showArchivedSpools = true, archived spools MUST appear
        assertEquals(spools, filterSpoolmanDropdownFilaments(spools, "", showArchivedSpools = true))
        assertEquals(listOf(archivedPetg), filterSpoolmanDropdownFilaments(spools, "Black", showArchivedSpools = true))
        assertEquals(spools, filterSpoolmanDropdownFilaments(spools, "Generic", showArchivedSpools = true))
    }

    private fun testSpool(
        id: Int,
        brand: String,
        spoolmanName: String,
        material: String,
        location: String? = null,
        lotNr: String? = null,
        archived: Boolean = false
    ): FilamentSpool {
        return FilamentSpool(
            id = id,
            material = material,
            variant = "Basic",
            brand = brand,
            colorHex = "FFFFFF",
            minTemp = 190,
            maxTemp = 220,
            bedMinTemp = 40,
            bedMaxTemp = 65,
            location = location,
            lotNr = lotNr,
            archived = archived,
            spoolmanName = spoolmanName
        )
    }
}
