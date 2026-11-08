package com.anysoftkeyboard.calque.preprocessor

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DictionaryCollisionTest {

    private fun row(
        surface: String,
        lemma: String,
        gloss: String,
        rank: Int? = null,
        grammar: String? = "inflected",
    ) =
        DictionaryRow(
            surface,
            lemma,
            "verb",
            grammar,
            gloss,
            isCognate = false,
            frequencyRank = rank
        )

    private fun base(surface: String, gloss: String, rank: Int? = null, pos: String = "adv") =
        row(surface, surface, gloss, rank, DictionaryGrammar.BASE).copy(pos = pos)

    private fun SeedDatabase.glossOf(surface: String): String? =
        queryDictionaryRow(surface)?.englishGloss

    private fun SeedDatabase.rowCount(): Int = countDictionaryRows()

    @Test
    fun keepingBest_aBaseRowBeatsAnInflectedRowInAnyOrder() {
        for (order in listOf(true, false)) {
            SeedDatabase.open().use { db ->
                val inflected = row("como", "comer", "I eat", rank = 1)
                val lemma = base("como", "as, like", rank = 500)

                val batches = if (order) listOf(inflected, lemma) else listOf(lemma, inflected)
                batches.forEach { db.insertDictionaryBatchKeepingBest(listOf(it)) }

                assertEquals("order=$order", "as, like", db.glossOf("como"))
            }
        }
    }

    @Test
    fun keepingBest_theLowerFrequencyRankWinsBetweenTwoInflectedRows() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(row("fue", "ir", "went", rank = 900)))
            db.insertDictionaryBatchKeepingBest(listOf(row("fue", "ser", "was", rank = 40)))
            db.insertDictionaryBatchKeepingBest(listOf(row("fue", "dar", "gave", rank = 2000)))

            assertEquals("was", db.glossOf("fue"))
        }
    }

    @Test
    fun keepingBest_theLowerFrequencyRankWinsBetweenTwoBaseRows() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(base("bajo", "under", rank = 300)))
            db.insertDictionaryBatchKeepingBest(listOf(base("bajo", "bass", rank = 120)))

            assertEquals("bass", db.glossOf("bajo"))
        }
    }

    @Test
    fun keepingBest_aMissingRankLosesToAnyRank() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(row("sé", "ser", "be", rank = null)))
            db.insertDictionaryBatchKeepingBest(listOf(row("sé", "saber", "know", rank = 99_999)))
            db.insertDictionaryBatchKeepingBest(listOf(row("sé", "saber", "never", rank = null)))

            assertEquals("know", db.glossOf("sé"))
        }
    }

    @Test
    fun keepingBest_aTieKeepsTheRowThatWasInsertedFirst() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(base("como", "as", rank = 50, pos = "conj")))
            db.insertDictionaryBatchKeepingBest(
                listOf(base("como", "like", rank = 50, pos = "adv"))
            )

            assertEquals("as", db.glossOf("como"))
        }
    }

    @Test
    fun keepingBest_aTieBetweenTwoUnrankedRowsKeepsTheFirst() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(row("x", "a", "first")))
            db.insertDictionaryBatchKeepingBest(listOf(row("x", "b", "second")))

            assertEquals("first", db.glossOf("x"))
        }
    }

    @Test
    fun keepingBest_resolvesConflictsInsideOneBatch() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(
                listOf(
                    row("fue", "ir", "went", rank = 900),
                    row("fue", "ser", "was", rank = 40),
                    row("fue", "ir", "walked", rank = 700),
                    base("fue", "fue (base)", rank = 5000),
                    row("fue", "ser", "also was", rank = 1),
                )
            )

            assertEquals("fue (base)", db.glossOf("fue"))
            assertEquals(1, db.rowCount())
        }
    }

    @Test
    fun keepingBest_replacesEveryColumnOfTheLosingRow() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(row("fue", "ir", "went", rank = 900)))
            db.insertDictionaryBatchKeepingBest(
                listOf(
                    row("fue", "ser", "was", rank = 40, grammar = null)
                        .copy(pos = "aux", isCognate = true)
                )
            )

            val info = db.lookupWord("fue")
            assertEquals(40, info?.frequencyRank)
            assertEquals(true, info?.isCognate)
            assertNull(info?.grammar)
            assertEquals(listOf("fue"), db.querySurfacesForLemma("ser"))
            assertEquals(emptyList<String>(), db.querySurfacesForLemma("ir"))
        }
    }

    @Test
    fun keepingBest_doesNotTouchOtherSurfaceForms() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(row("a", "x", "one"), row("b", "y", "two")))
            db.insertDictionaryBatchKeepingBest(listOf(row("a", "x", "newer", rank = 1)))

            assertEquals("newer", db.glossOf("a"))
            assertEquals("two", db.glossOf("b"))
            assertEquals(2, db.rowCount())
        }
    }

    private fun proper(surface: String, lemma: String, gloss: String, rank: Int? = null) =
        row(surface, lemma, gloss, rank, DictionaryGrammar.PROPER_NOUN).copy(pos = "name")

    @Test
    fun keepingBest_aProperNounLosesToTheCommonWordInAnyOrderEvenWithABetterRank() {
        for (properFirst in listOf(true, false)) {
            SeedDatabase.open().use { db ->
                val name = proper("amigo", "Amigo", "a surname", rank = 1)
                val word = base("amigo", "friend", rank = 900, pos = "noun")

                val inserts = if (properFirst) listOf(name, word) else listOf(word, name)
                inserts.forEach { db.insertDictionaryBatchKeepingBest(listOf(it)) }

                assertEquals("properFirst=$properFirst", "friend", db.glossOf("amigo"))
            }
        }
    }

    @Test
    fun keepingBest_aProperNounLosesToAnInflectedFormToo() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(
                listOf(
                    proper("estado", "Estado", "state", rank = 194),
                    row("estado", "estar", "been", rank = 900)
                )
            )

            assertEquals("been", db.glossOf("estado"))
        }
    }

    @Test
    fun keepingBest_aProperNounIsKeptWhenItIsTheOnlyRow() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(
                listOf(proper("madrid", "Madrid", "Madrid", rank = 800))
            )

            assertEquals("Madrid", db.queryDictionaryRow("madrid")!!.lemma)
        }
    }

    @Test
    fun keepingBest_twoProperNounsFollowTheRankRule() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(
                listOf(
                    proper("luz", "Luz", "first", rank = 900),
                    proper("luz", "LUZ", "second", rank = 100)
                )
            )

            assertEquals("second", db.glossOf("luz"))
        }
    }

    @Test
    fun plainInsert_alwaysReplacesSoCuratedRowsWin() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(listOf(base("hotel", "inn", rank = 1)))
            db.insertDictionaryBatch(listOf(base("hotel", "hotel", rank = 5000)))

            assertEquals("hotel", db.glossOf("hotel"))
        }
    }

    @Test
    fun pipeline_curatedDictionarySeedWinsOverKaikkiForTheSameWord() {
        val dir = createTempDirectory("calque_collision_pipeline").toFile()
        val kaikki =
            File(dir, "kaikki.jsonl").apply {
                writeText(
                    """{"word":"hotel","pos":"noun","senses":[{"glosses":["inn"]}]}""" +
                        "\n" +
                        """{"word":"casa","pos":"noun","senses":[{"glosses":["house"]}]}""" +
                        "\n"
                )
            }
        val seed =
            File(dir, "dictionary_seed.json").apply {
                writeText(
                    """[{"surface_form":"hotel","lemma":"hotel","pos":"noun","grammar_info":"masc. sing.",""" +
                        """"english_gloss":"hotel (curated)","is_cognate":true,"frequency_rank":500}]"""
                )
            }
        val dbFile = File(dir, "seed.db")

        PreprocessorPipeline(PipelineConfig(dbFile, kaikkiFile = kaikki, dictionarySeedFile = seed))
            .execute()

        SeedDatabase.open("jdbc:sqlite:${dbFile.absolutePath}").use { db ->
            assertEquals("hotel (curated)", db.glossOf("hotel"))
            assertEquals("house", db.glossOf("casa"))
        }
        dir.deleteRecursively()
    }

    @Test
    fun pipeline_readsAGzipKaikkiFile() {
        val dir = createTempDirectory("calque_gzip_pipeline").toFile()
        val kaikki = File(dir, "lemmas.jsonl.gz")
        kaikki.outputStream().use {
            DeterministicGzip.compress(
                """{"word":"casa","pos":"noun","senses":[{"glosses":["house"]}]}""".toByteArray(),
                it,
            )
        }
        val dbFile = File(dir, "seed.db")

        val summary = PreprocessorPipeline(PipelineConfig(dbFile, kaikkiFile = kaikki)).execute()

        assertEquals(1L, summary.dictionaryRows)
        SeedDatabase.open("jdbc:sqlite:${dbFile.absolutePath}").use { db ->
            assertEquals("house", db.glossOf("casa"))
        }
        dir.deleteRecursively()
    }
}
