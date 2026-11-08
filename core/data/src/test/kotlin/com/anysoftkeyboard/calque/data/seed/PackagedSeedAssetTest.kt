package com.anysoftkeyboard.calque.data.seed

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the generated `seed.db` that the app packages (`//app:seed_db_asset`): the hand-written
 * seeds are in it, and so is the data extracted from Kaikki, SUBTLEX-ESP and Tatoeba.
 */
class PackagedSeedAssetTest {

    private fun <T> withSeed(block: (SeedDatabase) -> T): T {
        val assetFile = File("app/src/main/assets/databases/seed.db")
        assertTrue("Expected packaged seed database at ${assetFile.path}", assetFile.exists())
        assertTrue(
            "Expected database size > 10KB, got ${assetFile.length()}",
            assetFile.length() > 10_000,
        )
        return SeedDatabase.open("jdbc:sqlite:${assetFile.path}").use(block)
    }

    @Test
    fun dictionary_holdsTheCuratedRowsAndTheKaikkiForms() = withSeed { db ->
        val dictRepo = SqliteDictionaryRepository(db.connection)

        val hotelWord = dictRepo.getBySurfaceForm("hotel")
        assertNotNull("Dictionary should contain 'hotel'", hotelWord)
        assertEquals("hotel", hotelWord?.lemma)
        assertTrue("hotel should be marked as cognate", hotelWord?.isCognate == true)

        val esWord = dictRepo.getBySurfaceForm("es")
        assertNotNull("Dictionary should contain 'es'", esWord)
        assertEquals("ser", esWord?.lemma)

        // The full dictionary comes from the Kaikki subset: inflections and variants resolve.
        assertEquals("hablar", dictRepo.getBySurfaceForm("hablaba")?.lemma)
        assertEquals("dar", dictRepo.getBySurfaceForm("dame")?.lemma)
        assertEquals("tuyo", dictRepo.getBySurfaceForm("tu")?.lemma)
        assertNotNull(
            "palabra must keep its SUBTLEX rank",
            dictRepo.getBySurfaceForm("palabra")?.frequencyRank,
        )
        assertNull("Kaikki metadata must not become words", dictRepo.getBySurfaceForm("es-conj"))
    }

    @Test
    fun falseCognates_areExpandedAcrossInflections() = withSeed { db ->
        val fcRepo = SqliteFalseCognatesRepository(db.connection)

        val exitoCard = fcRepo.getBySurfaceForm("éxito")
        assertNotNull("False cognates should contain 'éxito'", exitoCard)
        assertEquals("exit", exitoCard?.apparentEnglish)
        assertEquals("salida", exitoCard?.realSpanishForApparent)

        val carpetasPlural = fcRepo.getBySurfaceForm("carpetas")
        assertNotNull("False cognates should expand inflections like 'carpetas'", carpetasPlural)
        assertEquals("carpeta", carpetasPlural?.lemma)
    }

    @Test
    fun lessons_areTheHandWrittenOnesPlusTheGeneratedTatoebaLessons() = withSeed { db ->
        val lessons = SqliteCuratedLessonsRepository(db.connection).getAll()
        val tatoeba = lessons.filter { it.lessonId.startsWith("tatoeba_") }

        assertEquals("Should contain the 8 hand-written lessons", 8, lessons.size - tatoeba.size)
        assertTrue("Should contain generated Tatoeba lessons", tatoeba.isNotEmpty())
        assertEquals(setOf(0, 1, 2), lessons.map { it.level }.toSet())
        assertEquals(
            "Lesson ids must be unique",
            lessons.size,
            lessons.map { it.lessonId }.toSet().size,
        )
        for (lesson in tatoeba) {
            val sentences = lesson.parseSentences()
            assertTrue("${lesson.lessonId} must have sentences", sentences.isNotEmpty())
            assertTrue(
                sentences.all { it.sentenceEs.isNotBlank() && it.translationEn.isNotBlank() }
            )
        }
        for (level in 0..2) {
            val sequences = lessons.filter { it.level == level }.map { it.sequenceOrder }
            assertEquals(
                "Unique sequence_order in level $level",
                sequences.size,
                sequences.toSet().size
            )
        }
    }

    @Test
    fun firstLesson_isTheHandWrittenCognateUnlock() = withSeed { db ->
        val firstLesson =
            SqliteCuratedLessonsRepository(db.connection).getById("lvl0_00_cognate_unlock")

        assertNotNull(firstLesson)
        assertEquals("The Cognate Unlock & Vowel Soundboard", firstLesson?.title)
        assertEquals(
            "El hotel moderno es excelente.",
            firstLesson?.parseSentences()?.first()?.sentenceEs,
        )
    }

    @Test
    fun cognateRules_areAllThere() = withSeed { db ->
        val rulesRepo = SqliteCognateRulesRepository(db.connection)

        assertEquals("Should contain 12 cognate rules", 12, rulesRepo.getAll().size)
        val matchingRule = rulesRepo.findRuleForSpanishWord("situación")
        assertNotNull(matchingRule)
        assertEquals("tion_to_cion", matchingRule?.ruleId)
    }
}
