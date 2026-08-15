package app.nudroidlabs.waktusolat.doa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoaCatalogTest {
    @Test
    fun catalogHasUniqueCompleteEntries() {
        val entries = DoaCatalog.entries

        assertEquals(13, entries.size)
        assertEquals(entries.size, entries.map { it.id }.toSet().size)

        entries.forEach { doa ->
            assertTrue(doa.id.isNotBlank())
            assertTrue(doa.title.isNotBlank())
            assertTrue(doa.category.isNotBlank())
            assertTrue(doa.arabic.isNotBlank())
            assertTrue(doa.rumi.isNotBlank())
            assertTrue(doa.meaningMalay.isNotBlank())
            assertTrue(doa.reference.isNotBlank())
        }
    }

    @Test
    fun referencesComeFromDeclaredQuranOrHadithCollections() {
        val allowedPrefixes = listOf(
            "Al-Quran, ",
            "Sahih al-Bukhari ",
            "Sahih Muslim ",
            "Sunan Abi Dawud ",
            "Jami' at-Tirmidhi "
        )

        DoaCatalog.entries.forEach { doa ->
            assertTrue(
                "Unexpected reference for ${doa.id}: ${doa.reference}",
                allowedPrefixes.any(doa.reference::startsWith)
            )
        }
    }
}
