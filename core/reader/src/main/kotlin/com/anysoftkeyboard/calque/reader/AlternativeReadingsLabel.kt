package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.linguistics.DictionaryEntry

/**
 * Builds the one-line "also" text of the word peek card for the other readings of a spelling, for
 * example `adjective: cool, acceptable • verb (padrar): to sire`. The reading's lemma is only shown
 * when it differs from the primary reading's, because that is what tells `fue` (a form of *ser*)
 * from `fue` (a form of *ir*).
 */
object AlternativeReadingsLabel {
    const val MAX_SHOWN = 3
    const val MAX_GLOSS_LENGTH = 40
    private const val SEPARATOR = " • "
    private const val ELLIPSIS = "…"

    private val POS_NAMES =
        mapOf(
            "adj" to "adjective",
            "adv" to "adverb",
            "conj" to "conjunction",
            "det" to "determiner",
            "intj" to "interjection",
            "prep" to "preposition",
            "pron" to "pronoun",
            "num" to "number",
            "name" to "name",
        )

    /** The label for [alternatives] of a reading whose lemma is [primaryLemma], or null if none. */
    fun of(alternatives: List<DictionaryEntry>, primaryLemma: String?): String? {
        val shown = alternatives.map { describe(it, primaryLemma) }.distinct()
        if (shown.isEmpty()) return null
        val extra = shown.size - MAX_SHOWN
        val text = shown.take(MAX_SHOWN).joinToString(SEPARATOR)
        return if (extra > 0) "$text$SEPARATOR+$extra more" else text
    }

    private fun describe(entry: DictionaryEntry, primaryLemma: String?): String {
        val lemma =
            if (entry.lemma.equals(primaryLemma, ignoreCase = true)) "" else " (${entry.lemma})"
        return "${posName(entry.pos)}$lemma: ${shorten(entry.englishGloss)}"
    }

    /** Spells out the Kaikki part-of-speech abbreviations a learner would not know. */
    fun posName(pos: String): String = POS_NAMES[pos] ?: pos

    /** The first sense of a gloss, cut to [MAX_GLOSS_LENGTH] characters on a word boundary. */
    fun shorten(gloss: String): String {
        val first = gloss.substringBefore(';').trim()
        if (first.length <= MAX_GLOSS_LENGTH) return first
        val cut =
            first.take(MAX_GLOSS_LENGTH).substringBeforeLast(' ', first.take(MAX_GLOSS_LENGTH))
        return cut.trimEnd(',', ' ') + ELLIPSIS
    }
}
