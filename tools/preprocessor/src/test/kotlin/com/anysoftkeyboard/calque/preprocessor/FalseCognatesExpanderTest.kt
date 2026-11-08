package com.anysoftkeyboard.calque.preprocessor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FalseCognatesExpanderTest {

    private fun seed(lemma: String, extra: String = "") =
        """{"lemma":"$lemma","apparent_english":"a","actual_english":"b","real_spanish_for_apparent":"c",""" +
            """"severity":"HIGH_RISK","disambiguation_tip":"tip","example_es":"es","example_en":"en"$extra}"""

    private fun row(surface: String, lemma: String, grammar: String? = null) =
        DictionaryRow(surface, lemma, "verb", grammar, "gloss", false, 100)

    private fun SeedDatabase.surfacesOf(lemma: String): List<String> =
        queryFalseCognates().filter { it.lemma == lemma }.map { it.surfaceForm }.sorted()

    @Test
    fun expand_storesOneRowPerDictionarySurfaceOfTheLemma() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatch(
                listOf(
                    row("soportar", "soportar", DictionaryGrammar.BASE),
                    row("soporta", "soportar", "present"),
                    row("soportaba", "soportar", "imperfect"),
                    row("comer", "comer", DictionaryGrammar.BASE),
                )
            )

            val count = FalseCognatesExpander.expand("[${seed("soportar")}]".byteInputStream(), db)

            assertEquals(3, count)
            assertEquals(listOf("soporta", "soportaba", "soportar"), db.surfacesOf("soportar"))
        }
    }

    @Test
    fun expand_includesAliasRowsBecauseTheyBelongToTheTargetLemma() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatch(
                listOf(
                    row("dar", "dar", DictionaryGrammar.BASE),
                    row("dame", "dar", "imperative"),
                )
            )

            FalseCognatesExpander.expand("[${seed("dar")}]".byteInputStream(), db)

            assertEquals(listOf("dame", "dar"), db.surfacesOf("dar"))
        }
    }

    @Test
    fun expand_alwaysStoresTheLowercasedLemmaEvenWithoutDictionaryRows() {
        SeedDatabase.open().use { db ->
            val result =
                FalseCognatesExpander.expandWithReport(
                    "[${seed("Constipado")}]".byteInputStream(),
                    db
                )

            assertEquals(1, result.rows)
            assertEquals(listOf("Constipado"), result.lemmasWithoutDictionaryRows)
            assertEquals("constipado", db.queryFalseCognates().single().surfaceForm)
            assertEquals("Constipado", db.queryFalseCognates().single().lemma)
        }
    }

    @Test
    fun expandWithReport_namesOnlyTheSeedsWithoutDictionaryRows() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatch(listOf(row("carpeta", "carpeta", DictionaryGrammar.BASE)))

            val result =
                FalseCognatesExpander.expandWithReport(
                    "[${seed("carpeta")},${seed("constipado")},${seed("embarazada")}]"
                        .byteInputStream(),
                    db,
                )

            assertEquals(listOf("constipado", "embarazada"), result.lemmasWithoutDictionaryRows)
            assertEquals(3, result.rows)
        }
    }

    @Test
    fun expand_aSeedKeyedByAnInflectedFormOnlyExpandsToThatForm() {
        // "embarazada" is the feminine form of the dictionary lemma "embarazado": the seed should
        // be
        // keyed by the lemma, otherwise the other forms get no false cognate card.
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatch(
                listOf(
                    row("embarazado", "embarazado", DictionaryGrammar.BASE),
                    row("embarazada", "embarazado", "feminine"),
                    row("embarazados", "embarazado", "plural"),
                )
            )

            FalseCognatesExpander.expand("[${seed("embarazada")}]".byteInputStream(), db)
            assertEquals(listOf("embarazada"), db.surfacesOf("embarazada"))

            FalseCognatesExpander.expand("[${seed("embarazado")}]".byteInputStream(), db)
            assertEquals(
                listOf("embarazada", "embarazado", "embarazados"),
                db.surfacesOf("embarazado"),
            )
        }
    }

    @Test
    fun expand_followsTheWinningDictionaryRowWhenAFormIsSharedByTwoLemmas() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatchKeepingBest(
                listOf(
                    row("fue", "ir").copy(frequencyRank = 900),
                    row("fue", "ser").copy(frequencyRank = 14)
                )
            )

            FalseCognatesExpander.expand("[${seed("ser")},${seed("ir")}]".byteInputStream(), db)

            assertEquals(listOf("fue", "ser"), db.surfacesOf("ser"))
            assertEquals(listOf("ir"), db.surfacesOf("ir"))
        }
    }

    @Test
    fun expand_copiesEveryCardFieldToEveryExpandedRow() {
        SeedDatabase.open().use { db ->
            db.insertDictionaryBatch(listOf(row("carpeta", "carpeta"), row("carpetas", "carpeta")))
            val json =
                """[{"lemma":"carpeta","apparent_english":"carpet","actual_english":"folder",""" +
                    """"real_spanish_for_apparent":"alfombra","severity":"MILD_NUANCE",""" +
                    """"disambiguation_tip":"tip","example_es":"Una carpeta.","example_en":"A folder."}]"""

            FalseCognatesExpander.expand(json.byteInputStream(), db)

            for (stored in db.queryFalseCognates()) {
                assertEquals("carpet", stored.apparentEnglish)
                assertEquals("folder", stored.actualEnglish)
                assertEquals("alfombra", stored.realSpanishForApparent)
                assertEquals("MILD_NUANCE", stored.severity)
                assertEquals("tip", stored.disambiguationTip)
                assertEquals("Una carpeta.", stored.exampleEs)
                assertEquals("A folder.", stored.exampleEn)
            }
            assertEquals(2, db.queryFalseCognates().size)
        }
    }

    @Test
    fun parseSeeds_defaultsTheSeverityToHighRiskAndSkipsSeedsWithoutALemma() {
        val json = """[{"apparent_english":"x"},{"lemma":"sopa"}]"""

        val seeds = FalseCognatesExpander.parseSeeds(json.byteInputStream())

        assertEquals(1, seeds.size)
        assertEquals("HIGH_RISK", seeds.single().severity)
        assertEquals("", seeds.single().exampleEs)
    }

    @Test
    fun expand_anEmptySeedListStoresNothing() {
        SeedDatabase.open().use { db ->
            assertEquals(0, FalseCognatesExpander.expand("[]".byteInputStream(), db))
            assertTrue(db.queryFalseCognates().isEmpty())
        }
    }

    @Test
    fun expand_rejectsInputThatIsNotAJsonArray() {
        SeedDatabase.open().use { db ->
            assertThrows(IllegalArgumentException::class.java) {
                FalseCognatesExpander.expand("""{"lemma":"sopa"}""".byteInputStream(), db)
            }
        }
    }

    @Test
    fun expand_afterTheKaikkiProcessorCoversEveryInflectionOfAVerb() {
        SeedDatabase.open().use { db ->
            val line =
                """{"word":"soportar","pos":"verb","senses":[{"glosses":["to tolerate"]}],"forms":[""" +
                    """{"form":"soporto","tags":["first-person","present"]},""" +
                    """{"form":"soportas","tags":["second-person","present"]},""" +
                    """{"form":"soportaba","tags":["imperfect"]}]}"""
            KaikkiProcessor.processStream(line.byteInputStream(), emptyMap(), db)

            val count = FalseCognatesExpander.expand("[${seed("soportar")}]".byteInputStream(), db)

            assertEquals(4, count)
            assertEquals(
                listOf("soportaba", "soportar", "soportas", "soporto"),
                db.surfacesOf("soportar"),
            )
        }
    }
}
