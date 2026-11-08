package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KaikkiSubsetExtractorTest {

    // Shapes below are taken from real entries of the Kaikki Spanish dump.
    private val pie =
        """{"word":"pie","pos":"noun","senses":[{"glosses":["foot"]}],""" +
            """"forms":[{"form":"pies","tags":["plural"]}]}"""

    private val piesFormOf =
        """{"word":"pies","pos":"noun","senses":[{"glosses":["plural of pie"],""" +
            """"tags":["form-of","masculine","plural"],"form_of":[{"word":"pie"}]}]}"""

    private val absolver =
        """{"word":"absolver","pos":"verb","senses":[{"glosses":["to absolve"]}],"forms":[""" +
            """{"form":"absuelvo","tags":["first-person","present","singular"]},""" +
            """{"form":"absolví","tags":["first-person","preterite","singular"]},""" +
            """{"form":"irregular","source":"conjugation","tags":["table-tags"]},""" +
            """{"form":"es-conj","source":"conjugation","tags":["inflection-template"]},""" +
            """{"form":"o-ue alternation","source":"conjugation","tags":["class"]},""" +
            """{"form":"absolverme","tags":["combined-form","infinitive"]},""" +
            """{"form":"no absuelvas","tags":["imperative","negative"]},""" +
            """{"form":"absolver","tags":["infinitive"]}]}"""

    private fun extract(vararg lines: String, wanted: Set<String>) =
        KaikkiSubsetExtractor.extract({ lines.joinToString("\n").byteInputStream() }, wanted)

    private val mucho = """{"word":"mucho","pos":"adv","senses":[{"glosses":["much, a lot"]}]}"""

    // Real shape: Kaikki lists "muy" only as an apocopic form of "mucho", and mucho does not list
    // it.
    private val muyFormOf =
        """{"word":"muy","pos":"adv","senses":[{"glosses":["apocopic form of mucho; very"],""" +
            """"tags":["apocopic","form-of"],"form_of":[{"word":"mucho"}]}]}"""

    private val dameFormOf =
        """{"word":"dame","pos":"verb","senses":[{"glosses":["second-person singular """ +
            """imperative of dar combined with me"],"tags":["form-of","imperative"],""" +
            """"form_of":[{"word":"dar"}]}]}"""

    private val dar =
        """{"word":"dar","pos":"verb","senses":[{"glosses":["to give"]}],""" +
            """"forms":[{"form":"doy","tags":["first-person","present"]}]}"""

    @Test
    fun extract_keepsALemmaWhoseWordIsInScope() {
        val subset = extract(pie, wanted = setOf("pie"))

        assertEquals(
            listOf(
                SubsetEntry("pie", "noun", "foot", listOf(SubsetForm("pies", listOf("plural"))))
            ),
            subset.entries,
        )
        assertEquals(1, subset.stats.entries)
        assertEquals(1, subset.stats.forms)
    }

    @Test
    fun extract_dropsFormOfEntriesBecauseTheirLemmaListsTheSameForms() {
        val subset = extract(piesFormOf, pie, wanted = setOf("pies", "pie"))

        assertEquals(listOf("pie"), subset.entries.map { it.word })
        assertEquals(1L, subset.stats.formOfEntries)
    }

    @Test
    fun extract_keepsAnAliasForAnInScopeFormOfWordThatNoLemmaLists() {
        val subset = extract(muyFormOf, mucho, wanted = setOf("muy", "mucho"))

        assertEquals(listOf("mucho", "muy"), subset.entries.map { it.word })
        val alias = subset.entries.last()
        assertEquals("mucho", alias.formOf)
        assertEquals("apocopic form of mucho; very", alias.gloss)
        assertEquals(listOf("apocopic", "form-of"), alias.tags)
        assertTrue(alias.forms.isEmpty())
        assertEquals(1, subset.stats.aliases)
        assertEquals(1, subset.stats.entries)
    }

    @Test
    fun extract_pullsInTheTargetLemmaOfAnAliasEvenWhenItIsNotInScopeItself() {
        val subset = extract(dameFormOf, dar, mucho, wanted = setOf("dame"))

        // "doy" and "dar" are not wanted; the alias target is kept so the gloss can be resolved.
        assertEquals(listOf("dame", "dar"), subset.entries.map { it.word }.sorted())
        assertEquals(1, subset.stats.outOfScopeEntries.toInt())
    }

    @Test
    fun extract_dropsAnAliasWhoseWordIsAlreadyCoveredByAKeptLemma() {
        val comoAdverb = """{"word":"como","pos":"adv","senses":[{"glosses":["as"]}]}"""
        val comoFormOf =
            """{"word":"como","pos":"verb","senses":[{"glosses":["first-person singular """ +
                """present of comer"],"form_of":[{"word":"comer"}]}]}"""

        val subset = extract(comoFormOf, comoAdverb, wanted = setOf("como"))

        assertEquals(listOf("adv"), subset.entries.map { it.pos })
        assertEquals(0, subset.stats.aliases)
    }

    @Test
    fun extract_reducesACombinedWithDescriptionToTheLemmaItNames() {
        val line =
            """{"word":"agradecérmelo","pos":"verb","senses":[{"glosses":["infinitive of agradecer """ +
                """combined with indirect object me and direct object lo"],"form_of":[{"word":""" +
                """"agradecer combined with indirect object me and direct object lo"}]}]}"""

        val subset = extract(line, wanted = setOf("agradecérmelo"))

        assertEquals("agradecer", subset.entries.single().formOf)
    }

    @Test
    fun extract_keepsAMultiWordTargetAsAStandaloneEntryWithItsOwnGloss() {
        val line =
            """{"word":"ONU","pos":"name","senses":[{"glosses":["acronym of Organización de las """ +
                """Naciones Unidas"],"form_of":[{"word":"Organización de las Naciones Unidas"}]}]}"""

        val subset = extract(line, wanted = setOf("onu"))

        val entry = subset.entries.single()
        assertEquals(null, entry.formOf)
        assertEquals("acronym of Organización de las Naciones Unidas", entry.gloss)
        assertEquals(1, subset.stats.aliases)
    }

    @Test
    fun extract_ignoresFormOfEntriesForWordsOutOfScope() {
        val subset = extract(muyFormOf, mucho, wanted = setOf("mucho"))

        assertEquals(listOf("mucho"), subset.entries.map { it.word })
        assertEquals(0, subset.stats.aliases)
    }

    @Test
    fun extract_keepsOneAliasPerWordPartOfSpeechAndTarget() {
        val subset = extract(muyFormOf, muyFormOf, wanted = setOf("muy"))

        assertEquals(1, subset.stats.aliases)
    }

    @Test
    fun extract_dropsFormOfEntriesThatNameNoTarget() {
        val nameless =
            """{"word":"sra","pos":"noun","senses":[{"glosses":["misspelling of Sra."],""" +
                """"tags":["misspelling"]}]}"""

        val subset = extract(nameless, wanted = setOf("sra"))

        assertTrue(subset.entries.isEmpty())
        assertEquals(1L, subset.stats.formOfEntries)
    }

    @Test
    fun extract_usesTheFirstSenseThatIsNotAVariantOfAnotherWord() {
        val mixed =
            """{"word":"vino","pos":"noun","senses":[""" +
                """{"glosses":["alternative form of vina"],"alt_of":[{"word":"vina"}]},""" +
                """{"glosses":["misspelling of vinó"],"tags":["misspelling"]},""" +
                """{"glosses":["wine"]}]}"""

        val subset = extract(mixed, wanted = setOf("vino"))

        assertEquals("wine", subset.entries.single().gloss)
    }

    @Test
    fun extract_keepsALemmaThatIsOnlyInScopeThroughOneOfItsForms() {
        val subset = extract(absolver, wanted = setOf("absuelvo"))

        assertEquals(listOf("absolver"), subset.entries.map { it.word })
    }

    @Test
    fun extract_dropsLemmasWithNothingInScope() {
        val subset = extract(pie, absolver, wanted = setOf("casa"))

        assertTrue(subset.entries.isEmpty())
        assertEquals(2L, subset.stats.outOfScopeEntries)
    }

    @Test
    fun extract_dropsMetadataCliticMultiWordAndLemmaEqualForms() {
        val subset = extract(absolver, wanted = setOf("absolver"))

        assertEquals(
            listOf(
                SubsetForm("absuelvo", listOf("first-person", "present", "singular")),
                SubsetForm("absolví", listOf("first-person", "preterite", "singular")),
            ),
            subset.entries.single().forms,
        )
    }

    @Test
    fun extract_excludesUnwantedPartsOfSpeechMultiWordHeadwordsAndEntriesWithoutGloss() {
        val lines =
            arrayOf(
                """{"word":"anti","pos":"prefix","senses":[{"glosses":["anti-"]}]}""",
                """{"word":"a la vez","pos":"adv","senses":[{"glosses":["at once"]}]}""",
                """{"word":"dicho","pos":"phrase","senses":[{"glosses":["saying"]}]}""",
                """{"word":"nada","pos":"pron","senses":[{"glosses":[]}]}""",
                """{"word":"nada","pos":"pron"}""",
                """{"pos":"noun","senses":[{"glosses":["nameless"]}]}""",
            )

        val subset = extract(*lines, wanted = setOf("anti", "a", "dicho", "nada"))

        assertTrue(subset.entries.isEmpty())
        assertEquals(6L, subset.stats.excludedEntries)
    }

    @Test
    fun extract_countsMalformedLinesAndKeepsGoing() {
        val subset = extract("not json", "[1,2]", "", pie, "{\"word\":", wanted = setOf("pie"))

        assertEquals(listOf("pie"), subset.entries.map { it.word })
        assertEquals(3L, subset.stats.malformedLines)
        assertEquals(4L, subset.stats.linesRead)
    }

    @Test
    fun extract_mergesEtymologyDuplicatesOfTheSameWordAndPartOfSpeech() {
        val first =
            """{"word":"bajo","pos":"adj","senses":[{"glosses":["short"]}],""" +
                """"forms":[{"form":"bajos","tags":["plural"]}]}"""
        val second =
            """{"word":"bajo","pos":"adj","senses":[{"glosses":["low"]}],""" +
                """"forms":[{"form":"bajos","tags":["plural"]},{"form":"baja","tags":["feminine"]}]}"""

        val subset = extract(first, second, wanted = setOf("bajo"))

        val entry = subset.entries.single()
        assertEquals("short", entry.gloss)
        assertEquals(listOf("bajos", "baja"), entry.forms.map { it.form })
        assertEquals(1L, subset.stats.duplicateEntries)
    }

    @Test
    fun extract_keepsEveryPartOfSpeechOfAnInScopeWord() {
        val adverb = """{"word":"como","pos":"adv","senses":[{"glosses":["like"]}]}"""
        val conjunction = """{"word":"como","pos":"conj","senses":[{"glosses":["as"]}]}"""

        val subset = extract(conjunction, adverb, wanted = setOf("como"))

        assertEquals(listOf("adv", "conj"), subset.entries.map { it.pos })
    }

    @Test
    fun extract_sortsByWordThenPartOfSpeechRegardlessOfInputOrder() {
        val lines =
            listOf("zorro", "casa", "árbol").map {
                """{"word":"$it","pos":"noun","senses":[{"glosses":["$it"]}]}"""
            }

        val subset = extract(*lines.toTypedArray(), wanted = setOf("zorro", "casa", "árbol"))

        assertEquals(listOf("casa", "zorro", "árbol"), subset.entries.map { it.word })
    }

    @Test
    fun extract_comparesInScopeWordsIgnoringCaseAndUnicodeNormalization() {
        val decomposed =
            "caf\u00e9".let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
        val line = """{"word":"Café","pos":"noun","senses":[{"glosses":["coffee"]}]}"""

        val subset = extract(line, wanted = setOf(KaikkiSubsetExtractor.normalize(decomposed)))

        assertEquals(listOf("Café"), subset.entries.map { it.word })
    }

    @Test
    fun toJson_writesAnAliasAsAFormOfSenseWithItsTagsAndTarget() {
        val alias = extract(muyFormOf, wanted = setOf("muy")).entries.single()

        assertEquals(
            """{"word":"muy","pos":"adv","senses":[{"glosses":["apocopic form of mucho; very"],""" +
                """"tags":["apocopic","form-of"],"form_of":[{"word":"mucho"}]}]}""",
            alias.toJson().toString(),
        )
    }

    @Test
    fun toJson_isReadableByKaikkiProcessorWithTheSameFieldNames() {
        val subset = extract(pie, wanted = setOf("pie"))
        val line = subset.entries.single().toJson().toString()

        val rows = KaikkiProcessor.parseKaikkiLine(line, mapOf("pie" to 12))

        assertEquals(listOf("pie", "pies"), rows.map { it.surfaceForm })
        assertEquals(listOf("base", "plural"), rows.map { it.grammarInfo })
        assertEquals("foot", rows.first().englishGloss)
        assertEquals(12, rows.first().frequencyRank)
    }

    @Test
    fun writeGzipJsonLines_producesDeterministicGzipOfOneJsonObjectPerLine() {
        val entries = extract(pie, absolver, wanted = setOf("pie", "absolver")).entries
        val first =
            ByteArrayOutputStream().also { KaikkiSubsetFiles.writeGzipJsonLines(entries, it) }
        val second =
            ByteArrayOutputStream().also { KaikkiSubsetFiles.writeGzipJsonLines(entries, it) }

        val text =
            GZIPInputStream(first.toByteArray().inputStream()).readBytes().toString(Charsets.UTF_8)

        assertTrue(first.toByteArray().contentEquals(second.toByteArray()))
        assertEquals(2, text.lines().filter { it.isNotEmpty() }.size)
        assertTrue(text.startsWith("""{"word":"absolver""""))
    }

    @Test
    fun readWordList_ignoresBlankLinesAndCommentsAndNormalizesCase() {
        val words =
            KaikkiSubsetFiles.readWordList("# header\nCasa\n\n  perro  \n".byteInputStream())

        assertEquals(setOf("casa", "perro"), words)
    }

    @Test
    fun metadataJson_recordsProvenanceAndCounts() {
        val stats = extract(pie, "oops", wanted = setOf("pie")).stats

        val json =
            KaikkiSubsetFiles.metadataJson(
                stats,
                25_000,
                mapOf("source" to "dump.jsonl", "sourceEtag" to "abc")
            )

        assertTrue(json.contains("\"format\": \"kaikki-lemma-subset/1\""))
        assertTrue(json.contains("\"sourceEtag\": \"abc\""))
        assertTrue(json.contains("\"wantedWords\": 25000"))
        assertTrue(json.contains("\"malformedLines\": 1"))
        assertTrue(json.endsWith("}\n"))
    }

    @Test
    fun extractToFiles_readsGzipInputAndWritesSubsetAndMetadata() {
        val dir = createTempDirectory("kaikki-extract").toFile()
        val input = File(dir, "dump.jsonl.gz")
        input.outputStream().use {
            DeterministicGzip.compress("$pie\n$piesFormOf\n".toByteArray(), it)
        }
        val output = File(dir, "out/spanish_lemmas.jsonl.gz")
        val metadata = File(dir, "out/spanish_lemmas.meta.json")

        val stats =
            KaikkiSubsetFiles.extractToFiles(
                input,
                setOf("pie"),
                output,
                metadata,
                mapOf("source" to input.name)
            )

        assertEquals(1, stats.entries)
        val written = GZIPInputStream(output.inputStream()).readBytes().toString(Charsets.UTF_8)
        assertTrue(written.contains("\"word\":\"pie\""))
        assertTrue(metadata.readText().contains("\"source\": \"dump.jsonl.gz\""))
        dir.deleteRecursively()
    }
}
