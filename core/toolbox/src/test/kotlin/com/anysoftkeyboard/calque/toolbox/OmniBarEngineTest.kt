package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OmniBarEngineTest {

    private lateinit var omniBarEngine: OmniBarEngine

    @Before
    fun setUp() {
        omniBarEngine =
            OmniBarEngine(
                cognateRulesRepository = createTestRulesRepo(),
                falseCognatesRepository = createTestFalseCognatesRepo(),
                wordLookupProvider = createTestLookupProvider(),
            )
    }

    @Test
    fun route_digitsMatchesNumber() {
        val result = omniBarEngine.route("492")
        assertTrue(result is OmniBarResult.NumberResult)
        val numResult = result as OmniBarResult.NumberResult
        assertEquals(492L, numResult.number)
        assertEquals("cuatrocientos noventa y dos", numResult.masculineWords)
    }

    @Test
    fun route_clockTimeMatchesTime() {
        val result = omniBarEngine.route("14:30")
        assertTrue(result is OmniBarResult.NumberResult)
        val numResult = result as OmniBarResult.NumberResult
        assertEquals("Son las dos y media de la tarde", numResult.masculineWords)
    }

    @Test
    fun route_suffixMatchesCognateRule() {
        val result1 = omniBarEngine.route("-tion")
        assertTrue(result1 is OmniBarResult.SuffixRuleResult)
        assertEquals("rule_tion_cion", (result1 as OmniBarResult.SuffixRuleResult).rule.ruleId)

        val result2 = omniBarEngine.route("dad")
        assertTrue(result2 is OmniBarResult.SuffixRuleResult)
        assertEquals("rule_ty_dad", (result2 as OmniBarResult.SuffixRuleResult).rule.ruleId)
    }

    @Test
    fun route_falseCognateMatchesTrap() {
        val result = omniBarEngine.route("carpeta")
        assertTrue(result is OmniBarResult.FalseCognateResult)
        val trapResult = result as OmniBarResult.FalseCognateResult
        assertEquals("folder / binder", trapResult.card.actualEnglish)
    }

    @Test
    fun route_cliticMatchesSurgery() {
        val result = omniBarEngine.route("explicárselo")
        assertTrue(result is OmniBarResult.CliticResult)
        val cliticResult = result as OmniBarResult.CliticResult
        assertEquals("explicar", cliticResult.surgery.rootVerb)
        assertEquals("se lo va a explicar", cliticResult.surgery.procliticEquivalent)
    }

    @Test
    fun route_dictionaryWordMatchesLookup() {
        val result = omniBarEngine.route("libro")
        assertTrue(result is OmniBarResult.WordLookupResult)
        val wordResult = result as OmniBarResult.WordLookupResult
        assertEquals("book", wordResult.word.englishGloss)
    }

    @Test
    fun route_emptyOrUnknownMatchesNone() {
        assertTrue(omniBarEngine.route("") is OmniBarResult.None)
        assertTrue(omniBarEngine.route("xyzrandomword") is OmniBarResult.None)
    }

    private fun createTestRulesRepo(): CognateRulesRepository =
        object : CognateRulesRepository {
            private val rules =
                listOf(
                    CognateRule(
                        ruleId = "rule_tion_cion",
                        englishSuffix = "-tion",
                        spanishSuffix = "-ción",
                        genderRule = "Always feminine (la)",
                        spellingNote = "Convert -tion to -ción.",
                        examplesJson = """[{"english":"action","spanish":"acción"}]""",
                    ),
                    CognateRule(
                        ruleId = "rule_ty_dad",
                        englishSuffix = "-ty",
                        spanishSuffix = "-dad",
                        genderRule = "Always feminine (la)",
                        spellingNote = "Convert -ty to -dad.",
                        examplesJson = """[{"english":"community","spanish":"comunidad"}]""",
                    ),
                )

            override fun getAll(): List<CognateRule> = rules

            override fun getById(ruleId: String): CognateRule? =
                rules.firstOrNull { it.ruleId == ruleId }

            override fun findRuleForSpanishWord(spanishWord: String): CognateRule? =
                rules.firstOrNull { spanishWord.endsWith(it.spanishSuffix.removePrefix("-")) }
        }

    private fun createTestFalseCognatesRepo(): FalseCognatesRepository =
        object : FalseCognatesRepository {
            private val traps =
                mapOf(
                    "carpeta" to
                        FalseCognateEntry(
                            surfaceForm = "carpeta",
                            lemma = "carpeta",
                            apparentEnglish = "carpet",
                            actualEnglish = "folder / binder",
                            realSpanishForApparent = "alfombra",
                            severity = "HIGH_RISK",
                            disambiguationTip = "Carpeta is a file folder.",
                            exampleEs = "Pon el papel en la carpeta.",
                            exampleEn = "Put the paper in the folder.",
                        ),
                )

            override fun getBySurfaceForm(surfaceForm: String): FalseCognateEntry? =
                traps[surfaceForm]

            override fun getBySurfaceForms(
                surfaceForms: Collection<String>
            ): Map<String, FalseCognateEntry> =
                surfaceForms.mapNotNull { f -> traps[f]?.let { f to it } }.toMap()

            override fun getByLemma(lemma: String): List<FalseCognateEntry> =
                traps.values.filter { it.lemma == lemma }

            override fun getAll(): List<FalseCognateEntry> = traps.values.toList()
        }

    private fun createTestLookupProvider(): WordLookupProvider =
        object : WordLookupProvider {
            private val dictionary =
                mapOf(
                    "explicar" to
                        DictionaryEntry(
                            surfaceForm = "explicar",
                            lemma = "explicar",
                            pos = "verb",
                            grammarInfo = "inf",
                            englishGloss = "to explain",
                            isCognate = true,
                            frequencyRank = 450,
                        ),
                    "libro" to
                        DictionaryEntry(
                            surfaceForm = "libro",
                            lemma = "libro",
                            pos = "noun",
                            grammarInfo = "masc",
                            englishGloss = "book",
                            isCognate = false,
                            frequencyRank = 120,
                        ),
                )

            override fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry> =
                words.mapNotNull { w -> dictionary[w]?.let { w to it } }.toMap()

            override fun batchLookupFalseCognates(
                words: Set<String>
            ): Map<String, FalseCognateEntry> = emptyMap()

            override fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus> =
                emptyMap()

            override fun lookupSingleWord(word: String): DictionaryEntry? = dictionary[word]
        }
}
