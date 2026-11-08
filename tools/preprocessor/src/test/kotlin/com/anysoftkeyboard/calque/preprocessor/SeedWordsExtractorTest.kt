package com.anysoftkeyboard.calque.preprocessor

import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SeedWordsExtractorTest {

    private val lessons =
        """[{"lesson_id":"a","content":{"sentences":[{"sentence_es":"¿Necesita el equipo más información?"},""" +
            """{"sentence_es":"El hotel es Excelente."}]}}]"""

    private val falseCognates =
        """[{"lemma":"Constipado","real_spanish_for_apparent":"estreñido","example_es":"Estoy constipado hoy."}]"""

    private val dictionary =
        """[{"surface_form":"hablaba","lemma":"hablar"},{"surface_form":"niño","lemma":"niño"}]"""

    private fun collect(
        lessons: String = this.lessons,
        falseCognates: String = this.falseCognates,
        dictionary: String = this.dictionary,
    ) =
        SeedWordsExtractor.collect(
            lessons.byteInputStream(),
            falseCognates.byteInputStream(),
            dictionary.byteInputStream(),
        )

    @Test
    fun collect_returnsEveryWordOfAllThreeSeedsSortedAndLowercased() {
        val words = collect()

        assertEquals(
            listOf(
                "constipado",
                "el",
                "equipo",
                "es",
                "estoy",
                "estreñido",
                "excelente",
                "hablaba",
                "hablar",
                "hotel",
                "hoy",
                "información",
                "más",
                "necesita",
                "niño",
            ),
            words,
        )
    }

    @Test
    fun collect_dropsPunctuationAndInvertedMarks() {
        val words =
            collect(lessons = """[{"content":{"sentences":[{"sentence_es":"¡Hola, amigo!"}]}}]""")

        assertEquals(true, "hola" in words && "amigo" in words)
        assertEquals(emptyList<String>(), words.filter { it.any { c -> !c.isLetter() } })
    }

    @Test
    fun collect_listsEachWordOnce() {
        val words = collect(dictionary = """[{"surface_form":"hotel","lemma":"hotel"}]""")

        assertEquals(1, words.count { it == "hotel" })
    }

    @Test
    fun collect_toleratesEntriesWithMissingFields() {
        val words =
            collect(
                lessons = """[{"lesson_id":"x"},{"content":{}},{"content":{"sentences":[{}]}}]""",
                falseCognates = """[{"lemma":"sopa"},{}]""",
                dictionary = """[{}]""",
            )

        assertEquals(listOf("sopa"), words)
    }

    @Test
    fun collect_rejectsInputThatIsNotAJsonArray() {
        assertThrows(IllegalArgumentException::class.java) { collect(lessons = """{"a":1}""") }
    }

    @Test
    fun write_putsOneWordPerLineUnderACommentThatTheWordListReaderIgnores() {
        val out = ByteArrayOutputStream()

        SeedWordsExtractor.write(listOf("casa", "niño"), out)
        val text = out.toString(Charsets.UTF_8)

        assertEquals(true, text.startsWith("# "))
        assertEquals(setOf("casa", "niño"), KaikkiSubsetFiles.readWordList(text.byteInputStream()))
    }
}
