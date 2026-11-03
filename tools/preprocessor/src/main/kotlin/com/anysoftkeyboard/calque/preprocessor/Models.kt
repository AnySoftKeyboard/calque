package com.anysoftkeyboard.calque.preprocessor

/** Model for the dictionary seed table. */
data class DictionaryRow(
    val surfaceForm: String,
    val lemma: String,
    val pos: String,
    val grammarInfo: String?,
    val englishGloss: String,
    val isCognate: Boolean,
    val frequencyRank: Int?,
)

/** Model for the false_cognates seed table. */
data class FalseCognateRow(
    val surfaceForm: String,
    val lemma: String,
    val apparentEnglish: String,
    val actualEnglish: String,
    val realSpanishForApparent: String,
    val severity: String,
    val disambiguationTip: String,
    val exampleEs: String,
    val exampleEn: String,
)

/** Model representing a raw curated seed entry before expansion. */
data class FalseCognateSeed(
    val lemma: String,
    val apparentEnglish: String,
    val actualEnglish: String,
    val realSpanishForApparent: String,
    val severity: String,
    val disambiguationTip: String,
    val exampleEs: String,
    val exampleEn: String,
)

/** Model for the curated_lessons seed table. */
data class CuratedLessonRow(
    val lessonId: String,
    val level: Int,
    val sequenceOrder: Int,
    val title: String,
    val targetConcept: String,
    val contentJson: String,
)

/** Represents a single token in an interlinear word-for-word literal gloss. */
data class InterlinearGlossToken(
    val es: String,
    val en: String,
)

/** Represents a structured sentence inside a curated lesson's `content_json`. */
data class LessonSentence(
    val sentenceEs: String,
    val translationEn: String,
    val interlinearGloss: List<InterlinearGlossToken>,
    val grammarCallout: String?,
)

/** Model for the cognate_rules seed table. */
data class CognateRuleRow(
    val ruleId: String,
    val englishSuffix: String,
    val spanishSuffix: String,
    val genderRule: String?,
    val spellingNote: String,
    val examplesJson: String,
)

/** Represents a single English-to-Spanish word pair in a cognate transformation rule. */
data class CognateRuleExample(
    val english: String,
    val spanish: String,
)
