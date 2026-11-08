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
    fun dictionary_commonWordsAreNotHiddenByProperNouns() = withSeed { db ->
        // Kaikki also has "Amigo" (a surname), "Bueno", "Largo"...; they must never win the row of
        // the lowercase word.
        val dictRepo = SqliteDictionaryRepository(db.connection)
        val words =
            listOf(
                "amigo",
                "bueno",
                "grande",
                "claro",
                "año",
                "estado",
                "amor",
                "vuelta",
                "segura",
                "largo"
            )

        for (word in words) {
            val entry = dictRepo.getBySurfaceForm(word)
            assertNotNull("'$word' must be in the dictionary", entry)
            assertTrue(
                "'$word' resolved to the proper noun '${entry?.lemma}'",
                entry?.lemma?.first()?.isLowerCase() == true
            )
        }
    }

    @Test
    fun dictionary_aSpellingWithSeveralReadingsKeepsThemAllWithTheIntendedOneFirst() =
        withSeed { db ->
            val dictRepo = SqliteDictionaryRepository(db.connection)

            val padre = dictRepo.getBySurfaceForm("padre")!!
            assertEquals("noun", padre.pos)
            assertEquals("father, parent", padre.englishGloss)
            assertTrue(
                "padre keeps its adjective reading",
                padre.alternatives.any { it.pos == "adj" }
            )

            val fue = dictRepo.getBySurfaceForm("fue")!!
            assertEquals(setOf("ser", "ir"), fue.readings.map { it.lemma }.toSet())
            assertEquals("ser", fue.lemma)

            val vino = dictRepo.getBySurfaceForm("vino")!!
            assertEquals("wine", vino.englishGloss.substringBefore(" ("))
            assertTrue(vino.alternatives.any { it.lemma == "venir" })
        }

    @Test
    fun dictionary_theHandCheckedPrimariesAreRight() = withSeed { db ->
        // reading_overrides.json fixes the order of the most frequent ambiguous spellings.
        val dictRepo = SqliteDictionaryRepository(db.connection)
        val expected =
            mapOf(
                "amigo" to ("amigo" to "noun"),
                "sí" to ("sí" to "particle"),
                "sé" to ("saber" to "verb"),
                "mira" to ("mirar" to "verb"),
                "veces" to ("vez" to "noun"),
                "creo" to ("creer" to "verb"),
                "siento" to ("sentir" to "verb"),
                "hombre" to ("hombre" to "noun"),
                "nada" to ("nada" to "pron"),
                "he" to ("haber" to "verb"),
                "vamos" to ("ir" to "verb"),
            )

        for ((surface, lemmaAndPos) in expected) {
            val entry = dictRepo.getBySurfaceForm(surface)!!
            assertEquals("$surface", lemmaAndPos, entry.lemma to entry.pos)
        }
    }

    @Test
    fun dictionary_everySpellingHasExactlyOnePrimaryReadingAndGaplessOrder() = withSeed { db ->
        val sql =
            "SELECT surface_form FROM dictionary GROUP BY surface_form " +
                "HAVING MIN(reading_order) <> 0 OR MAX(reading_order) <> COUNT(*) - 1 " +
                "OR COUNT(DISTINCT reading_order) <> COUNT(*) LIMIT 5"
        val broken = mutableListOf<String>()
        db.connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs -> while (rs.next()) broken.add(rs.getString(1)) }
        }

        assertTrue("Spellings whose reading_order is not 0..n-1: $broken", broken.isEmpty())
    }

    @Test
    fun dictionary_noProperNounOrGenderCounterpartCompetesWithACommonWord() = withSeed { db ->
        // A proper noun is only noise when a common word shares its spelling ("jesús" and "OK"
        // have nothing but capitalized lemmas, which is fine).
        val sql =
            "SELECT surface_form FROM dictionary d WHERE " +
                "(grammar_info = 'proper noun' AND EXISTS (SELECT 1 FROM dictionary other " +
                "WHERE other.surface_form = d.surface_form AND other.grammar_info <> 'proper noun')) " +
                "OR (grammar_info IN ('masculine', 'feminine') AND surface_form <> lemma " +
                "AND EXISTS (SELECT 1 FROM dictionary own WHERE own.surface_form = d.surface_form " +
                "AND own.grammar_info = 'base')) LIMIT 5"
        val noisy = mutableListOf<String>()
        db.connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs -> while (rs.next()) noisy.add(rs.getString(1)) }
        }

        assertTrue("Noise readings next to a common word: $noisy", noisy.isEmpty())
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
    fun falseCognateRows_everyOneMatchesADictionaryRowOfTheSameLemma() = withSeed { db ->
        // A false cognate card belongs to a word the dictionary knows under the same lemma, in
        // any of its readings ("fue" is a form of ser and of ir). A seed keyed by an inflected
        // form ("embarazada" instead of "embarazado"), or by a lemma the dictionary files under
        // another one, breaks that.
        val sql =
            "SELECT f.surface_form, f.lemma, '' FROM false_cognates f WHERE NOT EXISTS " +
                "(SELECT 1 FROM dictionary d WHERE d.surface_form = f.surface_form " +
                "AND d.lemma = f.lemma)"
        val mismatches = mutableListOf<String>()
        db.connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                while (rs.next()) mismatches.add(
                    "${rs.getString(1)}: card ${rs.getString(2)}, dictionary ${rs.getString(3)}"
                )
            }
        }

        assertTrue(
            "False cognate rows without a matching dictionary row: $mismatches",
            mismatches.isEmpty()
        )
    }

    @Test
    fun falseCognateSeeds_everyLemmaExpandsToItsFormsNotJustItself() = withSeed { db ->
        val sql =
            "SELECT f.lemma, COUNT(*) AS cards, " +
                "(SELECT COUNT(DISTINCT d.surface_form) FROM dictionary d " +
                "WHERE d.lemma = f.lemma) AS forms " +
                "FROM false_cognates f GROUP BY f.lemma HAVING cards < forms"
        val incomplete = mutableListOf<String>()
        db.connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { rs ->
                while (rs.next()) incomplete.add(
                    "${rs.getString(1)} (${rs.getInt(2)} of ${rs.getInt(3)} forms)"
                )
            }
        }

        assertTrue("Lemmas that miss some of their forms: $incomplete", incomplete.isEmpty())
    }

    @Test
    fun lessons_areTheHandWrittenOnesPlusTheGeneratedTatoebaLessons() = withSeed { db ->
        val lessons = SqliteCuratedLessonsRepository(db.connection).getAll()
        val tatoeba = lessons.filter { it.lessonId.startsWith("tatoeba_") }

        assertTrue("Should contain the hand-written lessons", lessons.size - tatoeba.size >= 12)
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
    fun handWrittenLessons_everyWordResolvesInTheDictionary() = withSeed { db ->
        // The reader looks every tapped word up; a hand-written lesson with a word that is not in
        // the dictionary would show an empty peek card. extra_words.txt (extract-seed-words) is
        // what guarantees this, so it must be regenerated when a lesson changes.
        val dictRepo = SqliteDictionaryRepository(db.connection)
        val word = Regex("""\p{L}+""")
        val lessons = SqliteCuratedLessonsRepository(db.connection).getAll()

        val unresolved =
            lessons
                .filterNot { it.lessonId.startsWith("tatoeba_") }
                .flatMap { lesson ->
                    lesson.parseSentences().flatMap { sentence ->
                        word.findAll(sentence.sentenceEs).map { it.value.lowercase() }.toList()
                    }
                }
                .distinct()
                .filter { dictRepo.getBySurfaceForm(it) == null }

        assertTrue(
            "Words of hand-written lessons missing from the dictionary: $unresolved",
            unresolved.isEmpty()
        )
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
