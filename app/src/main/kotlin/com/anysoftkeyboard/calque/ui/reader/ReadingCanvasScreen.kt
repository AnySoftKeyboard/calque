package com.anysoftkeyboard.calque.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anysoftkeyboard.calque.linguistics.TokenSpan
import com.anysoftkeyboard.calque.reader.ReaderSessionEngine
import com.anysoftkeyboard.calque.ui.theme.CalqueTheme

/** Interactive reading canvas rendering tokenized Spanish text and interlinear glosses. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingCanvasScreen(
    session: ReaderSessionEngine,
    onCloseSession: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CalqueTheme.colors
    val state = session.state
    val currentSentence = state.currentSentence

    Column(
        modifier = modifier.fillMaxSize().background(colors.background),
    ) {
        // Main scrollable reading content
        Column(
            modifier =
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        "${state.title} (${state.currentSentenceIndex + 1}/${state.sentences.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier =
                        Modifier.clickable { onCloseSession() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "✕ Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.slateBlueAccent,
                    )
                }
            }

            if (currentSentence != null) {
                // Tokens Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    FlowRow(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for ((i, token) in currentSentence.tokens.withIndex()) {
                            TokenItem(
                                token = token,
                                isSelected = i == state.selectedTokenIndex,
                                showGloss = state.showInterlinear,
                                onClick = { session.selectToken(i) },
                            )
                        }
                    }
                }

                // Natural Translation Card
                val translation = currentSentence.translationEn
                if (state.showTranslation && translation != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = colors.backgroundSecondary,
                            ),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                text = "Natural Translation:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = translation,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Serif,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }

                // Grammar Callout
                val grammar = currentSentence.grammarCallout
                if (grammar != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 $grammar",
                        fontSize = 12.sp,
                        color = colors.slateBlueAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            } else if (state.isCompleted) {
                // Completion Card
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🎉 Lesson Completed!",
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = colors.cognateEmerald,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onCloseSession() }) { Text(text = "Return to Curriculum") }
                }
            }
        }

        // Action Bar at bottom
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clickable { session.toggleInterlinear() }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "👁 Gloss",
                        fontSize = 14.sp,
                        color =
                            if (state.showInterlinear) colors.slateBlueAccent
                            else colors.textPrimary,
                        fontWeight =
                            if (state.showInterlinear) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clickable { session.toggleTranslation() }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "📝 Translate",
                        fontSize = 14.sp,
                        color =
                            if (state.showTranslation) colors.slateBlueAccent
                            else colors.textPrimary,
                        fontWeight =
                            if (state.showTranslation) FontWeight.Bold else FontWeight.Normal,
                    )
                }

                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clickable { session.nextSentence() }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Next ➔",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.slateBlueAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenItem(
    token: TokenSpan,
    isSelected: Boolean,
    showGloss: Boolean,
    onClick: () -> Unit,
) {
    val colors = CalqueTheme.colors
    val isFalseCognate = token.falseCognateEntry != null
    val isCognate = token.dictionaryEntry?.isCognate == true

    val tokenColor =
        when {
            isFalseCognate -> colors.falseCognateAmber
            isCognate -> colors.cognateEmerald
            else -> colors.textPrimary
        }

    val backgroundColor =
        if (isSelected) {
            colors.slateBlueAccent.copy(alpha = 0.15f)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        }

    Column(
        modifier =
            Modifier.background(backgroundColor, shape = RoundedCornerShape(4.dp))
                .clickable { onClick() }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = token.text,
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            color = tokenColor,
        )

        val glossText = token.dictionaryEntry?.englishGloss
        if (showGloss && glossText != null) {
            Text(
                text = glossText,
                fontSize = 11.sp,
                color = colors.textSecondary,
            )
        }
    }
}
