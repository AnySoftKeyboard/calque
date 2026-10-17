package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry

/** Result model for Smart Omni-Bar queries. */
sealed interface OmniBarResult {
    val input: String

    data class NumberResult(
        override val input: String,
        val number: Long,
        val masculineWords: String,
        val feminineWords: String,
    ) : OmniBarResult

    data class SuffixRuleResult(
        override val input: String,
        val rule: CognateRule,
    ) : OmniBarResult

    data class FalseCognateResult(
        override val input: String,
        val card: FalseCognateEntry,
    ) : OmniBarResult

    data class CliticResult(
        override val input: String,
        val surgery: CliticSurgeryResult,
    ) : OmniBarResult

    data class WordLookupResult(
        override val input: String,
        val word: DictionaryEntry,
    ) : OmniBarResult

    data class None(
        override val input: String,
    ) : OmniBarResult
}

/** Filter category for the Trap Detector false cognates sandbox. */
enum class TrapFilter {
    ALL,
    HIGH_RISK,
    MILD_NUANCE,
    SEEN_IN_READING,
}

/** 2-choice comprehension check question for "Spot the Trap" quiz mode. */
data class TrapQuizQuestion(
    val sentenceEs: String,
    val targetWord: String,
    val prompt: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String,
)

/** Result of a cognate suffix bridge transformation. */
data class TransformedCognateWord(
    val sourceWord: String,
    val resultWord: String,
    val ruleId: String,
    val stem: String,
    val sourceSuffix: String,
    val targetSuffix: String,
    val genderRule: String?,
    val spellingNote: String,
)

/** The 4 core reading tenses featured in the Tense Shifter sandbox. */
enum class ReadingTense(val displayName: String, val category: String) {
    PAST_COMPLETED("Pretérito", "Past Completed"),
    PAST_ONGOING("Imperfecto", "Past Ongoing"),
    PRESENT("Presente", "Present"),
    NEAR_FUTURE("Ir a + Infinitivo", "Near Future"),
}

/** The 5 subject pronoun categories used for reading verb conjugations. */
enum class SubjectPronoun(val displayPronoun: String) {
    YO("yo"),
    TU("tú"),
    EL_ELLA_USTED("él / ella / usted"),
    NOSOTROS("nosotros / nosotras"),
    ELLOS_USTEDES("ellos / ellas / ustedes"),
}

/** Conjugated verb details with stem/ending decomposition. */
data class ConjugatedVerbForm(
    val lemma: String,
    val pronoun: SubjectPronoun,
    val tense: ReadingTense,
    val fullForm: String,
    val stem: String,
    val suffix: String,
    val auxiliary: String? = null,
)

/** Sentence morphing result when shifting tense in a context sentence. */
data class ShiftedSentenceResult(
    val originalSentence: String,
    val shiftedSentence: String,
    val originalVerb: String,
    val newVerb: String,
    val tense: ReadingTense,
    val pronoun: SubjectPronoun,
)

/** Number conversion with masculine and feminine agreement forms. */
data class NumberAgreementResult(
    val number: Long,
    val masculineText: String,
    val feminineText: String,
)

/** Spelled-out Spanish clock time phrase. */
data class ClockTimeResult(
    val hour: Int,
    val minute: Int,
    val phrase: String,
    val periodOfDay: String,
)

/** Spelled-out Spanish date phrase. */
data class DatePhraseResult(
    val day: Int,
    val month: Int,
    val year: Int?,
    val phrase: String,
)

/** Color-coded functional role of a block in pronoun surgery. */
enum class PronounBlockType {
    ROOT_VERB,
    INDIRECT_PRONOUN,
    DIRECT_PRONOUN,
    REFLEXIVE_PRONOUN,
}

/** Single visual block in the pronoun surgery workbench. */
data class PronounBlock(
    val type: PronounBlockType,
    val text: String,
    val roleName: String,
    val meaning: String,
)

/** Result of pronoun surgery on a compound verb with enclitics. */
data class CliticSurgeryResult(
    val originalWord: String,
    val rootVerb: String,
    val rootMood: String,
    val blocks: List<PronounBlock>,
    val separatedPronouns: List<String>,
    val procliticEquivalent: String,
    val explanation: String,
)
