package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The readings of one spelling: numbering, curated and override precedence, clean-up. */
class ReadingsTest {

    /** A lemma row by default; pass a [grammar] such as "preterite" for an inflected form. */
    private fun row(surface: String, lemma: String, pos: String, gloss: String, rank: Int? = null) =
        DictionaryRow(surface, lemma, pos, DictionaryGrammar.BASE, gloss, false, rank)

    private fun DictionaryRow.inflected(tags: String?) = copy(grammarInfo = tags)

    private fun DictionaryRow.proper() = copy(grammarInfo = DictionaryGrammar.PROPER_NOUN)

    private fun withDb(block: (SeedDatabase) -> Unit) = SeedDatabase.open().use(block)

    private fun SeedDatabase.glosses(surface: String) =
        queryDictionaryRows(surface).map { it.englishGloss }

    // ----- numbering ----------------------------------------------------------------------------

    @Test
    fun finalize_numbersTheReadingsOfASpellingFromZeroInPriorityOrder() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("fue", "ir", "verb", "went", rank = 900).inflected("preterite"),
                row("fue", "ser", "verb", "was", rank = 40).inflected("preterite"),
                row("fue", "fue", "noun", "a fue (base)", rank = 7000),
            )
        )

        db.finalizeReadings()

        assertEquals(
            listOf(Triple("fue", "noun", 0), Triple("ser", "verb", 1), Triple("ir", "verb", 2)),
            db.queryReadingOrders("fue").sortedBy { it.third },
        )
    }

    @Test
    fun finalize_aSpellingWithOneReadingGetsZero() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(row("casa", "casa", "noun", "house", rank = 300))
        )

        db.finalizeReadings()

        assertEquals(listOf(Triple("casa", "noun", 0)), db.queryReadingOrders("casa"))
    }

    @Test
    fun finalize_numbersEachSpellingIndependently() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("a", "a", "prep", "to", rank = 4),
                row("a", "a", "intj", "ah", rank = 4),
                row("b", "b", "noun", "bee", rank = 9),
            )
        )

        db.finalizeReadings()

        assertEquals(listOf(0, 1), db.queryReadingOrders("a").map { it.third })
        assertEquals(listOf(0), db.queryReadingOrders("b").map { it.third })
    }

    @Test
    fun finalize_isIdempotent() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("para", "para", "prep", "for", rank = 20),
                row("para", "parar", "verb", "stop", rank = 20).inflected("imperative")
            )
        )

        db.finalizeReadings()
        val first = db.queryReadingOrders("para")
        db.finalizeReadings()

        assertEquals(first, db.queryReadingOrders("para"))
    }

    @Test
    fun readingsAreOrderedTheSameWayBeforeAndAfterFinalizing() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("fue", "ir", "verb", "went", rank = 900).inflected("preterite"),
                row("fue", "ser", "verb", "was", rank = 40).inflected("preterite"),
            )
        )
        val before = db.glosses("fue")

        db.finalizeReadings()

        assertEquals(before, db.glosses("fue"))
        assertEquals("was", db.queryDictionaryRow("fue")?.englishGloss)
    }

    // ----- curated seed -------------------------------------------------------------------------

    @Test
    fun curatedRow_becomesPrimaryEvenWithAWorseRankAndKeepsTheOtherReadingsAsAlternatives() =
        withDb { db ->
            db.insertDictionaryBatchKeepingBest(
                listOf(
                    row("como", "como", "adv", "like", rank = 22),
                    row("como", "comer", "verb", "I eat", rank = 22).inflected("present")
                )
            )

            db.insertCuratedDictionaryBatch(
                listOf(row("como", "como", "conj", "as (curated)", rank = 5000))
            )
            db.finalizeReadings()

            assertEquals(listOf("as (curated)", "like", "I eat"), db.glosses("como"))
        }

    @Test
    fun curatedRow_replacesTheKaikkiRowWithTheSameKey() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(listOf(row("hotel", "hotel", "noun", "inn", rank = 1)))

        db.insertCuratedDictionaryBatch(
            listOf(row("hotel", "hotel", "noun", "hotel (curated)", rank = 500))
        )
        db.finalizeReadings()

        assertEquals(listOf("hotel (curated)"), db.glosses("hotel"))
    }

    @Test
    fun curatedRow_keepsTheSameLemmaUnderAnotherPartOfSpeechAsAnAlternative() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("embarazado", "embarazado", "noun", "a pregnant person"),
                row("embarazado", "embarazado", "adj", "pregnant")
            )
        )

        db.insertCuratedDictionaryBatch(
            listOf(row("embarazado", "embarazado", "adj", "pregnant (curated)"))
        )
        db.finalizeReadings()

        assertEquals(listOf("pregnant (curated)", "a pregnant person"), db.glosses("embarazado"))
    }

    @Test
    fun curatedRows_keepTheirFileOrderWhenSeveralShareASpelling() = withDb { db ->
        db.insertCuratedDictionaryBatch(
            listOf(
                row("sí", "sí", "intj", "yes", rank = 300),
                row("sí", "sí", "pron", "himself", rank = 20),
            )
        )
        db.finalizeReadings()

        assertEquals(listOf("yes", "himself"), db.glosses("sí"))
    }

    // ----- overrides ----------------------------------------------------------------------------

    private val padreNoun = row("padre", "padre", "noun", "father", rank = 155)
    private val padreAdj = row("padre", "padre", "adj", "cool, acceptable", rank = 155)

    @Test
    fun promote_makesTheNamedPartOfSpeechPrimary() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(listOf(padreNoun, padreAdj))
        assertEquals("cool, acceptable", db.queryDictionaryRow("padre")?.englishGloss) // adj < noun

        val unmatched = db.promoteReadings(listOf(ReadingPreference("padre", "noun")))
        db.finalizeReadings()

        assertTrue(unmatched.isEmpty())
        assertEquals(listOf("father", "cool, acceptable"), db.glosses("padre"))
    }

    @Test
    fun promote_aLemmaNarrowsTheChoice() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("fue", "ir", "verb", "went", rank = 100).inflected("preterite"),
                row("fue", "ser", "verb", "was", rank = 900).inflected("preterite"),
            )
        )

        db.promoteReadings(listOf(ReadingPreference("fue", "verb", lemma = "ser")))
        db.finalizeReadings()

        assertEquals("was", db.queryDictionaryRow("fue")?.englishGloss)
    }

    @Test
    fun promote_reportsPreferencesThatMatchNothingAndStillAppliesTheRest() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(listOf(padreNoun, padreAdj))

        val unmatched =
            db.promoteReadings(
                listOf(
                    ReadingPreference("padre", "verb"),
                    ReadingPreference("nada", "pron"),
                    ReadingPreference("padre", "noun", lemma = "madre"),
                    ReadingPreference("padre", "noun"),
                )
            )
        db.finalizeReadings()

        assertEquals(
            listOf(
                ReadingPreference("padre", "verb"),
                ReadingPreference("nada", "pron"),
                ReadingPreference("padre", "noun", lemma = "madre"),
            ),
            unmatched,
        )
        assertEquals("father", db.queryDictionaryRow("padre")?.englishGloss)
    }

    @Test
    fun promote_anOverrideBeatsTheCuratedSeedForTheSameSpelling() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(listOf(padreNoun, padreAdj))
        db.insertCuratedDictionaryBatch(listOf(row("padre", "padre", "adj", "cool (curated)")))

        db.promoteReadings(listOf(ReadingPreference("padre", "noun")))
        db.finalizeReadings()

        assertEquals(listOf("father", "cool (curated)"), db.glosses("padre"))
    }

    @Test
    fun promote_theFirstOverrideOfASpellingWins() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(listOf(padreNoun, padreAdj))

        db.promoteReadings(
            listOf(ReadingPreference("padre", "adj"), ReadingPreference("padre", "noun"))
        )
        db.finalizeReadings()

        assertEquals("cool, acceptable", db.queryDictionaryRow("padre")?.englishGloss)
    }

    // ----- proper nouns -------------------------------------------------------------------------

    @Test
    fun finalize_dropsAProperNounThatSharesASpellingWithACommonWord() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("amigo", "Amigo", "name", "a surname", rank = 164).proper(),
                row("amigo", "amigo", "noun", "friend", rank = 164),
            )
        )

        db.finalizeReadings()

        assertEquals(listOf("friend"), db.glosses("amigo"))
    }

    @Test
    fun finalize_keepsAProperNounThatNoCommonWordCompetesWith() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(row("madrid", "Madrid", "name", "Madrid", rank = 800).proper())
        )

        db.finalizeReadings()

        assertEquals("Madrid", db.queryDictionaryRow("madrid")?.lemma)
    }

    @Test
    fun finalize_dropsProperNounsAlsoWhenTheCommonWordIsAnInflectedForm() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(
            listOf(
                row("estado", "Estado", "name", "state", rank = 194).proper(),
                row("estado", "estar", "verb", "been", rank = 20).inflected("participle"),
            )
        )

        db.finalizeReadings()

        assertEquals(listOf("been"), db.glosses("estado"))
    }

    // ----- the overrides file -------------------------------------------------------------------

    @Test
    fun parse_readsEntriesLowercasesTheSpellingAndSkipsIncompleteOnes() {
        val json =
            """[{"surface_form":"Padre","pos":"noun"},{"surface_form":"fue","pos":"verb","lemma":"ser"},""" +
                """{"pos":"noun"},{"surface_form":"x"},{"surface_form":" ","pos":"noun"}]"""

        val parsed = ReadingOverridesProcessor.parse(json.byteInputStream())

        assertEquals(
            listOf(ReadingPreference("padre", "noun"), ReadingPreference("fue", "verb", "ser")),
            parsed,
        )
    }

    @Test
    fun apply_reportsHowManyWereRequestedAndWhichDidNotMatch() = withDb { db ->
        db.insertDictionaryBatchKeepingBest(listOf(padreNoun, padreAdj))
        val json =
            """[{"surface_form":"padre","pos":"noun"},{"surface_form":"madre","pos":"noun"}]"""

        val result = ReadingOverridesProcessor.apply(json.byteInputStream(), db)

        assertEquals(2, result.requested)
        assertEquals(listOf(ReadingPreference("madre", "noun")), result.unmatched)
    }

    // ----- pipeline -----------------------------------------------------------------------------

    @Test
    fun pipeline_appliesOverridesAfterTheSeedAndReportsTheAmbiguousSpellings() {
        val dir = createTempDirectory("calque_readings_pipeline").toFile()
        val kaikki =
            File(dir, "kaikki.jsonl").apply {
                writeText(
                    """{"word":"padre","pos":"adj","senses":[{"glosses":["cool"]}]}""" +
                        "\n" +
                        """{"word":"padre","pos":"noun","senses":[{"glosses":["father"]}]}""" +
                        "\n" +
                        """{"word":"casa","pos":"noun","senses":[{"glosses":["house"]}]}""" +
                        "\n"
                )
            }
        val overrides =
            File(dir, "overrides.json").apply {
                writeText(
                    """[{"surface_form":"padre","pos":"noun"},{"surface_form":"nada","pos":"pron"}]"""
                )
            }
        val dbFile = File(dir, "seed.db")

        val summary =
            PreprocessorPipeline(
                    PipelineConfig(dbFile, kaikkiFile = kaikki, readingOverridesFile = overrides)
                )
                .execute()

        assertEquals(3, summary.dictionaryRows)
        assertEquals(1, summary.ambiguousSpellings)
        assertEquals(listOf(ReadingPreference("nada", "pron")), summary.unmatchedReadingOverrides)
        SeedDatabase.open("jdbc:sqlite:${dbFile.absolutePath}").use { db ->
            assertEquals(listOf("father", "cool"), db.glosses("padre"))
            assertNull(db.queryDictionaryRow("nada"))
        }
        dir.deleteRecursively()
    }
}
