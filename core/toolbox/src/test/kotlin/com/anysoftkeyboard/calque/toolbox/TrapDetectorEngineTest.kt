package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrapDetectorEngineTest {

    private val sampleTraps =
        listOf(
            FalseCognateEntry(
                surfaceForm = "éxito",
                lemma = "éxito",
                apparentEnglish = "exit",
                actualEnglish = "success",
                realSpanishForApparent = "salida",
                severity = "HIGH_RISK",
                disambiguationTip = "Éxito means success. For an exit door, look for 'salida'.",
                exampleEs = "La película fue un gran éxito.",
                exampleEn = "The movie was a great success.",
            ),
            FalseCognateEntry(
                surfaceForm = "carpeta",
                lemma = "carpeta",
                apparentEnglish = "carpet",
                actualEnglish = "folder / binder",
                realSpanishForApparent = "alfombra",
                severity = "HIGH_RISK",
                disambiguationTip =
                    "Carpeta is a file folder or binder. For floor carpet, use 'alfombra'.",
                exampleEs = "Pon los documentos en la carpeta azul.",
                exampleEn = "Put the documents in the blue folder.",
            ),
            FalseCognateEntry(
                surfaceForm = "sensible",
                lemma = "sensible",
                apparentEnglish = "sensible",
                actualEnglish = "sensitive",
                realSpanishForApparent = "sensato / razonable",
                severity = "MILD_NUANCE",
                disambiguationTip =
                    "Sensible means sensitive. For someone sensible/prudent, use 'sensato'.",
                exampleEs = "Ella tiene la piel muy sensible.",
                exampleEn = "She has very sensitive skin.",
            ),
        )

    @Test
    fun filterTraps_allReturnsEverything() {
        val result = TrapDetectorEngine.filterTraps(sampleTraps, TrapFilter.ALL)
        assertEquals(3, result.size)
    }

    @Test
    fun filterTraps_highRiskFiltersCorrectly() {
        val result = TrapDetectorEngine.filterTraps(sampleTraps, TrapFilter.HIGH_RISK)
        assertEquals(2, result.size)
        assertTrue(result.all { it.severity == "HIGH_RISK" })
    }

    @Test
    fun filterTraps_mildNuanceFiltersCorrectly() {
        val result = TrapDetectorEngine.filterTraps(sampleTraps, TrapFilter.MILD_NUANCE)
        assertEquals(1, result.size)
        assertEquals("sensible", result.first().lemma)
    }

    @Test
    fun filterTraps_seenInReadingMatchesSeenLemmas() {
        val seen = setOf("carpeta")
        val result = TrapDetectorEngine.filterTraps(sampleTraps, TrapFilter.SEEN_IN_READING, seen)
        assertEquals(1, result.size)
        assertEquals("carpeta", result.first().lemma)
    }

    @Test
    fun createQuizQuestion_generatesValidQuestion() {
        val trap = sampleTraps.first() // éxito
        val question = TrapDetectorEngine.createQuizQuestion(trap, correctFirst = true)

        assertEquals("La película fue un gran éxito.", question.sentenceEs)
        assertEquals("éxito", question.targetWord)
        assertEquals(0, question.correctOptionIndex)
        assertEquals("success", question.options[0])
        assertEquals("exit", question.options[1])
        assertTrue(question.explanation.contains("salida"))
    }

    @Test
    fun generateQuiz_alternatesOptionOrder() {
        val quiz = TrapDetectorEngine.generateQuiz(sampleTraps, alternateAnswers = true)
        assertEquals(3, quiz.size)
        assertEquals(0, quiz[0].correctOptionIndex)
        assertEquals(1, quiz[1].correctOptionIndex)
        assertEquals(0, quiz[2].correctOptionIndex)
    }
}
