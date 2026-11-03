package com.anysoftkeyboard.calque.ui.reader

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.reader.TokenInspection
import com.anysoftkeyboard.calque.ui.theme.CalqueColors

/** Peek inspection card presenting linguistic breakdown when a word token is tapped. */
class WordPeekCardView(
    context: Context,
    private val onToggleStar: (String) -> Unit,
    private val onDismiss: () -> Unit,
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(16))
        setBackgroundColor(CalqueColors.Light.surface)
        layoutParams =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun bind(inspection: TokenInspection?) {
        removeAllViews()
        if (inspection == null) {
            visibility = View.GONE
            return
        }
        visibility = View.VISIBLE

        addView(buildHeaderRow(inspection))
        addView(buildGlossRow(inspection))

        val trap = inspection.falseCognateTrap
        if (trap != null) {
            addView(buildTrapSection(trap))
        }

        val clitic = inspection.cliticSurgery
        if (clitic != null) {
            addView(buildCliticSection(clitic.rootVerb, clitic.procliticEquivalent))
        }

        val rule = inspection.cognateRuleNote
        if (rule != null) {
            addView(buildCognateSection(rule))
        }
    }

    private fun buildHeaderRow(inspection: TokenInspection): View {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val titles =
                LinearLayout(context).apply {
                    orientation = VERTICAL
                    layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

                    val word =
                        TextView(context).apply {
                            text = inspection.tokenSpan.text
                            textSize = 20f
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(CalqueColors.Light.textPrimary)
                        }
                    val subtitle =
                        TextView(context).apply {
                            val pos = inspection.pos ?: "word"
                            val status = inspection.userStatus.name.lowercase()
                            text = "Lemma: ${inspection.lemma} • $pos • $status"
                            textSize = 12f
                            setTextColor(CalqueColors.Light.textSecondary)
                        }
                    addView(word)
                    addView(subtitle)
                }
            addView(titles)
            addView(buildStarButton(inspection))
            addView(buildCloseButton())
        }
    }

    private fun buildStarButton(inspection: TokenInspection): TextView {
        return TextView(context).apply {
            text = if (inspection.isStarred) "★ Starred" else "☆ Star"
            textSize = 13f
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setTextColor(
                if (inspection.isStarred) {
                    CalqueColors.Light.falseCognateAmber
                } else {
                    CalqueColors.Light.textSecondary
                }
            )
            setOnClickListener { onToggleStar(inspection.lemma) }
        }
    }

    private fun buildCloseButton(): TextView {
        return TextView(context).apply {
            text = "✕"
            textSize = 16f
            setPadding(dp(10), dp(6), dp(4), dp(6))
            setTextColor(CalqueColors.Light.textSecondary)
            setOnClickListener { onDismiss() }
        }
    }

    private fun buildGlossRow(inspection: TokenInspection): View {
        return TextView(context).apply {
            val gloss = inspection.englishGloss ?: "—"
            text = "Meaning: $gloss"
            textSize = 15f
            setPadding(0, dp(8), 0, dp(4))
            setTextColor(CalqueColors.Light.textPrimary)
            typeface = Typeface.DEFAULT_BOLD
        }
    }

    private fun buildTrapSection(trap: FalseCognateEntry): View {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(0xFFFEF3C7.toInt())

            val title =
                TextView(context).apply {
                    text = "⚠️ False Cognate Alert: Not \"${trap.apparentEnglish}\"!"
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(CalqueColors.Light.falseCognateAmber)
                }
            val details =
                TextView(context).apply {
                    text =
                        "Actual meaning: ${trap.actualEnglish}\n" +
                            "To say \"${trap.apparentEnglish}\": use \"${trap.realSpanishForApparent}\"\n" +
                            "Tip: ${trap.disambiguationTip}"
                    textSize = 12f
                    setTextColor(CalqueColors.Light.textPrimary)
                }
            addView(title)
            addView(details)
        }
    }

    private fun buildCliticSection(root: String, proclitic: String): View {
        return TextView(context).apply {
            text = "✂️ Pronoun Surgery: Root \"$root\" • Proclitic: \"$proclitic\""
            textSize = 12f
            setPadding(0, dp(4), 0, dp(4))
            setTextColor(CalqueColors.Light.slateBlueAccent)
        }
    }

    private fun buildCognateSection(rule: String): View {
        return TextView(context).apply {
            text = "💡 Cognate Bridge: $rule"
            textSize = 12f
            setPadding(0, dp(4), 0, dp(4))
            setTextColor(CalqueColors.Light.cognateEmerald)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
