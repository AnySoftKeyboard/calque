package com.anysoftkeyboard.calque.linguistics

import java.util.Locale

/**
 * Deconstructs Spanish verbs with attached enclitic object pronouns.
 *
 * Implements the Clitic Pronoun Deconstruction ("Pronoun Surgery") algorithm from
 * docs/INGESTION.md.
 */
object CliticDeconstructor {
    private const val MIN_STEM_LENGTH = 2

    private val PRONOUN_SUFFIXES =
        listOf(
            // Compound double clitics (length 5-6)
            "noslos",
            "noslas",
            "melos",
            "melas",
            "telos",
            "telas",
            "selos",
            "selas",
            "oslos",
            "oslas",
            // Compound double clitics (length 4-5)
            "noslo",
            "nosla",
            "melo",
            "mela",
            "telo",
            "tela",
            "selo",
            "sela",
            "oslo",
            "osla",
            // Single clitics (length 2-3)
            "nos",
            "les",
            "los",
            "las",
            "me",
            "te",
            "se",
            "le",
            "lo",
            "la",
            "os",
        )

    private val ACCENT_MAP =
        mapOf(
            'á' to 'a',
            'é' to 'e',
            'í' to 'i',
            'ó' to 'o',
            'ú' to 'u',
        )

    /**
     * Attempts to deconstruct [word] into a root verb and attached pronoun(s).
     *
     * @return [CliticBreakdown] if a valid verb root is found in [lookupProvider], null otherwise.
     */
    fun deconstruct(word: String, lookupProvider: WordLookupProvider): CliticBreakdown? {
        val normalized = word.lowercase(Locale.ROOT)

        for (suffix in PRONOUN_SUFFIXES) {
            val breakdown = attemptDeconstructWithSuffix(normalized, suffix, lookupProvider)
            if (breakdown != null) {
                return breakdown
            }
        }
        return null
    }

    private fun attemptDeconstructWithSuffix(
        normalized: String,
        suffix: String,
        lookupProvider: WordLookupProvider,
    ): CliticBreakdown? {
        if (!normalized.endsWith(suffix)) return null
        val stemLength = normalized.length - suffix.length
        if (stemLength < MIN_STEM_LENGTH) return null

        val strippedStem = normalized.substring(0, stemLength)
        val unaccentedStem = stripWrittenAccent(strippedStem)

        val rootEntry =
            lookupProvider.lookupSingleWord(unaccentedStem)
                ?: lookupProvider.lookupSingleWord(strippedStem)

        if (rootEntry == null || !isVerb(rootEntry.pos)) return null

        val pronounParts = splitPronounSuffix(suffix)
        val combinedGloss = buildCombinedGloss(rootEntry.englishGloss, pronounParts)
        return CliticBreakdown(
            rootSurface = rootEntry.surfaceForm,
            rootLemma = rootEntry.lemma,
            attachedPronouns = pronounParts,
            combinedEnglishGloss = combinedGloss,
            rootEntry = rootEntry,
        )
    }

    private fun isVerb(pos: String): Boolean {
        return pos.equals("verb", ignoreCase = true) || pos.startsWith("v", ignoreCase = true)
    }

    private fun stripWrittenAccent(stem: String): String {
        val sb = StringBuilder(stem.length)
        for (ch in stem) {
            sb.append(ACCENT_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    internal fun splitPronounSuffix(suffix: String): List<String> {
        val indirectPrefixes = listOf("nos", "me", "te", "se", "os")
        val directSuffixes = listOf("los", "las", "lo", "la")

        for (indirect in indirectPrefixes) {
            if (suffix.startsWith(indirect)) {
                val remainder = suffix.substring(indirect.length)
                if (directSuffixes.contains(remainder)) {
                    return listOf(indirect, remainder)
                }
            }
        }
        return listOf(suffix)
    }

    private fun buildCombinedGloss(verbGloss: String, pronouns: List<String>): String {
        val pronounStr = pronouns.joinToString(" + ")
        return "$verbGloss + [$pronounStr]"
    }
}
