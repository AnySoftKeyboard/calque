package com.anysoftkeyboard.calque.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.ui.theme.CalqueColors

/** Curriculum landing view listing structured syntactic progression modules. */
class CurriculumView(
    context: Context,
    private val onSelectLesson: (CuratedLesson) -> Unit,
    private val onStartCustomText: (String, String) -> Unit,
) : ScrollView(context) {

    private val container =
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
        }

    init {
        addView(container)
        setBackgroundColor(CalqueColors.Light.background)
    }

    fun bind(lessons: List<CuratedLesson>) {
        container.removeAllViews()
        container.addView(buildCurriculumHeader())
        container.addView(buildQuickStarterCard())

        if (lessons.isEmpty()) {
            container.addView(buildEmptyNotice())
        } else {
            for (lesson in lessons) {
                container.addView(buildLessonCard(lesson))
            }
        }
    }

    private fun buildCurriculumHeader(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))

            val title =
                TextView(context).apply {
                    text = "Syntactic Progression Path"
                    textSize = 18f
                    typeface = Typeface.SERIF
                    setTextColor(CalqueColors.Light.textPrimary)
                }
            val subtitle =
                TextView(context).apply {
                    text = "Decode Spanish naturally using cognates, syntax patterns, and glosses."
                    textSize = 13f
                    setTextColor(CalqueColors.Light.textSecondary)
                }
            addView(title)
            addView(subtitle)
        }
    }

    private fun buildQuickStarterCard(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CalqueColors.Light.surface)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            lp.setMargins(0, 0, 0, dp(16))
            layoutParams = lp

            val cardTitle =
                TextView(context).apply {
                    text = "📖 Try Sample: Sentence Zero"
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(CalqueColors.Light.slateBlueAccent)
                }
            val sampleSnippet =
                TextView(context).apply {
                    text = "El hotel moderno es excelente y la música es fantástica."
                    textSize = 13f
                    setPadding(0, dp(4), 0, dp(8))
                    setTextColor(CalqueColors.Light.textSecondary)
                }
            addView(cardTitle)
            addView(sampleSnippet)

            setOnClickListener {
                onStartCustomText(
                    "Sentence Zero",
                    "El hotel moderno es excelente y la música es fantástica.",
                )
            }
        }
    }

    private fun buildLessonCard(lesson: CuratedLesson): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(CalqueColors.Light.surface)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            lp.setMargins(0, 0, 0, dp(12))
            layoutParams = lp

            val badge =
                TextView(context).apply {
                    text = "LEVEL ${lesson.level} • MODULE ${lesson.sequenceOrder}"
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(CalqueColors.Light.cognateEmerald)
                }
            val title =
                TextView(context).apply {
                    text = lesson.title
                    textSize = 16f
                    typeface = Typeface.SERIF
                    setPadding(0, dp(2), 0, dp(2))
                    setTextColor(CalqueColors.Light.textPrimary)
                }
            val concept =
                TextView(context).apply {
                    text = "Focus: ${lesson.targetConcept}"
                    textSize = 12f
                    setTextColor(CalqueColors.Light.textSecondary)
                }
            addView(badge)
            addView(title)
            addView(concept)

            setOnClickListener { onSelectLesson(lesson) }
        }
    }

    private fun buildEmptyNotice(): View {
        return TextView(context).apply {
            text = "No curated lessons available in database."
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(32), dp(16), dp(32))
            setTextColor(CalqueColors.Light.textSecondary)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
