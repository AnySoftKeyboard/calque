package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import java.util.Locale

/** Domain engine for the Trap Detector (False Cognates Sandbox). */
object TrapDetectorEngine {

    /** Filters a collection of [traps] according to [filter] mode and [seenLemmas]. */
    fun filterTraps(
        traps: List<FalseCognateEntry>,
        filter: TrapFilter,
        seenLemmas: Set<String> = emptySet(),
    ): List<FalseCognateEntry> {
        val normalizedSeen = seenLemmas.map { it.lowercase(Locale.ROOT) }.toSet()
        return when (filter) {
            TrapFilter.ALL -> traps
            TrapFilter.HIGH_RISK ->
                traps.filter { it.severity.equals("HIGH_RISK", ignoreCase = true) }
            TrapFilter.MILD_NUANCE ->
                traps.filter { it.severity.equals("MILD_NUANCE", ignoreCase = true) }
            TrapFilter.SEEN_IN_READING ->
                traps.filter {
                    normalizedSeen.contains(it.lemma.lowercase(Locale.ROOT)) ||
                        normalizedSeen.contains(it.surfaceForm.lowercase(Locale.ROOT))
                }
        }
    }

    /**
     * Generates a 2-choice "Spot the Trap" quiz question from a [FalseCognateEntry].
     *
     * @param trap The false cognate card to turn into a question.
     * @param correctFirst Whether the correct answer is the first option (for deterministic
     *   testing).
     */
    fun createQuizQuestion(
        trap: FalseCognateEntry,
        correctFirst: Boolean = true,
    ): TrapQuizQuestion {
        val correct = trap.actualEnglish
        val distractor = trap.apparentEnglish
        val options = if (correctFirst) listOf(correct, distractor) else listOf(distractor, correct)
        val correctIndex = if (correctFirst) 0 else 1

        val sentence =
            if (trap.exampleEs.isNotBlank()) {
                trap.exampleEs
            } else {
                "Cuidado con la palabra '${trap.surfaceForm}'."
            }

        return TrapQuizQuestion(
            sentenceEs = sentence,
            targetWord = trap.surfaceForm,
            prompt = "What does \"${trap.surfaceForm}\" mean in this sentence?",
            options = options,
            correctOptionIndex = correctIndex,
            explanation = trap.disambiguationTip,
        )
    }

    /** Generates quiz questions for all provided [traps]. */
    fun generateQuiz(
        traps: List<FalseCognateEntry>,
        alternateAnswers: Boolean = true,
    ): List<TrapQuizQuestion> {
        return traps.mapIndexed { index, card ->
            val correctFirst = if (alternateAnswers) index % 2 == 0 else true
            createQuizQuestion(card, correctFirst = correctFirst)
        }
    }
}
