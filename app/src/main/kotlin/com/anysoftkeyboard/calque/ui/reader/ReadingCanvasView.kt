package com.anysoftkeyboard.calque.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.anysoftkeyboard.calque.linguistics.TokenSpan
import com.anysoftkeyboard.calque.reader.EnrichedSentence
import com.anysoftkeyboard.calque.reader.ReaderSessionEngine
import com.anysoftkeyboard.calque.ui.theme.CalqueColors

/** Interactive reading canvas rendering tokenized Spanish text and interlinear glosses. */
class ReadingCanvasView(
    context: Context,
    private val session: ReaderSessionEngine,
    private val onCloseSession: () -> Unit,
    private val onStateUpdated: () -> Unit,
) : LinearLayout(context) {

    private val scrollContent =
        LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

    init {
        orientation = VERTICAL
        setBackgroundColor(CalqueColors.Light.background)

        val scrollView =
            ScrollView(context).apply {
                layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
                addView(scrollContent)
            }
        addView(scrollView)
        addView(buildActionBar())
        render()
    }

    fun render() {
        scrollContent.removeAllViews()
        val state = session.state
        val currentSentence = state.currentSentence

        scrollContent.addView(
            buildHeader(state.title, state.currentSentenceIndex, state.sentences.size)
        )

        if (currentSentence != null) {
            scrollContent.addView(
                buildSentenceTokens(
                    currentSentence,
                    state.selectedTokenIndex,
                    state.showInterlinear
                )
            )

            val translation = currentSentence.translationEn
            if (state.showTranslation && translation != null) {
                scrollContent.addView(buildTranslationCard(translation))
            }

            val grammar = currentSentence.grammarCallout
            if (grammar != null) {
                scrollContent.addView(buildGrammarCallout(grammar))
            }
        } else if (state.isCompleted) {
            scrollContent.addView(buildCompletionCard())
        }
    }

    private fun buildHeader(title: String, index: Int, total: Int): View {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(16))

            val titleView =
                TextView(context).apply {
                    text = "$title (${index + 1}/$total)"
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(CalqueColors.Light.textSecondary)
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                }
            val closeBtn =
                TextView(context).apply {
                    text = "✕ Close"
                    textSize = 14f
                    setTextColor(CalqueColors.Light.slateBlueAccent)
                    setOnClickListener { onCloseSession() }
                }
            addView(titleView)
            addView(closeBtn)
        }
    }

    private fun buildSentenceTokens(
        sentence: EnrichedSentence,
        selectedIndex: Int?,
        showGloss: Boolean
    ): View {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(12), dp(16), dp(12), dp(16))
            setBackgroundColor(CalqueColors.Light.surface)

            val row = LinearLayout(context).apply { orientation = HORIZONTAL }
            addView(row)

            for ((i, token) in sentence.tokens.withIndex()) {
                val tokenBlock = buildTokenBlock(token, i, i == selectedIndex, showGloss)
                row.addView(tokenBlock)
            }
        }
    }

    private fun buildTokenBlock(
        token: TokenSpan,
        index: Int,
        isSelected: Boolean,
        showGloss: Boolean
    ): View {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            if (isSelected) {
                setBackgroundColor(0xFFE2E8F0.toInt())
            }

            val isFalseCognate = token.falseCognateEntry != null
            val isCognate = token.dictionaryEntry?.isCognate == true

            val word =
                TextView(context).apply {
                    text = token.text
                    textSize = 18f
                    typeface = Typeface.SERIF
                    setTextColor(
                        when {
                            isFalseCognate -> CalqueColors.Light.falseCognateAmber
                            isCognate -> CalqueColors.Light.cognateEmerald
                            else -> CalqueColors.Light.textPrimary
                        }
                    )
                }
            addView(word)

            val glossText = token.dictionaryEntry?.englishGloss
            if (showGloss && glossText != null) {
                val gloss =
                    TextView(context).apply {
                        text = glossText
                        textSize = 11f
                        setTextColor(CalqueColors.Light.textSecondary)
                    }
                addView(gloss)
            }

            setOnClickListener {
                session.selectToken(index)
                render()
                onStateUpdated()
            }
        }
    }

    private fun buildTranslationCard(translation: String): View {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(0xFFF1F5F9.toInt())
            val lp =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            lp.setMargins(0, dp(12), 0, 0)
            layoutParams = lp

            val label =
                TextView(context).apply {
                    text = "Natural Translation:"
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(CalqueColors.Light.textSecondary)
                }
            val text =
                TextView(context).apply {
                    this.text = translation
                    textSize = 15f
                    typeface = Typeface.SERIF
                    setTextColor(CalqueColors.Light.textPrimary)
                }
            addView(label)
            addView(text)
        }
    }

    private fun buildGrammarCallout(callout: String): View {
        return TextView(context).apply {
            text = "💡 $callout"
            textSize = 12f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setTextColor(CalqueColors.Light.slateBlueAccent)
            val lp =
                LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            lp.setMargins(0, dp(8), 0, 0)
            layoutParams = lp
        }
    }

    private fun buildCompletionCard(): View {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(32), dp(24), dp(32))

            val congrats =
                TextView(context).apply {
                    text = "🎉 Lesson Completed!"
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(CalqueColors.Light.cognateEmerald)
                }
            val returnBtn =
                Button(context).apply {
                    text = "Return to Curriculum"
                    setOnClickListener { onCloseSession() }
                }
            addView(congrats)
            addView(returnBtn)
        }
    }

    private fun buildActionBar(): View {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(CalqueColors.Light.surface)

            val glossBtn =
                TextView(context).apply {
                    text = "👁 Gloss"
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        session.toggleInterlinear()
                        render()
                        onStateUpdated()
                    }
                }
            val transBtn =
                TextView(context).apply {
                    text = "📝 Translate"
                    textSize = 14f
                    gravity = Gravity.CENTER
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        session.toggleTranslation()
                        render()
                        onStateUpdated()
                    }
                }
            val nextBtn =
                TextView(context).apply {
                    text = "Next ➔"
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.CENTER
                    setTextColor(CalqueColors.Light.slateBlueAccent)
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        session.nextSentence()
                        render()
                        onStateUpdated()
                    }
                }
            addView(glossBtn)
            addView(transBtn)
            addView(nextBtn)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
