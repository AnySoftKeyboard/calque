package com.anysoftkeyboard.calque.di

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.data.models.UserCustomText
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryRepositoriesTest {

    @Test
    fun testDictionaryRepository() {
        val entry =
            DictionaryEntry(
                surfaceForm = "gato",
                lemma = "gato",
                pos = "noun",
                grammarInfo = "masculine",
                englishGloss = "cat",
                isCognate = false,
                frequencyRank = 100,
            )
        val repo = InMemoryDictionaryRepository(listOf(entry))

        assertEquals("cat", repo.getBySurfaceForm("gato")?.englishGloss)
        assertEquals("cat", repo.getBySurfaceForms(listOf("GATO"))["gato"]?.englishGloss)
        assertEquals(listOf("gato"), repo.getSurfacesForLemma("GATO"))
        assertEquals(1, repo.getEntriesForLemma("gato").size)
        assertEquals(1, repo.searchByPrefix("ga", 10).size)
    }

    @Test
    fun testFalseCognatesRepository() {
        val trap =
            FalseCognateEntry(
                surfaceForm = "actualmente",
                lemma = "actualmente",
                apparentEnglish = "actually",
                actualEnglish = "currently",
                realSpanishForApparent = "en realidad",
                severity = "HIGH_RISK",
                disambiguationTip = "Means currently, not actually.",
                exampleEs = "Actualmente vivo aquí.",
                exampleEn = "Currently I live here.",
            )
        val repo = InMemoryFalseCognatesRepository(listOf(trap))

        assertEquals("currently", repo.getBySurfaceForm("Actualmente")?.actualEnglish)
        assertEquals(1, repo.getByLemma("actualmente").size)
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun testCognateRulesRepository() {
        val rule =
            CognateRule(
                ruleId = "tion_to_cion",
                englishSuffix = "-tion",
                spanishSuffix = "-ción",
                genderRule = "feminine",
                spellingNote = "Accent on o",
                examplesJson = "[]",
            )
        val repo = InMemoryCognateRulesRepository(listOf(rule))

        assertEquals(rule, repo.getById("tion_to_cion"))
        assertEquals(rule, repo.findRuleForSpanishWord("conversación"))
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun testCuratedLessonsRepository() {
        val lesson =
            CuratedLesson(
                lessonId = "lvl0_00_test",
                level = 0,
                sequenceOrder = 1,
                title = "Test Lesson",
                targetConcept = "Concept",
                contentJson = "{}",
            )
        val repo = InMemoryCuratedLessonsRepository(listOf(lesson))

        assertEquals(lesson, repo.getById("lvl0_00_test"))
        assertEquals(1, repo.getByLevel(0).size)
        assertEquals(0, repo.getByLevel(1).size)
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun testUserWordStateRepository() {
        val repo = InMemoryUserWordStateRepository()
        assertNull(repo.getWordState("hablar"))

        repo.recordEncounter("hablar")
        val state = repo.getWordState("hablar")
        assertNotNull(state)
        assertEquals(UserWordStatus.LEARNING, state?.status)
        assertEquals(1, state?.encounterCount)

        repo.setStarred("hablar", true, "Hablo español.")
        assertTrue(repo.getStarredWords().first().starred)

        repo.setWordStatus("hablar", UserWordStatus.MASTERED)
        assertEquals(1, repo.getWordsByStatus(UserWordStatus.MASTERED).size)

        repo.deleteWordState("hablar")
        assertNull(repo.getWordState("hablar"))
    }

    @Test
    fun testUserCustomTextRepository() {
        val repo = InMemoryUserCustomTextRepository()
        val text =
            UserCustomText(
                title = "Don Quijote",
                originalSpanish = "En un lugar de la Mancha...",
                readabilityLevel = 3,
                createdTimestamp = 1000L,
            )
        val id = repo.insert(text)
        assertTrue(id > 0)

        val retrieved = repo.getById(id)
        assertNotNull(retrieved)
        assertEquals("Don Quijote", retrieved?.title)

        val updated = retrieved?.copy(title = "Don Quixote")
        assertTrue(repo.update(updated!!))
        assertEquals("Don Quixote", repo.getById(id)?.title)

        assertEquals(1, repo.getAll().size)
        assertTrue(repo.delete(id))
        assertEquals(0, repo.getAll().size)
    }
}
