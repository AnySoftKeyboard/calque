package com.anysoftkeyboard.calque.data.seed

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SeedDictionaryRepositoryTest {

    private lateinit var database: SeedDatabase

    @Before
    fun setUp() {
        database = SeedDatabase.open()
        SeedDatabase.createTables(database.connection)
        insertTestData()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun insertTestData() {
        val sql =
            """
            INSERT INTO dictionary (surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent()

        database.connection.prepareStatement(sql).use { stmt ->
            // Entry 1: hablar (base verb)
            stmt.setString(1, "hablar")
            stmt.setString(2, "hablar")
            stmt.setString(3, "verb")
            stmt.setString(4, "infinitive")
            stmt.setString(5, "to speak")
            stmt.setInt(6, 0)
            stmt.setInt(7, 45)
            stmt.addBatch()

            // Entry 2: hablamos (inflected)
            stmt.setString(1, "hablamos")
            stmt.setString(2, "hablar")
            stmt.setString(3, "verb")
            stmt.setString(4, "1st pl. present")
            stmt.setString(5, "to speak")
            stmt.setInt(6, 0)
            stmt.setInt(7, 320)
            stmt.addBatch()

            // Entry 3: hotel (cognate noun)
            stmt.setString(1, "hotel")
            stmt.setString(2, "hotel")
            stmt.setString(3, "noun")
            stmt.setString(4, "masculine singular")
            stmt.setString(5, "hotel")
            stmt.setInt(6, 1)
            stmt.setInt(7, 1200)
            stmt.addBatch()

            // Entry 4: raro without frequency
            stmt.setString(1, "raro")
            stmt.setString(2, "raro")
            stmt.setString(3, "adj")
            stmt.setNull(4, java.sql.Types.VARCHAR)
            stmt.setString(5, "rare / strange")
            stmt.setInt(6, 1)
            stmt.setNull(7, java.sql.Types.INTEGER)
            stmt.addBatch()

            stmt.executeBatch()
        }
    }

    @Test
    fun getBySurfaceForm_exactAndCaseInsensitiveLookup() {
        val entry = database.dictionary.getBySurfaceForm("hablar")
        assertNotNull(entry)
        assertEquals("hablar", entry?.surfaceForm)
        assertEquals("hablar", entry?.lemma)
        assertEquals("verb", entry?.pos)
        assertEquals("infinitive", entry?.grammarInfo)
        assertEquals("to speak", entry?.englishGloss)
        assertFalse(entry!!.isCognate)
        assertEquals(45, entry.frequencyRank)

        // Case-insensitivity and whitespace trim
        val upperEntry = database.dictionary.getBySurfaceForm("  HABLAMOS  ")
        assertNotNull(upperEntry)
        assertEquals("hablamos", upperEntry?.surfaceForm)

        // Unknown surface returns null
        val notFound = database.dictionary.getBySurfaceForm("desconocido")
        assertNull(notFound)
    }

    @Test
    fun getBySurfaceForms_batchLookupWithChunks() {
        val results =
            database.dictionary.getBySurfaceForms(setOf("hablar", "hotel", "desconocido", "raro"))
        assertEquals(3, results.size)
        assertTrue(results.containsKey("hablar"))
        assertTrue(results.containsKey("hotel"))
        assertTrue(results.containsKey("raro"))
        assertFalse(results.containsKey("desconocido"))

        val hotel = results["hotel"]
        assertNotNull(hotel)
        assertTrue(hotel!!.isCognate)

        val raro = results["raro"]
        assertNotNull(raro)
        assertNull(raro?.frequencyRank)
        assertNull(raro?.grammarInfo)
    }

    @Test
    fun getBySurfaceForms_emptyInputReturnsEmptyMap() {
        val results = database.dictionary.getBySurfaceForms(emptyList())
        assertTrue(results.isEmpty())
    }

    @Test
    fun getSurfacesForLemma_returnsAllInflections() {
        val surfaces = database.dictionary.getSurfacesForLemma("hablar")
        assertEquals(listOf("hablamos", "hablar"), surfaces)

        val empty = database.dictionary.getSurfacesForLemma("noexiste")
        assertTrue(empty.isEmpty())
    }

    @Test
    fun getEntriesForLemma_returnsAllEntries() {
        val entries = database.dictionary.getEntriesForLemma("hablar")
        assertEquals(2, entries.size)
        assertEquals("hablamos", entries[0].surfaceForm)
        assertEquals("hablar", entries[1].surfaceForm)
    }

    @Test
    fun searchByPrefix_matchesPrefixOrderedByFrequency() {
        val results = database.dictionary.searchByPrefix("hab")
        assertEquals(2, results.size)
        // 45 comes before 320
        assertEquals("hablar", results[0].surfaceForm)
        assertEquals("hablamos", results[1].surfaceForm)

        val noMatch = database.dictionary.searchByPrefix("xyz")
        assertTrue(noMatch.isEmpty())
    }
}
