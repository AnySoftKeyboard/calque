package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.data.models.CognateRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WordTransformerEngineTest {

    private val sampleRules =
        listOf(
            CognateRule(
                ruleId = "rule_tion_cion",
                englishSuffix = "-tion",
                spanishSuffix = "-ción",
                genderRule = "Always feminine (la)",
                spellingNote =
                    "Convert -tion to -ción. Double consonants simplify: attention -> atención.",
                examplesJson = """[{"english":"action","spanish":"acción"}]""",
            ),
            CognateRule(
                ruleId = "rule_ty_dad",
                englishSuffix = "-ty",
                spanishSuffix = "-dad",
                genderRule = "Always feminine (la)",
                spellingNote = "Convert -ty to -dad. Difficult consonants simplify.",
                examplesJson = """[{"english":"community","spanish":"comunidad"}]""",
            ),
            CognateRule(
                ruleId = "rule_ous_oso",
                englishSuffix = "-ous",
                spanishSuffix = "-oso",
                genderRule = "Masc: -oso, Fem: -osa",
                spellingNote = "Convert -ous to -oso/-osa.",
                examplesJson = """[{"english":"famous","spanish":"famoso"}]""",
            ),
            CognateRule(
                ruleId = "rule_ly_mente",
                englishSuffix = "-ly",
                spanishSuffix = "-mente",
                genderRule = null,
                spellingNote = "Convert -ly to -mente.",
                examplesJson = """[{"english":"rapidly","spanish":"rápidamente"}]""",
            ),
        )

    @Test
    fun transformEnglishToSpanish_tionToCion() {
        val result = WordTransformerEngine.transformEnglishToSpanish("situation", sampleRules)
        assertNotNull(result)
        assertEquals("situación", result!!.resultWord)
        assertEquals("Always feminine (la)", result.genderRule)
    }

    @Test
    fun transformEnglishToSpanish_simplifiesDoubleConsonants() {
        val result = WordTransformerEngine.transformEnglishToSpanish("attention", sampleRules)
        assertNotNull(result)
        assertEquals("atención", result!!.resultWord)
    }

    @Test
    fun transformEnglishToSpanish_tyToDad() {
        val result = WordTransformerEngine.transformEnglishToSpanish("community", sampleRules)
        assertNotNull(result)
        assertEquals("comunidad", result!!.resultWord)
    }

    @Test
    fun transformEnglishToSpanish_ousToOso() {
        val result = WordTransformerEngine.transformEnglishToSpanish("famous", sampleRules)
        assertNotNull(result)
        assertEquals("famoso", result!!.resultWord)
    }

    @Test
    fun transformSpanishToEnglish_cionToTion() {
        val result = WordTransformerEngine.transformSpanishToEnglish("comunicación", sampleRules)
        assertNotNull(result)
        assertEquals("comunication", result!!.resultWord)
    }

    @Test
    fun transformSpanishToEnglish_dadToTy() {
        val result = WordTransformerEngine.transformSpanishToEnglish("universidad", sampleRules)
        assertNotNull(result)
        assertEquals("university", result!!.resultWord)
    }

    @Test
    fun transformEnglishToSpanish_unknownSuffixReturnsNull() {
        val result = WordTransformerEngine.transformEnglishToSpanish("running", sampleRules)
        assertNull(result)
    }
}
