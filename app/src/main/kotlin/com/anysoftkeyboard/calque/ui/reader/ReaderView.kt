package com.anysoftkeyboard.calque.ui.reader

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.anysoftkeyboard.calque.di.AppContainer

/** Top-level Reader view hosting the curriculum browser, reading canvas, and word peek sheet. */
class ReaderView(
    context: Context,
    private val appContainer: AppContainer,
) : FrameLayout(context) {

    val coordinator = ReaderCoordinator(appContainer)

    private val curriculumView =
        CurriculumView(
            context = context,
            onSelectLesson = { lesson -> coordinator.startCuratedLesson(lesson) },
            onStartCustomText = { title, content -> coordinator.startRawText(title, content) },
        )

    private val sessionContainer =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
        }

    private var activeCanvasView: ReadingCanvasView? = null

    private val peekCardView =
        WordPeekCardView(
            context = context,
            onToggleStar = { lemma ->
                val sessionState = (coordinator.state as? ReaderScreenState.ActiveSession)?.session
                sessionState?.toggleStarred(lemma)
                refreshPeekCard()
            },
            onDismiss = {
                val session = (coordinator.state as? ReaderScreenState.ActiveSession)?.session
                session?.clearSelection()
                activeCanvasView?.render()
                refreshPeekCard()
            },
        )

    init {
        addView(curriculumView)
        sessionContainer.addView(peekCardView)
        addView(sessionContainer)

        coordinator.addListener { renderState() }
        renderState()
    }

    fun handleBackPressed(): Boolean {
        val state = coordinator.state
        if (state is ReaderScreenState.ActiveSession) {
            if (state.session.state.activeInspection != null) {
                state.session.clearSelection()
                activeCanvasView?.render()
                refreshPeekCard()
                return true
            }
            coordinator.closeSession()
            return true
        }
        return false
    }

    private fun renderState() {
        when (val currentState = coordinator.state) {
            is ReaderScreenState.Curriculum -> {
                curriculumView.visibility = VISIBLE
                sessionContainer.visibility = GONE
                curriculumView.bind(currentState.lessons)
            }
            is ReaderScreenState.ActiveSession -> {
                curriculumView.visibility = GONE
                sessionContainer.visibility = VISIBLE
                setupActiveSession(currentState)
            }
        }
    }

    private fun setupActiveSession(state: ReaderScreenState.ActiveSession) {
        if (activeCanvasView == null) {
            val canvas =
                ReadingCanvasView(
                        context = context,
                        session = state.session,
                        onCloseSession = { coordinator.closeSession() },
                        onStateUpdated = { refreshPeekCard() },
                    )
                    .apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                0,
                                1f,
                            )
                    }
            activeCanvasView = canvas
            sessionContainer.addView(canvas, 0)
        } else {
            activeCanvasView?.render()
        }
        refreshPeekCard()
    }

    private fun refreshPeekCard() {
        val session = (coordinator.state as? ReaderScreenState.ActiveSession)?.session
        peekCardView.bind(session?.state?.activeInspection)
    }
}
