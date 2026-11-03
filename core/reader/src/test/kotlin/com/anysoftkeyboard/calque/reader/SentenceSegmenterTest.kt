package com.anysoftkeyboard.calque.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceSegmenterTest {

    @Test
    fun segment_emptyOrBlankTextReturnsEmpty() {
        assertTrue(SentenceSegmenter.segment("").isEmpty())
        assertTrue(SentenceSegmenter.segment("   \n\t  ").isEmpty())
    }

    @Test
    fun segment_simpleSentences() {
        val text = "El gato come pescado. La mujer lee un libro. El perro corre."
        val sentences = SentenceSegmenter.segment(text)

        assertEquals(3, sentences.size)
        assertEquals("El gato come pescado.", sentences[0])
        assertEquals("La mujer lee un libro.", sentences[1])
        assertEquals("El perro corre.", sentences[2])
    }

    @Test
    fun segment_spanishInvertedQuestionsAndExclamations() {
        val text = "¡Hola! ¿Cómo estás tú? Yo estoy muy bien, gracias."
        val sentences = SentenceSegmenter.segment(text)

        assertEquals(3, sentences.size)
        assertEquals("¡Hola!", sentences[0])
        assertEquals("¿Cómo estás tú?", sentences[1])
        assertEquals("Yo estoy muy bien, gracias.", sentences[2])
    }

    @Test
    fun segment_abbreviationsDoNotCausePrematureSplit() {
        val text = "El Dr. Gómez llegó a las tres. El Sr. López habló con él."
        val sentences = SentenceSegmenter.segment(text)

        assertEquals(2, sentences.size)
        assertEquals("El Dr. Gómez llegó a las tres.", sentences[0])
        assertEquals("El Sr. López habló con él.", sentences[1])
    }

    @Test
    fun segment_multilineParagraphs() {
        val text =
            """
            Había una vez en una ciudad lejana.
            Un viajero caminaba sin prisa. ¿Quién sabe su destino?
            """
                .trimIndent()
        val sentences = SentenceSegmenter.segment(text)

        assertEquals(3, sentences.size)
        assertEquals("Había una vez en una ciudad lejana.", sentences[0])
        assertEquals("Un viajero caminaba sin prisa.", sentences[1])
        assertEquals("¿Quién sabe su destino?", sentences[2])
    }
}
