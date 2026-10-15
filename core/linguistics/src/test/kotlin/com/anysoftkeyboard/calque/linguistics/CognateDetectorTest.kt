package com.anysoftkeyboard.calque.linguistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CognateDetectorTest {

    @Test
    fun testExactCognates() {
        assertTrue(CognateDetector.detectCognate("natural", "natural"))
        assertTrue(CognateDetector.detectCognate("animal", "animal"))
        assertTrue(CognateDetector.detectCognate("hotel", "hotel"))
        assertTrue(CognateDetector.detectCognate("radio", "radio"))
        assertTrue(CognateDetector.detectCognate("color", "color"))
        assertTrue(CognateDetector.detectCognate("drama", "drama"))
        assertTrue(CognateDetector.detectCognate("error", "error"))
    }

    @Test
    fun testAccentNormalizedExactMatches() {
        assertTrue(CognateDetector.detectCognate("café", "cafe"))
        assertTrue(CognateDetector.detectCognate("menú", "menu"))
    }

    @Test
    fun testCionToTion() {
        assertTrue(CognateDetector.detectCognate("comunicación", "communication"))
        assertTrue(CognateDetector.detectCognate("información", "information"))
        assertTrue(CognateDetector.detectCognate("nación", "nation"))
        assertTrue(CognateDetector.detectCognate("estación", "station"))
        assertTrue(CognateDetector.detectCognate("educación", "education"))
    }

    @Test
    fun testSionToSion() {
        assertTrue(CognateDetector.detectCognate("decisión", "decision"))
        assertTrue(CognateDetector.detectCognate("televisión", "television"))
        assertTrue(CognateDetector.detectCognate("conclusión", "conclusion"))
        assertTrue(CognateDetector.detectCognate("profesión", "profession"))
    }

    @Test
    fun testDadToTy() {
        assertTrue(CognateDetector.detectCognate("actividad", "activity"))
        assertTrue(CognateDetector.detectCognate("universidad", "university"))
        assertTrue(CognateDetector.detectCognate("ciudad", "city"))
        assertTrue(CognateDetector.detectCognate("comunidad", "community"))
        assertTrue(CognateDetector.detectCognate("sociedad", "society"))
    }

    @Test
    fun testMenteToLy() {
        assertTrue(CognateDetector.detectCognate("naturalmente", "naturally"))
        assertTrue(CognateDetector.detectCognate("rápidamente", "rapidly"))
        assertTrue(CognateDetector.detectCognate("totalmente", "totally"))
    }

    @Test
    fun testOsoToOus() {
        assertTrue(CognateDetector.detectCognate("curioso", "curious"))
        assertTrue(CognateDetector.detectCognate("famosa", "famous"))
        assertTrue(CognateDetector.detectCognate("religioso", "religious"))
        assertTrue(CognateDetector.detectCognate("nervioso", "nervous"))
    }

    @Test
    fun testIcoToIc() {
        assertTrue(CognateDetector.detectCognate("básico", "basic"))
        assertTrue(CognateDetector.detectCognate("romántico", "romantic"))
        assertTrue(CognateDetector.detectCognate("música", "music"))
        assertTrue(CognateDetector.detectCognate("dramático", "dramatic"))
    }

    @Test
    fun testAlToAl() {
        assertTrue(CognateDetector.detectCognate("oficial", "official"))
        assertTrue(CognateDetector.detectCognate("personal", "personal"))
        assertTrue(CognateDetector.detectCognate("central", "central"))
    }

    @Test
    fun testCtoToCt() {
        assertTrue(CognateDetector.detectCognate("perfecto", "perfect"))
        assertTrue(CognateDetector.detectCognate("exacto", "exact"))
        assertTrue(CognateDetector.detectCognate("directa", "direct"))
        assertTrue(CognateDetector.detectCognate("contacto", "contact"))
    }

    @Test
    fun testNciaToNce() {
        assertTrue(CognateDetector.detectCognate("importancia", "importance"))
        assertTrue(CognateDetector.detectCognate("diferencia", "difference"))
        assertTrue(CognateDetector.detectCognate("distancia", "distance"))
        assertTrue(CognateDetector.detectCognate("inteligencia", "intelligence"))
    }

    @Test
    fun testBleToBle() {
        assertTrue(CognateDetector.detectCognate("posible", "possible"))
        assertTrue(CognateDetector.detectCognate("terrible", "terrible"))
    }

    @Test
    fun testArioToAry() {
        assertTrue(CognateDetector.detectCognate("ordinario", "ordinary"))
        assertTrue(CognateDetector.detectCognate("necesaria", "necessary"))
    }

    @Test
    fun testIvoToIve() {
        assertTrue(CognateDetector.detectCognate("activo", "active"))
        assertTrue(CognateDetector.detectCognate("creativa", "creative"))
    }

    @Test
    fun testOrToOr() {
        assertTrue(CognateDetector.detectCognate("actor", "actor"))
        assertTrue(CognateDetector.detectCognate("profesor", "professor"))
    }

    @Test
    fun testVerbCognates() {
        assertTrue(CognateDetector.detectCognate("comunicar", "to communicate"))
        assertTrue(CognateDetector.detectCognate("participar", "to participate"))
        assertTrue(CognateDetector.detectCognate("separar", "to separate"))
        assertTrue(CognateDetector.detectCognate("decidir", "to decide"))
        assertTrue(CognateDetector.detectCognate("preparar", "to prepare"))
        assertTrue(CognateDetector.detectCognate("organizar", "to organize"))
    }

    @Test
    fun testComplexEnglishGlossExtraction() {
        // Leading infinitive particle with verb
        assertTrue(CognateDetector.detectCognate("comunicar", "to communicate"))
        // Leading article
        assertTrue(CognateDetector.detectCognate("doctor", "a doctor"))
        // Parenthetical qualifier
        assertTrue(CognateDetector.detectCognate("natural", "(anatomy, biology) natural"))
        // Comma-separated synonyms where cognate is later
        assertTrue(CognateDetector.detectCognate("rápido", "quick, rapid"))
        // Semicolon-separated definitions
        assertTrue(CognateDetector.detectCognate("exacto", "precise; exact"))
    }

    @Test
    fun testNonCognates() {
        assertFalse(CognateDetector.detectCognate("perro", "dog"))
        assertFalse(CognateDetector.detectCognate("gato", "cat"))
        assertFalse(CognateDetector.detectCognate("mesa", "table"))
        assertFalse(CognateDetector.detectCognate("agua", "water"))
        assertFalse(CognateDetector.detectCognate("hacer", "to do"))
        assertFalse(CognateDetector.detectCognate("comer", "to eat"))
        assertFalse(CognateDetector.detectCognate("ir", "to go"))
        assertFalse(CognateDetector.detectCognate("libro", "book"))
        assertFalse(CognateDetector.detectCognate("mano", "hand"))
        assertFalse(CognateDetector.detectCognate("casa", "house"))
    }

    @Test
    fun testEdgeCases() {
        assertFalse(CognateDetector.detectCognate("", ""))
        assertFalse(CognateDetector.detectCognate("   ", "   "))
        assertFalse(CognateDetector.detectCognate("a", "to"))
        assertFalse(CognateDetector.detectCognate("de", "of"))
        assertFalse(CognateDetector.detectCognate("el", "the"))
        assertFalse(CognateDetector.detectCognate("la", "the"))
    }

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, CognateDetector.levenshteinDistance("", ""))
        assertEquals(3, CognateDetector.levenshteinDistance("abc", ""))
        assertEquals(3, CognateDetector.levenshteinDistance("", "abc"))
        assertEquals(0, CognateDetector.levenshteinDistance("test", "test"))
        assertEquals(1, CognateDetector.levenshteinDistance("ofici", "offici"))
        assertEquals(3, CognateDetector.levenshteinDistance("kitten", "sitting"))
    }

    @Test
    fun testStripAccents() {
        assertEquals("comunicacion", CognateDetector.stripAccents("comunicación"))
        assertEquals("facilmente", CognateDetector.stripAccents("fácilmente"))
        assertEquals("rapido", CognateDetector.stripAccents("rápido"))
        assertEquals("Espana", CognateDetector.stripAccents("España"))
    }
}
