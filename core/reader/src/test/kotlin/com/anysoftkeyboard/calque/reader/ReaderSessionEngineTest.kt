package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.data.models.UserCustomText
import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReaderSessionEngineTest {

    private lateinit var fakeLookupProvider: WordLookupProvider
    private lateinit var fakeRulesRepo: CognateRulesRepository
    private lateinit var fakeUserStateRepo: FakeUserWordStateRepository

    @Before
    fun setUp() {
        fakeLookupProvider = createFakeLookupProvider()
        fakeRulesRepo = createFakeRulesRepo()
        fakeUserStateRepo = FakeUserWordStateRepository()
    }

    @Test
    fun curatedLesson_initializesSentencesWithCuratedMetadata() {
        val json =
            """
            [
              {
                "sentence_es": "El gato come pescado.",
                "translation_en": "The cat eats fish.",
                "interlinear_gloss": [
                  {"es": "El", "en": "The"},
                  {"es": " ", "en": ""},
                  {"es": "gato", "en": "cat"},
                  {"es": " ", "en": ""},
                  {"es": "come", "en": "eats"},
                  {"es": " ", "en": ""},
                  {"es": "pescado", "en": "fish"},
                  {"es": ".", "en": ""}
                ],
                "grammar_callout": "Present tense 3rd person singular"
              },
              {
                "sentence_es": "La casa es grande.",
                "translation_en": "The house is big."
              }
            ]
            """
                .trimIndent()

        val lesson =
            CuratedLesson(
                lessonId = "lesson_01",
                level = 1,
                sequenceOrder = 1,
                title = "El gato y la casa",
                targetConcept = "Subject-Verb-Object",
                contentJson = json,
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.Curated(lesson),
                lookupProvider = fakeLookupProvider,
                cognateRulesRepository = fakeRulesRepo,
                userWordStateRepository = fakeUserStateRepo,
            )

        assertEquals("El gato y la casa", engine.state.title)
        assertEquals(2, engine.state.sentences.size)
        assertEquals(0, engine.state.currentSentenceIndex)
        assertFalse(engine.state.isCompleted)

        val firstSentence = engine.state.currentSentence!!
        assertEquals("The cat eats fish.", firstSentence.translationEn)
        assertEquals("Present tense 3rd person singular", firstSentence.grammarCallout)
        assertEquals(8, firstSentence.interlinearGlosses.size)
    }

    @Test
    fun customText_segmentsAndGeneratesGloss() {
        val customText =
            UserCustomText(
                textId = 1L,
                title = "Mi Viaje",
                originalSpanish = "El gato come pescado. La casa es grande.",
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.CustomText(customText),
                lookupProvider = fakeLookupProvider,
                cognateRulesRepository = fakeRulesRepo,
                userWordStateRepository = fakeUserStateRepo,
            )

        assertEquals("Mi Viaje", engine.state.title)
        assertEquals(2, engine.state.sentences.size)
        assertEquals("El gato come pescado.", engine.state.sentences[0].textEs)
        assertNull(engine.state.sentences[0].translationEn)
        assertTrue(engine.state.sentences[0].interlinearGlosses.isNotEmpty())
    }

    @Test
    fun navigation_nextPreviousAndJump() {
        val customText =
            UserCustomText(
                title = "Test",
                originalSpanish = "Primera oración. Segunda oración. Tercera oración.",
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.CustomText(customText),
                lookupProvider = fakeLookupProvider,
            )

        assertEquals(0, engine.state.currentSentenceIndex)

        engine.nextSentence()
        assertEquals(1, engine.state.currentSentenceIndex)

        engine.nextSentence()
        assertEquals(2, engine.state.currentSentenceIndex)
        assertFalse(engine.state.isCompleted)

        engine.nextSentence() // Past last sentence
        assertTrue(engine.state.isCompleted)

        engine.previousSentence()
        assertEquals(1, engine.state.currentSentenceIndex)

        engine.jumpToSentence(0)
        assertEquals(0, engine.state.currentSentenceIndex)
    }

    @Test
    fun toggles_modeInterlinearAndTranslation() {
        val lesson =
            CuratedLesson(
                lessonId = "l1",
                level = 1,
                sequenceOrder = 1,
                title = "Title",
                targetConcept = "Concept",
                contentJson = """[{"sentence_es":"Hola.","translation_en":"Hello."}]""",
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.Curated(lesson),
                lookupProvider = fakeLookupProvider,
            )

        assertFalse(engine.state.showInterlinear)
        engine.toggleInterlinear()
        assertTrue(engine.state.showInterlinear)

        assertFalse(engine.state.showTranslation)
        engine.toggleTranslation()
        assertTrue(engine.state.showTranslation)

        assertEquals(ReaderMode.SENTENCE_FOCUS, engine.state.readerMode)
        engine.setReaderMode(ReaderMode.PARAGRAPH)
        assertEquals(ReaderMode.PARAGRAPH, engine.state.readerMode)
    }

    @Test
    fun selectToken_inspectsAndRecordsEncounter() {
        val customText =
            UserCustomText(
                title = "Story",
                originalSpanish = "El gato duerme.",
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.CustomText(customText),
                lookupProvider = fakeLookupProvider,
                cognateRulesRepository = fakeRulesRepo,
                userWordStateRepository = fakeUserStateRepo,
            )

        // Select "gato" (token index 2: "El" [0], " " [1], "gato" [2])
        engine.selectToken(2)
        assertNotNull(engine.state.activeInspection)
        val inspection = engine.state.activeInspection!!
        assertEquals("gato", inspection.lemma)
        assertEquals("cat", inspection.englishGloss)

        // Verify encounter recorded
        val wordState = fakeUserStateRepo.getWordState("gato")
        assertNotNull(wordState)
        assertEquals(1, wordState!!.encounterCount)

        // Deselect by clicking again
        engine.selectToken(2)
        assertNull(engine.state.activeInspection)
    }

    @Test
    fun vaultActions_updateStatusAndToggleStarred() {
        val customText =
            UserCustomText(
                title = "Story",
                originalSpanish = "El gato duerme.",
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.CustomText(customText),
                lookupProvider = fakeLookupProvider,
                userWordStateRepository = fakeUserStateRepo,
            )

        engine.selectToken(2) // "gato"
        assertEquals(UserWordStatus.NEW, engine.state.activeInspection?.userStatus)
        assertFalse(engine.state.activeInspection?.isStarred ?: true)

        engine.updateWordStatus("gato", UserWordStatus.MASTERED)
        assertEquals(UserWordStatus.MASTERED, engine.state.activeInspection?.userStatus)
        assertEquals(UserWordStatus.MASTERED, fakeUserStateRepo.getWordState("gato")?.status)

        engine.toggleStarred("gato")
        assertTrue(engine.state.activeInspection?.isStarred ?: false)
        assertTrue(fakeUserStateRepo.getWordState("gato")?.starred ?: false)
    }

    @Test
    fun getSessionSummary_computesAccurateMetrics() {
        val customText =
            UserCustomText(
                title = "Story",
                originalSpanish = "El gato come pescado.",
            )

        val engine =
            ReaderSessionEngine(
                source = ReadingSource.CustomText(customText),
                lookupProvider = fakeLookupProvider,
            )

        val summary = engine.getSessionSummary()
        assertEquals(1, summary.totalSentences)
        assertEquals(4, summary.uniqueWords) // el, gato, come, pescado
        assertFalse(summary.isCompleted)
    }

    private fun createFakeLookupProvider(): WordLookupProvider =
        object : WordLookupProvider {
            private val dict =
                mapOf(
                    "el" to DictionaryEntry("el", "el", "det", "masc", "the", false, 1),
                    "gato" to DictionaryEntry("gato", "gato", "noun", "masc", "cat", false, 200),
                    "come" to DictionaryEntry("come", "comer", "verb", "pres", "to eat", false, 50),
                    "pescado" to
                        DictionaryEntry("pescado", "pescado", "noun", "masc", "fish", false, 300),
                    "casa" to DictionaryEntry("casa", "casa", "noun", "fem", "house", false, 80),
                    "grande" to DictionaryEntry("grande", "grande", "adj", null, "big", true, 90),
                )

            override fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry> =
                words.mapNotNull { w -> dict[w]?.let { w to it } }.toMap()

            override fun batchLookupFalseCognates(
                words: Set<String>
            ): Map<String, FalseCognateEntry> = emptyMap()

            override fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus> =
                emptyMap()

            override fun lookupSingleWord(word: String): DictionaryEntry? = dict[word]
        }

    private fun createFakeRulesRepo(): CognateRulesRepository =
        object : CognateRulesRepository {
            override fun getAll(): List<CognateRule> = emptyList()

            override fun getById(ruleId: String): CognateRule? = null

            override fun findRuleForSpanishWord(spanishWord: String): CognateRule? = null
        }

    private class FakeUserWordStateRepository : UserWordStateRepository {
        private val vault = mutableMapOf<String, UserWordState>()

        override fun getWordState(lemma: String): UserWordState? = vault[lemma]

        override fun batchGetWordStates(lemmas: Collection<String>): Map<String, UserWordState> =
            lemmas.mapNotNull { l -> vault[l]?.let { l to it } }.toMap()

        override fun upsertWordState(state: UserWordState) {
            vault[state.lemma] = state
        }

        override fun recordEncounter(lemma: String, timestamp: Long) {
            val existing = vault[lemma]
            val count = (existing?.encounterCount ?: 0) + 1
            vault[lemma] =
                existing?.copy(encounterCount = count, lastSeenTimestamp = timestamp)
                    ?: UserWordState(
                        lemma = lemma,
                        encounterCount = 1,
                        lastSeenTimestamp = timestamp
                    )
        }

        override fun setWordStatus(lemma: String, status: UserWordStatus, timestamp: Long) {
            val existing = vault[lemma]
            vault[lemma] =
                existing?.copy(status = status, lastSeenTimestamp = timestamp)
                    ?: UserWordState(lemma = lemma, status = status, lastSeenTimestamp = timestamp)
        }

        override fun setStarred(
            lemma: String,
            starred: Boolean,
            contextSentence: String?,
            timestamp: Long
        ) {
            val existing = vault[lemma]
            vault[lemma] =
                existing?.copy(
                    starred = starred,
                    savedContextSentence = contextSentence ?: existing.savedContextSentence,
                    lastSeenTimestamp = timestamp,
                )
                    ?: UserWordState(
                        lemma = lemma,
                        starred = starred,
                        savedContextSentence = contextSentence,
                        lastSeenTimestamp = timestamp,
                    )
        }

        override fun getStarredWords(): List<UserWordState> = vault.values.filter { it.starred }

        override fun getWordsByStatus(status: UserWordStatus): List<UserWordState> =
            vault.values.filter { it.status == status }

        override fun deleteWordState(lemma: String): Boolean = vault.remove(lemma) != null
    }
}
