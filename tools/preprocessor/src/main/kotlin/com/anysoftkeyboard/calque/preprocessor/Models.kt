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

/** Model for the cognate_rules seed table. */
data class CognateRuleRow(
    val ruleId: String,
    val englishSuffix: String,
    val spanishSuffix: String,
    val genderRule: String?,
    val spellingNote: String,
    val examplesJson: String,
)
