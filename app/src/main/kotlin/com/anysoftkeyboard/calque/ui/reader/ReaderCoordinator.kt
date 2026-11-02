package com.anysoftkeyboard.calque.ui.reader

import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.di.AppContainer
import com.anysoftkeyboard.calque.reader.ReadingSource

/**
 * Coordinates user interaction, lesson loading, and session lifecycle for the Reader destination.
 */
class ReaderCoordinator(
    private val appContainer: AppContainer,
) {
    var state: ReaderScreenState = ReaderScreenState.Curriculum(emptyList())
        private set

    private val listeners = mutableListOf<(ReaderScreenState) -> Unit>()

    init {
        loadCurriculum()
    }

    fun loadCurriculum() {
        val lessons = appContainer.curatedLessonsRepository.getAll()
        updateState(ReaderScreenState.Curriculum(lessons))
    }

    fun startCuratedLesson(lesson: CuratedLesson) {
        val engine = appContainer.createReaderSession(ReadingSource.Curated(lesson))
        updateState(ReaderScreenState.ActiveSession(engine))
    }

    fun startCuratedLessonById(lessonId: String): Boolean {
        val lesson = appContainer.curatedLessonsRepository.getById(lessonId) ?: return false
        startCuratedLesson(lesson)
        return true
    }

    fun startRawText(title: String, content: String) {
        val source = ReadingSource.RawText(title = title, content = content)
        val engine = appContainer.createReaderSession(source)
        updateState(ReaderScreenState.ActiveSession(engine))
    }

    fun closeSession() {
        loadCurriculum()
    }

    fun addListener(listener: (ReaderScreenState) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (ReaderScreenState) -> Unit) {
        listeners.remove(listener)
    }

    private fun updateState(newState: ReaderScreenState) {
        state = newState
        for (listener in listeners) {
            listener(newState)
        }
    }
}
