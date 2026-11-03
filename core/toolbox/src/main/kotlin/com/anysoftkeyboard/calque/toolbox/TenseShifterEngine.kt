package com.anysoftkeyboard.calque.toolbox

import java.util.Locale

/** Domain engine for the Tense Shifter (Verb Timeline Sandbox). */
object TenseShifterEngine {

    private const val MIN_LEMMA_LENGTH = 3
    private const val VERB_ENDING_LENGTH = 2

    /** Returns the list of standard supported verbs in the Tense Shifter sandbox. */
    fun supportedVerbs(): List<String> =
        listOf(
            "hablar",
            "comer",
            "vivir",
            "ser",
            "estar",
            "ir",
            "tener",
            "hacer",
            "poder",
            "querer",
            "ver",
            "saber",
            "decir",
            "dar",
        )

    /** Conjugates [verb] for the specified [pronoun] and [tense]. */
    fun conjugate(
        verb: String,
        pronoun: SubjectPronoun,
        tense: ReadingTense,
    ): ConjugatedVerbForm {
        val lower = verb.lowercase(Locale.ROOT).trim()

        val irregularForm = IrregularConjugationData.getConjugation(lower, pronoun, tense)
        if (irregularForm != null) {
            return irregularForm
        }

        return VerbConjugationTables.conjugateRegular(lower, pronoun, tense)
    }

    /**
     * Replaces the verb in [sentence] corresponding to [lemma] with its conjugated form in [tense]
     * and [pronoun].
     */
    fun shiftSentence(
        sentence: String,
        lemma: String,
        pronoun: SubjectPronoun,
        tense: ReadingTense,
    ): ShiftedSentenceResult {
        val newForm = conjugate(lemma, pronoun, tense)
        val tokens = sentence.split(Regex("(?<=\\s)|(?=\\s)|(?=[.,!?;:])|(?<=[.,!?;:])"))

        val replacedTokens = mutableListOf<String>()
        var foundVerb = false
        var originalVerb = lemma

        for (token in tokens) {
            val cleanToken = token.lowercase(Locale.ROOT)
            if (!foundVerb && matchesVerbStemOrLemma(cleanToken, lemma)) {
                originalVerb = token
                replacedTokens.add(newForm.fullForm)
                foundVerb = true
            } else {
                replacedTokens.add(token)
            }
        }

        val shiftedSentence =
            if (foundVerb) {
                replacedTokens.joinToString("")
            } else {
                "$sentence -> ${newForm.fullForm}"
            }

        return ShiftedSentenceResult(
            originalSentence = sentence,
            shiftedSentence = shiftedSentence,
            originalVerb = originalVerb,
            newVerb = newForm.fullForm,
            tense = tense,
            pronoun = pronoun,
        )
    }

    private fun matchesVerbStemOrLemma(token: String, lemma: String): Boolean {
        if (token.isEmpty() || lemma.length < MIN_LEMMA_LENGTH) return false
        val stem = lemma.dropLast(VERB_ENDING_LENGTH)
        return token == lemma || token.startsWith(stem)
    }
}
