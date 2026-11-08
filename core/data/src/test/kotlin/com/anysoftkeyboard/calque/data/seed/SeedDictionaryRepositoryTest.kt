package com.anysoftkeyboard.calque.data.seed

import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
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
            INSERT INTO dictionary (surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank, reading_order)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0)
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

    private fun entry(surface: String, gloss: String) =
        DictionaryEntry(surface, surface, "noun", null, gloss, false, null)

    /** Inserts a reading of [surface]; [lemmaAndPos] is the lemma and the part of speech. */
    private fun insertReading(
        surface: String,
        lemmaAndPos: Pair<String, String>,
        gloss: String,
        order: Int,
        rank: Int? = null,
    ) {
        val sql =
            "INSERT INTO dictionary (surface_form, lemma, pos, grammar_info, english_gloss, " +
                "is_cognate, frequency_rank, reading_order) VALUES (?, ?, ?, NULL, ?, 0, ?, ?)"
        database.connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, surface)
            stmt.setString(2, lemmaAndPos.first)
            stmt.setString(3, lemmaAndPos.second)
            stmt.setString(4, gloss)
            if (rank != null) stmt.setInt(5, rank) else stmt.setNull(5, java.sql.Types.INTEGER)
            stmt.setInt(6, order)
            stmt.executeUpdate()
        }
    }

    @Test
    fun getBySurfaceForm_returnsThePrimaryReadingWithTheOthersAsAlternativesInOrder() {
        // Inserted out of order on purpose: reading_order decides, not insertion order.
        insertReading("padre", "padre" to "adj", "cool", order = 1)
        insertReading("padre", "padre" to "noun", "father", order = 0)
        insertReading("padre", "padrar" to "verb", "to sire", order = 2)

        val entry = database.dictionary.getBySurfaceForm("padre")!!

        assertEquals("father", entry.englishGloss)
        assertEquals("noun", entry.pos)
        assertEquals(listOf("cool", "to sire"), entry.alternatives.map { it.englishGloss })
        assertTrue(entry.alternatives.all { it.alternatives.isEmpty() })
        assertEquals(listOf("father", "cool", "to sire"), entry.readings.map { it.englishGloss })
    }

    @Test
    fun getBySurfaceForm_aSingleReadingHasNoAlternatives() {
        val entry = database.dictionary.getBySurfaceForm("hotel")!!

        assertTrue(entry.alternatives.isEmpty())
        assertEquals(listOf(entry), entry.readings)
    }

    @Test
    fun getBySurfaceForms_groupsTheReadingsOfEachSpellingSeparately() {
        insertReading("fue", "ser" to "verb", "was", order = 0, rank = 14)
        insertReading("fue", "ir" to "verb", "went", order = 1, rank = 14)
        insertReading("para", "para" to "prep", "for", order = 0)

        val results = database.dictionary.getBySurfaceForms(listOf("fue", "para", "hotel"))

        assertEquals(setOf("fue", "para", "hotel"), results.keys)
        assertEquals(listOf("went"), results.getValue("fue").alternatives.map { it.englishGloss })
        assertTrue(results.getValue("para").alternatives.isEmpty())
    }

    @Test
    fun getSurfacesForLemma_listsASpellingOnceEvenWhenTheLemmaHasSeveralReadingsOfIt() {
        insertReading("padre", "padre" to "noun", "father", order = 0)
        insertReading("padre", "padre" to "adj", "cool", order = 1)

        assertEquals(listOf("padre"), database.dictionary.getSurfacesForLemma("padre"))
        assertEquals(2, database.dictionary.getEntriesForLemma("padre").size)
    }

    @Test
    fun searchByPrefix_returnsOnlyPrimaryReadings() {
        insertReading("padre", "padre" to "noun", "father", order = 0, rank = 155)
        insertReading("padre", "padre" to "adj", "cool", order = 1, rank = 155)

        val results = database.dictionary.searchByPrefix("pad")

        assertEquals(listOf("father"), results.map { it.englishGloss })
    }

    @Test
    fun groupReadings_keepsTheOrderOfTheGivenEntries() {
        val a = entry("x", "first")
        val b = entry("x", "second")
        val c = entry("y", "only")

        val grouped = groupReadings(listOf(a, b, c))

        assertEquals(listOf("x", "y"), grouped.keys.toList())
        assertEquals(listOf(b), grouped.getValue("x").alternatives)
        assertEquals("first", grouped.getValue("x").englishGloss)
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
