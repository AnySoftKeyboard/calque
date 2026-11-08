package com.anysoftkeyboard.calque.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
