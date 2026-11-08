package com.anysoftkeyboard.calque.preprocessor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KaikkiProcessorTest {

    // Shapes below are taken from real entries of the Kaikki Spanish dump.
    private val hablar =
        """{"word":"hablar","pos":"verb","senses":[{"glosses":["to talk; to speak"]}],"forms":[""" +
            """{"form":"hablo","tags":["first-person","present","singular"]},""" +
            """{"form":"irregular","source":"conjugation","tags":["table-tags"]},""" +
            """{"form":"es-conj","source":"conjugation","tags":["inflection-template"]},""" +
            """{"form":"hablarme","tags":["combined-form","infinitive"]},""" +
            """{"form":"no hables","tags":["imperative","negative"]},""" +
            """{"form":"hablar","tags":["infinitive"]}]}"""

    private val mucho = """{"word":"mucho","pos":"adv","senses":[{"glosses":["much, a lot"]}]}"""

    private val muy =
        """{"word":"muy","pos":"adv","senses":[{"glosses":["apocopic form of mucho; very"],""" +
            """"tags":["apocopic","form-of"],"form_of":[{"word":"mucho"}]}]}"""

    private val dame =
        """{"word":"dame","pos":"verb","senses":[{"glosses":["inflection of dar:"],""" +
            """"tags":["form-of","imperative"],"form_of":[{"word":"dar"}]}]}"""

    private val dar =
        """{"word":"dar","pos":"verb","senses":[{"glosses":["to give"]}],""" +
            """"forms":[{"form":"doy","tags":["first-person","present"]}]}"""

    private val pies =
        """{"word":"pies","pos":"noun","senses":[{"glosses":["plural of pie"],""" +
            """"tags":["form-of"],"form_of":[{"word":"pie"}]}]}"""

    private fun process(
        vararg lines: String,
        ranks: Map<String, Int> = emptyMap(),
        batchSize: Int = 5000,
        block: (SeedDatabase, KaikkiProcessingStats) -> Unit,
    ) {
        SeedDatabase.open().use { db ->
            val stats =
                KaikkiProcessor.processStreamWithStats(
                    lines.joinToString("\n").byteInputStream(),
                    ranks,
                    db,
                    batchSize,
                )
            block(db, stats)
        }
    }

    // ----- lemma entries ------------------------------------------------------------------------

    @Test
    fun lemmaEntry_becomesABaseRowAndOneRowPerUsableForm() {
        process(hablar) { db, stats ->
            val base = db.queryDictionaryRow("hablar")!!
            assertEquals(DictionaryGrammar.BASE, base.grammarInfo)
            assertEquals("to talk; to speak", base.englishGloss)
            assertEquals("verb", base.pos)
            assertEquals("hablar", base.lemma)

            val form = db.queryDictionaryRow("hablo")!!
            assertEquals("first-person, present, singular", form.grammarInfo)
            assertEquals("hablar", form.lemma)
            assertEquals(2, db.countDictionaryRows())
            assertEquals(2L, stats.rows)
            assertEquals(1L, stats.lemmas)
        }
    }

    @Test
    fun lemmaEntry_skipsMetadataCliticMultiWordAndLemmaEqualForms() {
        process(hablar) { db, _ ->
            for (junk in listOf("irregular", "es-conj", "hablarme", "no hables", "no")) {
                assertNull(junk, db.queryDictionaryRow(junk))
            }
        }
    }

    @Test
    fun lemmaEntry_keepsTheOriginalCaseOfTheLemmaButLowercasesTheSurface() {
        val line = """{"word":"Madrid","pos":"name","senses":[{"glosses":["Madrid"]}]}"""

        process(line) { db, _ ->
            val row = db.queryDictionaryRow("madrid")!!
            assertEquals("Madrid", row.lemma)
        }
    }

    @Test
    fun lemmaEntry_detectsCognatesFromTheLemmaAndGloss() {
        val line =
            """{"word":"conversación","pos":"noun","senses":[{"glosses":["conversation"]}],""" +
                """"forms":[{"form":"conversaciones","tags":["plural"]}]}"""

        process(line) { db, _ ->
            assertTrue(db.queryDictionaryRow("conversación")!!.isCognate)
            assertTrue(db.queryDictionaryRow("conversaciones")!!.isCognate)
        }
    }

    @Test
    fun lemmaEntry_aFormWithoutTagsHasNoGrammarInfo() {
        val line =
            """{"word":"casa","pos":"noun","senses":[{"glosses":["house"]}],"forms":[{"form":"casas"}]}"""

        process(line) { db, _ -> assertNull(db.queryDictionaryRow("casas")!!.grammarInfo) }
    }

    // ----- frequency ----------------------------------------------------------------------------

    @Test
    fun frequency_theLemmaRankIsUsedForTheBaseRowAndItsForms() {
        process(hablar, ranks = mapOf("hablar" to 150)) { db, _ ->
            assertEquals(150, db.queryDictionaryRow("hablar")!!.frequencyRank)
            assertEquals(150, db.queryDictionaryRow("hablo")!!.frequencyRank)
        }
    }

    @Test
    fun frequency_aFormUsesItsOwnRankWhenThatIsBetterThanTheLemmaRank() {
        process(hablar, ranks = mapOf("hablar" to 150, "hablo" to 40)) { db, _ ->
            assertEquals(150, db.queryDictionaryRow("hablar")!!.frequencyRank)
            assertEquals(40, db.queryDictionaryRow("hablo")!!.frequencyRank)
        }
    }

    @Test
    fun frequency_aFormUsesItsOwnRankWhenTheLemmaHasNone() {
        process(hablar, ranks = mapOf("hablo" to 900)) { db, _ ->
            assertNull(db.queryDictionaryRow("hablar")!!.frequencyRank)
            assertEquals(900, db.queryDictionaryRow("hablo")!!.frequencyRank)
        }
    }

    @Test
    fun frequency_isMissingWhenNeitherTheLemmaNorTheFormIsRanked() {
        process(hablar) { db, _ -> assertNull(db.queryDictionaryRow("hablo")!!.frequencyRank) }
    }

    // ----- form-of entries and aliases ----------------------------------------------------------

    @Test
    fun formOfEntry_isNotImportedAsALemmaWhenItsWordIsNotRanked() {
        process(pies, mucho, ranks = mapOf("mucho" to 30)) { db, stats ->
            assertNull(db.queryDictionaryRow("pies"))
            assertEquals(1L, stats.skippedFormOfEntries)
            assertEquals(0L, stats.aliases)
        }
    }

    @Test
    fun alias_belongsToItsTargetLemmaAndKeepsItsOwnGlossAndTags() {
        process(muy, mucho, ranks = mapOf("muy" to 43, "mucho" to 120)) { db, stats ->
            val row = db.queryDictionaryRow("muy")!!
            assertEquals("mucho", row.lemma)
            assertEquals("adv", row.pos)
            assertEquals("apocopic form of mucho; very", row.englishGloss)
            assertEquals("apocopic", row.grammarInfo)
            assertEquals(43, row.frequencyRank)
            assertEquals(1L, stats.aliases)
        }
    }

    @Test
    fun alias_isResolvedEvenWhenItComesBeforeItsTarget() {
        val darCognate = """{"word":"dar","pos":"verb","senses":[{"glosses":["to give"]}]}"""

        process(dame, darCognate, ranks = mapOf("dame" to 406)) { db, _ ->
            assertEquals("inflection of dar: to give", db.queryDictionaryRow("dame")!!.englishGloss)
        }
    }

    @Test
    fun alias_aGlossCutOffAfterAColonGetsTheTargetGlossAppended() {
        process(dar, dame, ranks = mapOf("dame" to 406)) { db, _ ->
            val row = db.queryDictionaryRow("dame")!!
            assertEquals("inflection of dar: to give", row.englishGloss)
            assertEquals("imperative", row.grammarInfo)
            assertEquals("dar", row.lemma)
        }
    }

    @Test
    fun alias_withoutItsTargetInTheDataKeepsItsOwnDescription() {
        process(dame, ranks = mapOf("dame" to 406)) { db, _ ->
            val row = db.queryDictionaryRow("dame")!!
            assertEquals("dar", row.lemma)
            assertEquals("inflection of dar", row.englishGloss)
            assertEquals(false, row.isCognate)
        }
    }

    @Test
    fun alias_takesTheCognateFlagOfItsTarget() {
        val hotel = """{"word":"hotel","pos":"noun","senses":[{"glosses":["hotel"]}]}"""
        val hoteles =
            """{"word":"hoteles","pos":"noun","senses":[{"glosses":["plural of hotel"],""" +
                """"tags":["form-of","plural"],"form_of":[{"word":"hotel"}]}]}"""

        process(hotel, hoteles, ranks = mapOf("hoteles" to 2000)) { db, _ ->
            assertTrue(db.queryDictionaryRow("hoteles")!!.isCognate)
            assertEquals("plural", db.queryDictionaryRow("hoteles")!!.grammarInfo)
        }
    }

    @Test
    fun alias_prefersTheTargetWithTheSamePartOfSpeech() {
        val darNoun = """{"word":"dar","pos":"noun","senses":[{"glosses":["a dar (noun)"]}]}"""

        process(darNoun, dar, dame, ranks = mapOf("dame" to 406)) { db, _ ->
            assertEquals("inflection of dar: to give", db.queryDictionaryRow("dame")!!.englishGloss)
        }
    }

    @Test
    fun alias_aCapitalizedVariantWithNoTargetIsAProperNounOfItsOwn() {
        val onu =
            """{"word":"ONU","pos":"name","senses":[{"glosses":["acronym of Organización de las Naciones Unidas"],""" +
                """"form_of":[{"word":"Organización de las Naciones Unidas"}]}]}"""

        process(onu, ranks = mapOf("onu" to 3000)) { db, _ ->
            val row = db.queryDictionaryRow("onu")!!
            assertEquals("ONU", row.lemma)
            assertEquals(DictionaryGrammar.PROPER_NOUN, row.grammarInfo)
            assertEquals("acronym of Organización de las Naciones Unidas", row.englishGloss)
        }
    }

    @Test
    fun alias_aLowercaseVariantWithNoTargetIsItsOwnBaseLemma() {
        val line =
            """{"word":"cia","pos":"noun","senses":[{"glosses":["acronym of central"],""" +
                """"form_of":[{"word":"Agencia Central de Inteligencia"}]}]}"""

        process(line, ranks = mapOf("cia" to 4000)) { db, _ ->
            assertEquals(DictionaryGrammar.BASE, db.queryDictionaryRow("cia")!!.grammarInfo)
        }
    }

    // ----- proper nouns -------------------------------------------------------------------------

    // Real shapes: Kaikki has "Amigo" (a surname) and "amigo" (friend) as separate entries, and the
    // surface form is lowercased, so they compete for the row of "amigo".
    private val amigoSurname =
        """{"word":"Amigo","pos":"name","senses":[{"glosses":["a surname"]}],""" +
            """"forms":[{"form":"Amigos","tags":["plural"]}]}"""

    private val amigoWord =
        """{"word":"amigo","pos":"noun","senses":[{"glosses":["friend"]}],""" +
            """"forms":[{"form":"amigos","tags":["plural"]}]}"""

    @Test
    fun properNoun_neverHidesTheCommonWordWhicheverComesFirst() {
        for (lines in listOf(listOf(amigoSurname, amigoWord), listOf(amigoWord, amigoSurname))) {
            process(*lines.toTypedArray(), ranks = mapOf("amigo" to 164)) { db, _ ->
                val row = db.queryDictionaryRow("amigo")!!
                assertEquals("amigo", row.lemma)
                assertEquals("friend", row.englishGloss)
                assertEquals(DictionaryGrammar.BASE, row.grammarInfo)
            }
        }
    }

    @Test
    fun properNoun_aloneIsKeptMarkedAsAProperNounAndNotAsACognate() {
        val madrid = """{"word":"Madrid","pos":"name","senses":[{"glosses":["Madrid"]}]}"""

        process(madrid, ranks = mapOf("madrid" to 800)) { db, _ ->
            val row = db.queryDictionaryRow("madrid")!!
            assertEquals("Madrid", row.lemma)
            assertEquals(DictionaryGrammar.PROPER_NOUN, row.grammarInfo)
            assertEquals(false, row.isCognate)
            assertEquals(800, row.frequencyRank)
        }
    }

    @Test
    fun properNoun_formsAreNotListed() {
        process(amigoSurname) { db, _ -> assertNull(db.queryDictionaryRow("amigos")) }
    }

    @Test
    fun properNoun_doesNotHideAnInflectedFormOfACommonWord() {
        // "Estado" the name must lose against "estado", the participle of estar.
        val estadoName = """{"word":"Estado","pos":"name","senses":[{"glosses":["state"]}]}"""
        val estar =
            """{"word":"estar","pos":"verb","senses":[{"glosses":["to be"]}],""" +
                """"forms":[{"form":"estado","tags":["participle"]}]}"""

        process(estadoName, estar, ranks = mapOf("estado" to 194, "estar" to 20)) { db, _ ->
            assertEquals("estar", db.queryDictionaryRow("estado")!!.lemma)
        }
    }

    @Test
    fun alias_isKeptWhenNoRanksAreGiven() {
        process(muy, mucho) { db, stats ->
            assertEquals("mucho", db.queryDictionaryRow("muy")!!.lemma)
            assertEquals(1L, stats.aliases)
        }
    }

    @Test
    fun alias_neverReplacesABaseRowOfTheSameWord() {
        val comoAdverb = """{"word":"como","pos":"adv","senses":[{"glosses":["as"]}]}"""
        val comoVerb =
            """{"word":"como","pos":"verb","senses":[{"glosses":["first-person singular present of comer"],""" +
                """"form_of":[{"word":"comer"}]}]}"""

        process(comoVerb, comoAdverb, ranks = mapOf("como" to 20)) { db, _ ->
            val row = db.queryDictionaryRow("como")!!
            assertEquals("as", row.englishGloss)
            assertEquals("como", row.lemma)
        }
    }

    // ----- robustness ---------------------------------------------------------------------------

    @Test
    fun malformedLinesAreCountedAndSkippedInsteadOfAbortingTheRun() {
        process("not json", mucho, "{\"word\":", "[1]") { db, stats ->
            assertEquals(3L, stats.malformedLines)
            assertEquals("much, a lot", db.queryDictionaryRow("mucho")!!.englishGloss)
        }
    }

    @Test
    fun unwantedEntriesAreExcluded() {
        val prefix = """{"word":"anti","pos":"prefix","senses":[{"glosses":["anti-"]}]}"""
        val phrase = """{"word":"a la vez","pos":"adv","senses":[{"glosses":["at once"]}]}"""
        val noGloss = """{"word":"nada","pos":"pron","senses":[{"glosses":[]}]}"""

        process(prefix, phrase, noGloss, mucho) { db, stats ->
            assertEquals(3L, stats.excludedEntries)
            assertEquals(1, db.countDictionaryRows())
        }
    }

    @Test
    fun theResultDoesNotDependOnTheBatchSize() {
        val all = arrayOf(hablar, mucho, muy, dame, dar, pies)
        val ranks = mapOf("muy" to 43, "dame" to 406, "hablar" to 150)
        var expected: List<DictionaryRow?> = emptyList()
        var actual: List<DictionaryRow?> = emptyList()
        val surfaces = listOf("hablar", "hablo", "mucho", "muy", "dame", "dar", "doy", "pies")

        process(*all, ranks = ranks, batchSize = 5000) { db, _ ->
            expected = surfaces.map { db.queryDictionaryRow(it) }
        }
        process(*all, ranks = ranks, batchSize = 1) { db, _ ->
            actual = surfaces.map { db.queryDictionaryRow(it) }
        }

        assertEquals(expected, actual)
    }

    @Test
    fun blankLinesAreIgnored() {
        process("", "   ", mucho, "") { db, stats ->
            assertEquals(1, db.countDictionaryRows())
            assertEquals(0L, stats.malformedLines)
        }
    }

    // ----- parseKaikkiLine and the extractor's output -------------------------------------------

    @Test
    fun parseKaikkiLine_returnsNoRowsForMalformedAndExcludedLines() {
        assertEquals(
            emptyList<DictionaryRow>(),
            KaikkiProcessor.parseKaikkiLine("oops", emptyMap())
        )
        assertEquals(
            emptyList<DictionaryRow>(),
            KaikkiProcessor.parseKaikkiLine("""{"word":"anti","pos":"prefix"}""", emptyMap()),
        )
    }

    @Test
    fun parseKaikkiLine_aFormOfLineBecomesAnAliasRowForItsTarget() {
        val row = KaikkiProcessor.parseKaikkiLine(pies, emptyMap()).single()

        assertEquals("pies", row.surfaceForm)
        assertEquals("pie", row.lemma)
    }

    @Test
    fun parseKaikkiLine_aliasKeepsItsOwnGlossBecauseNoTargetIsKnown() {
        val rows = KaikkiProcessor.parseKaikkiLine(dame, mapOf("dame" to 406))

        assertEquals("inflection of dar", rows.single().englishGloss)
        assertEquals("dar", rows.single().lemma)
    }

    @Test
    fun theExtractorsOutputRoundTripsThroughTheProcessor() {
        val extracted =
            KaikkiSubsetExtractor.extract(
                {
                    listOf(hablar, mucho, muy, dame, dar, pies).joinToString("\n").byteInputStream()
                },
                setOf("hablo", "muy", "dame"),
            )
        val lines = extracted.entries.joinToString("\n") { it.toJson().toString() }

        process(lines, ranks = mapOf("muy" to 43, "dame" to 406, "hablo" to 77)) { db, stats ->
            assertEquals("mucho", db.queryDictionaryRow("muy")!!.lemma)
            assertEquals("dar", db.queryDictionaryRow("dame")!!.lemma)
            assertEquals("hablar", db.queryDictionaryRow("hablo")!!.lemma)
            assertEquals(77, db.queryDictionaryRow("hablo")!!.frequencyRank)
            assertEquals(0L, stats.malformedLines)
        }
    }
}
