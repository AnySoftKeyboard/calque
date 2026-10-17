package com.anysoftkeyboard.calque.data

import com.anysoftkeyboard.calque.data.seed.SeedDatabase
import com.anysoftkeyboard.calque.data.user.UserDatabase
import com.anysoftkeyboard.calque.linguistics.ReaderEnricher
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalqueWordLookupProviderTest {

    private lateinit var seedDb: SeedDatabase
    private lateinit var userDb: UserDatabase
    private lateinit var provider: CalqueWordLookupProvider

    @Before
    fun setUp() {
        seedDb = SeedDatabase.open()
        SeedDatabase.createTables(seedDb.connection)

        userDb = UserDatabase.open()
        UserDatabase.createTables(userDb.connection)

        provider =
            CalqueWordLookupProvider(
                dictionaryRepository = seedDb.dictionary,
                falseCognatesRepository = seedDb.falseCognates,
                userWordStateRepository = userDb.wordStates,
            )

        populateSeedData()
    }

    @After
    fun tearDown() {
        seedDb.close()
        userDb.close()
    }

    private fun populateSeedData() {
        // Insert dictionary entry for 'hotel'
        val dictSql =
            """
            INSERT INTO dictionary (surface_form, lemma, pos, grammar_info, english_gloss, is_cognate, frequency_rank)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent()
        seedDb.connection.prepareStatement(dictSql).use { stmt ->
            stmt.setString(1, "hotel")
            stmt.setString(2, "hotel")
            stmt.setString(3, "noun")
            stmt.setString(4, "masc. sing.")
            stmt.setString(5, "hotel")
            stmt.setInt(6, 1)
            stmt.setInt(7, 500)
            stmt.executeUpdate()
        }

        // Insert false cognate entry for 'éxito'
        val fcSql =
            """
            INSERT INTO false_cognates (
                surface_form, lemma, apparent_english, actual_english, real_spanish_for_apparent,
                severity, disambiguation_tip, example_es, example_en
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """
                .trimIndent()
        seedDb.connection.prepareStatement(fcSql).use { stmt ->
            stmt.setString(1, "éxito")
            stmt.setString(2, "éxito")
            stmt.setString(3, "exit")
            stmt.setString(4, "success")
            stmt.setString(5, "salida")
            stmt.setString(6, "HIGH_RISK")
            stmt.setString(7, "éxito means success")
            stmt.setString(8, "Gran éxito")
            stmt.setString(9, "Great success")
            stmt.executeUpdate()
        }

        // Set user state for 'hotel'
        userDb.wordStates.recordEncounter("hotel")
        userDb.wordStates.setWordStatus("hotel", UserWordStatus.LEARNING)
    }

    @Test
    fun provider_resolvesDictionaryAndUserStateTogether() {
        val dictEntry = provider.lookupSingleWord("hotel")
        assertNotNull(dictEntry)
        assertEquals("hotel", dictEntry?.lemma)
        assertTrue(dictEntry!!.isCognate)

        val fcEntry = provider.batchLookupFalseCognates(setOf("éxito"))
        assertTrue(fcEntry.containsKey("éxito"))
        assertEquals("success", fcEntry["éxito"]?.actualEnglish)

        val statuses = provider.batchLookupUserState(setOf("hotel", "éxito", "nuevo"))
        assertEquals(UserWordStatus.LEARNING, statuses["hotel"])
        assertEquals(UserWordStatus.NEW, statuses["éxito"])
        assertEquals(UserWordStatus.NEW, statuses["nuevo"])
    }

    @Test
    fun readerEnricher_integratesWithCalqueWordLookupProvider() {
        val spans = ReaderEnricher.enrichText("El hotel tuvo gran éxito.", provider)
        val hotelSpan = spans.firstOrNull { it.normalizedWord == "hotel" }
        assertNotNull(hotelSpan)
        assertEquals(UserWordStatus.LEARNING, hotelSpan?.userStatus)
        assertTrue(hotelSpan?.dictionaryEntry?.isCognate == true)

        val exitoSpan = spans.firstOrNull { it.normalizedWord == "éxito" }
        assertNotNull(exitoSpan)
        assertNotNull(exitoSpan?.falseCognateEntry)
        assertEquals("success", exitoSpan?.falseCognateEntry?.actualEnglish)
        assertEquals("exit", exitoSpan?.falseCognateEntry?.apparentEnglish)
    }
}
