package com.anysoftkeyboard.calque.data.seed

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackagedSeedAssetTest {

    @Test
    fun packagedSeedAsset_existsAndHasAllSeedTablesPopulated() {
        val assetFile = File("app/src/main/assets/databases/seed.db")
        assertTrue("Expected packaged seed database at ${assetFile.path}", assetFile.exists())
        assertTrue(
            "Expected database size > 10KB, got ${assetFile.length()}",
            assetFile.length() > 10_000
        )

        val database = SeedDatabase.open("jdbc:sqlite:${assetFile.path}")
        database.use { db ->
            val dictRepo = SqliteDictionaryRepository(db.connection)
            val fcRepo = SqliteFalseCognatesRepository(db.connection)
            val lessonRepo = SqliteCuratedLessonsRepository(db.connection)
            val rulesRepo = SqliteCognateRulesRepository(db.connection)

            // 1. Verify dictionary entries
            val hotelWord = dictRepo.getBySurfaceForm("hotel")
            assertNotNull("Dictionary should contain 'hotel'", hotelWord)
            assertEquals("hotel", hotelWord?.lemma)
            assertTrue("hotel should be marked as cognate", hotelWord?.isCognate == true)

            val esWord = dictRepo.getBySurfaceForm("es")
            assertNotNull("Dictionary should contain 'es'", esWord)
            assertEquals("ser", esWord?.lemma)

            // 2. Verify false cognates
            val exitoCard = fcRepo.getBySurfaceForm("éxito")
            assertNotNull("False cognates should contain 'éxito'", exitoCard)
            assertEquals("exit", exitoCard?.apparentEnglish)
            assertEquals("salida", exitoCard?.realSpanishForApparent)

            val carpetasPlural = fcRepo.getBySurfaceForm("carpetas")
            assertNotNull(
                "False cognates should expand inflections like 'carpetas'",
                carpetasPlural
            )
            assertEquals("carpeta", carpetasPlural?.lemma)

            // 3. Verify curated lessons
            val lessons = lessonRepo.getAll()
            assertEquals("Should contain 8 curated lessons", 8, lessons.size)
            val levels = lessons.map { it.level }.toSet()
            assertEquals(setOf(0, 1, 2), levels)

            val firstLesson = lessonRepo.getById("lvl0_00_cognate_unlock")
            assertNotNull(firstLesson)
            assertEquals("The Cognate Unlock & Vowel Soundboard", firstLesson?.title)
            assertEquals(
                "El hotel moderno es excelente.",
                firstLesson?.parseSentences()?.first()?.sentenceEs,
            )

            // 4. Verify cognate rules
            val rules = rulesRepo.getAll()
            assertEquals("Should contain 12 cognate rules", 12, rules.size)
            val matchingRule = rulesRepo.findRuleForSpanishWord("situación")
            assertNotNull(matchingRule)
            assertEquals("tion_to_cion", matchingRule?.ruleId)
        }
    }
}
