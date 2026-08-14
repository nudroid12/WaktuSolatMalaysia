package app.nudroidlabs.waktusolat.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JakimZoneResolverTest {
    @Test
    fun detectsKualaNerusAsTrg01() {
        val zone = JakimZoneResolver.resolve(
            stateRaw = "Terengganu",
            placeParts = listOf("Daerah Kuala Nerus")
        )
        assertEquals("TRG01", zone?.code)
    }

    @Test
    fun detectsDungunAsTrg04() {
        val zone = JakimZoneResolver.resolve(
            stateRaw = "Terengganu",
            placeParts = listOf("Dungun")
        )
        assertEquals("TRG04", zone?.code)
    }

    @Test
    fun oneZoneStateCanResolveFromStateOnly() {
        val zone = JakimZoneResolver.resolve(
            stateRaw = "Melaka",
            placeParts = emptyList()
        )
        assertEquals("MLK01", zone?.code)
    }

    @Test
    fun detectsKlangAsSgr03() {
        val zone = JakimZoneResolver.resolve(
            stateRaw = "Selangor",
            placeParts = listOf("Daerah Klang")
        )
        assertEquals("SGR03", zone?.code)
    }

    @Test
    fun broadSandakanNameIsNotGuessed() {
        val zone = JakimZoneResolver.resolve(
            stateRaw = "Sabah",
            placeParts = listOf("Sandakan")
        )
        assertNull(zone)
    }

    @Test
    fun detectsKualaLumpurAsWly01() {
        val zone = JakimZoneResolver.resolve(
            stateRaw = "Wilayah Persekutuan Kuala Lumpur",
            placeParts = listOf("Kuala Lumpur")
        )
        assertEquals("WLY01", zone?.code)
    }
}
