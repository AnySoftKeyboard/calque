package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TatoebaCandidateExtractorTest {

    private val wanted =
        setOf(
            "me",
            "gusta",
            "el",
            "caf\u00e9",
            "con",
            "leche",
            "mucho",
            "tom",
            "ella",
            "come",
            "pan"
        )

    private fun spanish(vararg rows: Triple<Long, String, String>) =
        rows.joinToString("\n") { "${it.first}\t${it.second}\t${it.third}" }

    private fun sentence(id: Long, text: String) = Triple(id, "spa", text)

    private fun extract(
        spanishRows: String,
        englishRows: String,
        links: Map<Long, Long>,
        maxPairs: Int = 100,
    ) =
        TatoebaCandidateExtractor.extract(
            openSpanish = { spanishRows.byteInputStream() },
            openEnglish = { englishRows.byteInputStream() },
            links = links,
            wantedWords = wanted,
            maxPairs = maxPairs,
        )

    // ----- readLinks ---------------------------------------------------------------------------

    @Test
    fun readLinks_keepsTheLowestEnglishIdPerSpanishId() {
        val links =
            TatoebaCandidateExtractor.readLinks("10\t900\n10\t300\n11\t5\n".byteInputStream())

        assertEquals(mapOf(10L to 300L, 11L to 5L), links)
    }

    @Test
    fun readLinks_skipsMalformedLines() {
        val text = "x\t1\n2\ty\n3\n\n4\t40\textra\n"

        assertEquals(mapOf(4L to 40L), TatoebaCandidateExtractor.readLinks(text.byteInputStream()))
    }

    // ----- rejectionFor ------------------------------------------------------------------------

    @Test
    fun rejectionFor_acceptsAPlainInScopeSentence() {
        assertNull(
            TatoebaCandidateExtractor.rejectionFor("Me gusta el caf\u00e9 con leche.", wanted)
        )
    }

    @Test
    fun rejectionFor_acceptsQuestionAndExclamationMarks() {
        assertNull(
            TatoebaCandidateExtractor.rejectionFor("\u00bfMe gusta el caf\u00e9 con leche?", wanted)
        )
        assertNull(
            TatoebaCandidateExtractor.rejectionFor("\u00a1Me gusta mucho el caf\u00e9!", wanted)
        )
    }

    @Test
    fun rejectionFor_rejectsTooFewAndTooManyWords() {
        assertEquals(
            TatoebaRejection.WORD_COUNT,
            TatoebaCandidateExtractor.rejectionFor("Me gusta el", wanted),
        )
        val long = List(15) { "caf\u00e9" }.joinToString(" ")
        assertEquals(
            TatoebaRejection.WORD_COUNT,
            TatoebaCandidateExtractor.rejectionFor(long, wanted)
        )
    }

    @Test
    fun rejectionFor_acceptsTheWordCountBoundaries() {
        val four = "me gusta el caf\u00e9"
        val fourteen = List(14) { "caf\u00e9" }.joinToString(" ")

        assertNull(TatoebaCandidateExtractor.rejectionFor(four, wanted))
        assertNull(TatoebaCandidateExtractor.rejectionFor(fourteen, wanted))
    }

    @Test
    fun rejectionFor_rejectsMoreThanOneSentence() {
        assertEquals(
            TatoebaRejection.MULTIPLE_SENTENCES,
            TatoebaCandidateExtractor.rejectionFor(
                "Me gusta el caf\u00e9. Come pan con leche.",
                wanted
            ),
        )
        assertEquals(
            TatoebaRejection.MULTIPLE_SENTENCES,
            TatoebaCandidateExtractor.rejectionFor(
                "Me gusta el caf\u00e9; come pan con leche",
                wanted
            ),
        )
    }

    @Test
    fun rejectionFor_rejectsTextLongerThanTheCharacterLimit() {
        val text = "caf\u00e9 ".repeat(TatoebaCandidateExtractor.MAX_CHARACTERS)

        assertEquals(
            TatoebaRejection.TOO_LONG,
            TatoebaCandidateExtractor.rejectionFor(text, wanted)
        )
    }

    @Test
    fun rejectionFor_rejectsDigitsHyphensAndApostrophes() {
        for (text in
            listOf(
                "Me gusta el caf\u00e9 2",
                "Me gusta el anti-caf\u00e9",
                "Me gusta el caf\u00e9 d'ella"
            )) {
            assertEquals(
                text,
                TatoebaRejection.UNSUPPORTED_TOKEN,
                TatoebaCandidateExtractor.rejectionFor(text, wanted + setOf("anti", "d", "ella")),
            )
        }
    }

    @Test
    fun rejectionFor_rejectsCapitalizedWordsAfterTheFirstButAllowsASentenceInitialOne() {
        assertEquals(
            TatoebaRejection.PROPER_NAME,
            TatoebaCandidateExtractor.rejectionFor("Me gusta el caf\u00e9 de Tom", wanted),
        )
        assertEquals(
            TatoebaRejection.PROPER_NAME,
            TatoebaCandidateExtractor.rejectionFor("Me gusta el CAF\u00c9 con leche", wanted),
        )
        assertNull(TatoebaCandidateExtractor.rejectionFor("Tom come pan con leche", wanted))
    }

    @Test
    fun rejectionFor_rejectsAnyWordOutsideTheScopeList() {
        assertEquals(
            TatoebaRejection.OUT_OF_SCOPE_WORD,
            TatoebaCandidateExtractor.rejectionFor("Me gusta el t\u00e9 con leche", wanted),
        )
    }

    @Test
    fun rejectionFor_comparesWordsIgnoringCase() {
        assertNull(
            TatoebaCandidateExtractor.rejectionFor(
                "Me Gusta El Caf\u00e9 Con Leche".lowercase(),
                wanted
            )
        )
    }

    // ----- extract -----------------------------------------------------------------------------

    @Test
    fun extract_pairsAcceptedSpanishSentencesWithTheirEnglishText() {
        val result =
            extract(
                spanish(sentence(10, "Me gusta el caf\u00e9 con leche.")),
                "300\teng\tI like coffee with milk.",
                mapOf(10L to 300L),
            )

        assertEquals(
            listOf(
                TatoebaPair(10, 300, "Me gusta el caf\u00e9 con leche.", "I like coffee with milk.")
            ),
            result.pairs,
        )
        assertEquals(1, result.stats.selected)
    }

    @Test
    fun extract_countsEachRejectionReason() {
        val result =
            extract(
                spanish(
                    sentence(1, "Me gusta el caf\u00e9 con leche."),
                    sentence(2, "Ella come pan con leche."),
                    sentence(3, "Me gusta el t\u00e9 con leche."),
                    sentence(4, "Me gusta."),
                    Triple(5, "eng", "Not Spanish at all"),
                    sentence(6, "Me gusta mucho el caf\u00e9."),
                ),
                "100\teng\tI like coffee with milk.\n101\teng\tShe eats bread with milk.",
                mapOf(1L to 100L, 2L to 101L, 3L to 102L, 4L to 103L),
            )

        assertEquals(listOf(1L, 2L), result.pairs.map { it.spanishId })
        assertEquals(6L, result.stats.spanishLinesRead)
        assertEquals(1L, result.stats.rejections[TatoebaRejection.OUT_OF_SCOPE_WORD])
        assertEquals(1L, result.stats.rejections[TatoebaRejection.WORD_COUNT])
        assertEquals(1L, result.stats.rejections[TatoebaRejection.MALFORMED_LINE])
        assertEquals(1L, result.stats.rejections[TatoebaRejection.NO_TRANSLATION])
    }

    @Test
    fun extract_dropsASentenceWhoseEnglishTextIsMissingAndCountsIt() {
        val result =
            extract(
                spanish(sentence(1, "Me gusta el caf\u00e9 con leche.")),
                "999\teng\tUnrelated",
                mapOf(1L to 100L),
            )

        assertTrue(result.pairs.isEmpty())
        assertEquals(1L, result.stats.rejections[TatoebaRejection.MISSING_ENGLISH_TEXT])
    }

    @Test
    fun extract_keepsTheLowestIdOfIdenticalSentences() {
        val result =
            extract(
                spanish(
                    sentence(50, "Me gusta el caf\u00e9 con leche."),
                    sentence(20, "me gusta el caf\u00e9  con  leche."),
                    sentence(70, "Me gusta el caf\u00e9 con leche!"),
                ),
                "100\teng\tI like coffee with milk.\n101\teng\tI like it\n102\teng\tI like it too",
                mapOf(50L to 100L, 20L to 101L, 70L to 102L),
            )

        assertEquals(listOf(20L, 70L), result.pairs.map { it.spanishId })
        assertEquals(1L, result.stats.rejections[TatoebaRejection.DUPLICATE])
    }

    @Test
    fun extract_sortsTheOutputBySpanishId() {
        val result =
            extract(
                spanish(
                    sentence(30, "Ella come pan con leche."),
                    sentence(10, "Me gusta el caf\u00e9 con leche."),
                ),
                "1\teng\tA\n2\teng\tB",
                mapOf(30L to 1L, 10L to 2L),
            )

        assertEquals(listOf(10L, 30L), result.pairs.map { it.spanishId })
    }

    @Test
    fun extract_samplesDeterministicallyWhenThereAreMoreCandidatesThanMaxPairs() {
        // 50 distinct in-scope sentences: "me gusta X Y" for different word pairs.
        val words = wanted.toList().sorted()
        val sentences =
            (0 until 50).map { index ->
                val id = index + 1L
                sentence(id, "me gusta ${words[index % words.size]} ${words[index / words.size]}")
            }
        val spanishRows = spanish(*sentences.toTypedArray())
        val englishRows = (1L..50L).joinToString("\n") { "${1000 + it}\teng\tText $it" }
        val links = (1L..50L).associateWith { 1000 + it }

        val all = extract(spanishRows, englishRows, links, maxPairs = 100)
        val first = extract(spanishRows, englishRows, links, maxPairs = 5)
        val second = extract(spanishRows, englishRows, links, maxPairs = 5)

        assertEquals(50, all.pairs.size)
        assertEquals(5, first.pairs.size)
        assertEquals(first.pairs, second.pairs)
        assertEquals(first.pairs.map { it.spanishId }.sorted(), first.pairs.map { it.spanishId })
        assertTrue(all.pairs.containsAll(first.pairs))
        assertEquals(50, first.stats.candidates)
        assertEquals(5, first.stats.selected)
    }

    @Test
    fun extract_rejectsANonPositiveMaxPairs() {
        assertThrows(IllegalArgumentException::class.java) {
            extract("", "", emptyMap(), maxPairs = 0)
        }
    }

    // ----- files -------------------------------------------------------------------------------

    @Test
    fun writeGzipTsv_roundTripsThroughReadPairsAndIsDeterministic() {
        val pairs =
            listOf(
                TatoebaPair(10, 300, "Me gusta el caf\u00e9.", "I like coffee."),
                TatoebaPair(11, 301, "\u00bfCome pan?", "Does he eat bread?"),
            )
        val first = ByteArrayOutputStream().also { TatoebaCandidateFiles.writeGzipTsv(pairs, it) }
        val second = ByteArrayOutputStream().also { TatoebaCandidateFiles.writeGzipTsv(pairs, it) }

        val text = GZIPInputStream(first.toByteArray().inputStream()).readBytes()

        assertTrue(first.toByteArray().contentEquals(second.toByteArray()))
        assertEquals(pairs, TatoebaCandidateFiles.readPairs(text.inputStream()))
        assertTrue(text.toString(Charsets.UTF_8).startsWith("10\t300\tMe gusta"))
    }

    @Test
    fun readPairs_skipsLinesWithTheWrongShape() {
        val text = "1\t2\tuno\tone\nbad\n3\t4\tdos\n5\tx\ttres\tthree\n6\t7\tcuatro\tfour\n"

        val pairs = TatoebaCandidateFiles.readPairs(text.byteInputStream())

        assertEquals(listOf(1L, 6L), pairs.map { it.spanishId })
    }

    @Test
    fun metadataJson_recordsProvenanceAndEveryRejectionCounter() {
        val stats = TatoebaStats(10, mapOf(TatoebaRejection.DUPLICATE to 2L), 5, 3)

        val json =
            TatoebaCandidateFiles.metadataJson(stats, 25_000, 20_000, mapOf("source" to "spa.tsv"))

        assertTrue(json.contains("\"format\": \"tatoeba-candidates/1\""))
        assertTrue(json.contains("\"source\": \"spa.tsv\""))
        assertTrue(json.contains("\"rejected_duplicate\": 2"))
        assertTrue(json.contains("\"rejected_too_long\": 0"))
        assertTrue(json.contains("\"selected\": 3"))
    }

    @Test
    fun extractToFiles_readsPlainAndGzipInputsAndWritesPairsAndMetadata() {
        val dir = createTempDirectory("tatoeba-extract").toFile()
        val spanishFile =
            File(dir, "spa_sentences.tsv").apply {
                writeText("10\tspa\tMe gusta el caf\u00e9 con leche.\n")
            }
        val englishFile = File(dir, "eng_sentences.tsv.gz")
        englishFile.outputStream().use {
            DeterministicGzip.compress("300\teng\tI like coffee with milk.\n".toByteArray(), it)
        }
        val linksFile = File(dir, "links.tsv").apply { writeText("10\t300\n") }
        val output = File(dir, "out/pairs.tsv.gz")
        val metadata = File(dir, "out/pairs.meta.json")

        val stats =
            TatoebaCandidateFiles.extractToFiles(
                TatoebaRun(
                    inputs = TatoebaInputs(spanishFile, englishFile, linksFile),
                    wantedWords = wanted,
                    maxPairs = 100,
                    output = output,
                    metadataFile = metadata,
                    sourceInfo = mapOf("source" to spanishFile.name),
                )
            )

        assertEquals(1, stats.selected)
        val written = GZIPInputStream(output.inputStream()).readBytes().toString(Charsets.UTF_8)
        assertEquals(
            "10\t300\tMe gusta el caf\u00e9 con leche.\tI like coffee with milk.\n",
            written
        )
        assertTrue(metadata.readText().contains("\"selected\": 1"))
        dir.deleteRecursively()
    }
}
