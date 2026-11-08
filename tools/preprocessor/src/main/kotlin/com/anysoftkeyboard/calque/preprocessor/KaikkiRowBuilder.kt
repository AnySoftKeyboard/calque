package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.CognateDetector
import java.util.Locale

/** What an alias needs to know about the lemma it points to. */
data class LemmaInfo(val gloss: String, val isCognate: Boolean)

/**
 * Turns parsed Kaikki entries into dictionary rows (INGESTION.md Step 1).
 *
 * `frequency_rank` is the better (lower) of the lemma's rank and the surface form's own rank: a
 * form is familiar when either is common. The lemma rank is what the scoring thresholds were
 * designed for; the surface rank only matters when it is better, which is exactly the case of the
 * very frequent variants Kaikki lists separately (`tu` of `tuyo`, `muy` of `mucho`).
 */
internal class KaikkiRowBuilder(private val ranks: Map<String, Int>) {

    /**
     * The base row of [entry] followed by one row per form. A capitalized lemma is a proper noun:
     * its row is marked [DictionaryGrammar.PROPER_NOUN] so that it never hides the common word with
     * the same lowercase spelling, it is not flagged as a cognate, and its forms are not listed.
     */
    fun lemmaRows(entry: SubsetEntry): List<DictionaryRow> {
        val lemmaRank = rankOf(entry.word)
        val isProper = DictionaryGrammar.isProperNoun(entry.word)
        val isCognate = !isProper && CognateDetector.detectCognate(entry.word, entry.gloss)
        val base =
            DictionaryRow(
                surfaceForm = normalize(entry.word),
                lemma = entry.word,
                pos = entry.pos,
                grammarInfo =
                    if (isProper) DictionaryGrammar.PROPER_NOUN else DictionaryGrammar.BASE,
                englishGloss = entry.gloss,
                isCognate = isCognate,
                frequencyRank = lemmaRank,
            )
        val forms =
            entry.forms
                .filterNot { isProper }
                .map { form ->
                    DictionaryRow(
                        surfaceForm = normalize(form.form),
                        lemma = entry.word,
                        pos = entry.pos,
                        grammarInfo = form.tags.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        englishGloss = entry.gloss,
                        isCognate = isCognate,
                        frequencyRank = best(lemmaRank, rankOf(form.form)),
                    )
                }
        return listOf(base) + forms
    }

    /**
     * The row of an alias. With a known [target] the row belongs to the target lemma. A gloss that
     * Kaikki cut off after a colon (`inflection of dar:`) gets the target's gloss appended. Without
     * a target lemma in the data the alias keeps its own description. An alias with no target at
     * all (an acronym) is its own lemma.
     */
    fun aliasRow(alias: SubsetEntry, target: LemmaInfo?): DictionaryRow {
        val lemma = alias.formOf ?: alias.word
        val gloss = aliasGloss(alias.gloss, target)
        return DictionaryRow(
            surfaceForm = normalize(alias.word),
            lemma = lemma,
            pos = alias.pos,
            grammarInfo =
                if (alias.formOf == null) {
                    if (DictionaryGrammar.isProperNoun(alias.word)) {
                        DictionaryGrammar.PROPER_NOUN
                    } else {
                        DictionaryGrammar.BASE
                    }
                } else {
                    alias.tags
                        .filterNot { it in DERIVATIVE_TAGS }
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(", ")
                },
            englishGloss = gloss,
            isCognate = target?.isCognate ?: false,
            frequencyRank = best(rankOf(lemma), rankOf(alias.word)),
        )
    }

    private fun aliasGloss(gloss: String, target: LemmaInfo?): String {
        val trimmed = gloss.trimEnd(':', ' ')
        return if (gloss.trimEnd().endsWith(":") && target != null) {
            "$trimmed: ${target.gloss}"
        } else {
            trimmed
        }
    }

    private fun rankOf(word: String): Int? = ranks[normalize(word)]

    private fun best(first: Int?, second: Int?): Int? =
        if (first != null && second != null) minOf(first, second) else first ?: second

    private fun normalize(word: String): String = word.lowercase(Locale.ROOT)

    private companion object {
        /** Sense tags that only say "this is a variant"; they carry no grammar information. */
        val DERIVATIVE_TAGS = setOf("form-of", "alt-of", "misspelling")
    }
}
