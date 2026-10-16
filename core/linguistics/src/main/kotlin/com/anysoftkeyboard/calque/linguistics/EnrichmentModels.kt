package com.anysoftkeyboard.calque.linguistics

/** Status of the user's relationship with a vocabulary word. */
enum class UserWordStatus {
    NEW,
    LEARNING,
    MASTERED,
}

/** Read-only dictionary entry retrieved for a word form. */
data class DictionaryEntry(
    val surfaceForm: String,
    val lemma: String,
    val pos: String,
    val grammarInfo: String?,
    val englishGloss: String,
    val isCognate: Boolean,
    val frequencyRank: Int?,
)

/** 4-part false cognate trap entry. */
data class FalseCognateEntry(
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

/** Deconstructed verb with attached enclitic object pronouns. */
data class CliticBreakdown(
    val rootSurface: String,
    val rootLemma: String,
    val attachedPronouns: List<String>,
    val combinedEnglishGloss: String,
    val rootEntry: DictionaryEntry,
)

/**
 * Represents a segmented token (word or punctuation/whitespace) enriched with linguistic metadata.
 */
data class TokenSpan(
    val text: String,
    val normalizedWord: String?,
    val startIndex: Int,
    val endIndex: Int,
    val isWord: Boolean,
    val dictionaryEntry: DictionaryEntry? = null,
    val falseCognateEntry: FalseCognateEntry? = null,
    val cliticBreakdown: CliticBreakdown? = null,
    val userStatus: UserWordStatus = UserWordStatus.NEW,
)

/** Abstraction for batch querying linguistic data and user status. */
interface WordLookupProvider {
    fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry>

    fun batchLookupFalseCognates(words: Set<String>): Map<String, FalseCognateEntry>

    fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus>

    fun lookupSingleWord(word: String): DictionaryEntry?
}
