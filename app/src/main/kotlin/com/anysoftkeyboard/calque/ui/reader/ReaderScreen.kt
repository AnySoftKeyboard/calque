package com.anysoftkeyboard.calque.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Primary composable for the Reader destination conforming to the MVI unidirectional data flow
 * pattern.
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.currentTab) {
        ReaderTab.CURRICULUM -> {
            CurriculumScreen(
                lessons = uiState.curriculumLessons,
                onSelectLesson = { viewModel.onEvent(ReaderUiEvent.SelectLesson(it)) },
                onStartCustomText = { title, content ->
                    viewModel.onEvent(ReaderUiEvent.StartRawText(title, content))
                },
                modifier = modifier,
            )
        }
        ReaderTab.ACTIVE_SESSION -> {
            val activeSession = uiState.activeSession
            if (activeSession != null) {
                val activeInspection = activeSession.activeInspection

                BackHandler {
                    if (activeInspection != null) {
                        viewModel.onEvent(ReaderUiEvent.ClearSelection)
                    } else {
                        viewModel.onEvent(ReaderUiEvent.CloseSession)
                    }
                }

                Box(modifier = modifier.fillMaxSize()) {
                    ReadingCanvasScreen(
                        activeSession = activeSession,
                        onSelectToken = { viewModel.onEvent(ReaderUiEvent.SelectToken(it)) },
                        onToggleInterlinear = {
                            viewModel.onEvent(ReaderUiEvent.ToggleInterlinear)
                        },
                        onToggleTranslation = {
                            viewModel.onEvent(ReaderUiEvent.ToggleTranslation)
                        },
                        onNextSentence = { viewModel.onEvent(ReaderUiEvent.NextSentence) },
                        onCloseSession = { viewModel.onEvent(ReaderUiEvent.CloseSession) },
                        modifier = Modifier.fillMaxSize(),
                    )

                    if (activeInspection != null) {
                        WordPeekCard(
                            inspection = activeInspection,
                            onToggleStar = { lemma ->
                                viewModel.onEvent(ReaderUiEvent.ToggleStarred(lemma))
                            },
                            onDismiss = { viewModel.onEvent(ReaderUiEvent.ClearSelection) },
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                }
            }
        }
    }
}

/** Legacy adapter overload for [ReaderScreen] using [ReaderCoordinator]. */
@Composable
fun ReaderScreen(
    coordinator: ReaderCoordinator,
    modifier: Modifier = Modifier,
) {
    var screenState by remember { mutableStateOf(coordinator.state) }
    var sessionRevision by remember { mutableIntStateOf(0) }

    DisposableEffect(coordinator) {
        val listener: (ReaderScreenState) -> Unit = { newState ->
            screenState = newState
            sessionRevision = 0
        }
        coordinator.addListener(listener)
        screenState = coordinator.state
        onDispose { coordinator.removeListener(listener) }
    }

    when (val state = screenState) {
        is ReaderScreenState.Curriculum -> {
            CurriculumScreen(
                lessons = state.lessons,
                onSelectLesson = { coordinator.startCuratedLesson(it) },
                onStartCustomText = { title, content -> coordinator.startRawText(title, content) },
                modifier = modifier,
            )
        }
        is ReaderScreenState.ActiveSession -> {
            @Suppress("UNUSED_VARIABLE") val revision = sessionRevision
            val session = state.session
            val activeInspection = session.state.activeInspection

            BackHandler {
                if (activeInspection != null) {
                    session.clearSelection()
                    sessionRevision++
                } else {
                    coordinator.closeSession()
                }
            }

            Box(modifier = modifier.fillMaxSize()) {
                ReadingCanvasScreen(
                    session = session,
                    onCloseSession = { coordinator.closeSession() },
                    modifier = Modifier.fillMaxSize(),
                )

                if (activeInspection != null) {
                    WordPeekCard(
                        inspection = activeInspection,
                        onToggleStar = { lemma ->
                            session.toggleStarred(lemma)
                            sessionRevision++
                        },
                        onDismiss = {
                            session.clearSelection()
                            sessionRevision++
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
