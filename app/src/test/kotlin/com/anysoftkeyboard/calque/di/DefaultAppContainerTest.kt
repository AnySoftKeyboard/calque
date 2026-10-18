package com.anysoftkeyboard.calque.di

import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.reader.ReadingSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DefaultAppContainerTest {

    @Test
    fun testDefaultContainerInitializesWithInMemoryFallbacks() {
        val container = DefaultAppContainer()

        assertNotNull(container.dictionaryRepository)
        assertNotNull(container.falseCognatesRepository)
        assertNotNull(container.curatedLessonsRepository)
        assertNotNull(container.cognateRulesRepository)
        assertNotNull(container.userWordStateRepository)
        assertNotNull(container.userCustomTextRepository)
        assertNotNull(container.wordLookupProvider)
        assertNotNull(container.trapDetectorEngine)
        assertNotNull(container.wordTransformerEngine)
        assertNotNull(container.tenseShifterEngine)
        assertNotNull(container.numberOdometerEngine)
        assertNotNull(container.cliticSurgeryEngine)
        assertNotNull(container.omniBarEngine)
        assertNotNull(container.navStateManager)
    }

    @Test
    fun testWordLookupProviderFunctionsAcrossRepositories() {
        val container = DefaultAppContainer()
        container.userWordStateRepository.recordEncounter("hablar")
        container.userWordStateRepository.setWordStatus("hablar", UserWordStatus.MASTERED)

        val states = container.wordLookupProvider.batchLookupUserState(setOf("hablar", "comer"))
        assertEquals(UserWordStatus.MASTERED, states["hablar"])
        assertEquals(UserWordStatus.NEW, states["comer"])
    }

    @Test
    fun testCreateReaderSessionInitializesActiveSession() {
        val container = DefaultAppContainer()
        val source =
            ReadingSource.RawText(
                title = "Test Snippet",
                content = "Hola mundo.",
            )
        val session = container.createReaderSession(source)
        assertNotNull(session)
        assertEquals("Test Snippet", session.state.title)
    }
}
