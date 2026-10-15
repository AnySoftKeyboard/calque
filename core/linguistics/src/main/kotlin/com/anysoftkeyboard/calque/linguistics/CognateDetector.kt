package com.anysoftkeyboard.calque.linguistics

import java.text.Normalizer
import java.util.Locale
import kotlin.math.min

/**
 * Algorithmic cognate detector that determines whether a Spanish word and its English definition
 * share a transparent etymological root.
 *
 * Implements Step 2 from INGESTION.md using suffix transformation mapping, accent stripping, and
 * Levenshtein distance on word stems.
 */
object CognateDetector {
    /** Suffix conversion pairs between Spanish and English. */
    private val SUFFIX_MAP: List<Pair<String, String>> =
        listOf(
            "ción" to "tion",
            "sión" to "sion",
            "dad" to "ty",
            "mente" to "ly",
            "oso" to "ous",
            "osa" to "ous",
            "ico" to "ic",
            "ica" to "ic",
            "al" to "al",
            "cto" to "ct",
            "cta" to "ct",
            "ncia" to "nce",
            "ble" to "ble",
            "ario" to "ary",
            "aria" to "ary",
            "ivo" to "ive",
            "iva" to "ive",
            "ido" to "id",
            "ida" to "id",
            "ura" to "ure",
            "or" to "or",
            "ar" to "ate",
            "ar" to "e",
            "er" to "e",
            "ir" to "e",
            "izar" to "ize",
            "izar" to "ise",
        )

    private val PARENTHETICAL_REGEX = Regex("""\([^)]*\)|\[[^\]]*\]""")
    private val GLOSS_DELIMITERS_REGEX = Regex("""[,;/|\n]+""")
    private val LEADING_PARTICLES_REGEX = Regex("""^(?:to|a|an|the)\s+""")
    private val WORD_TOKEN_REGEX = Regex("""[\p{L}'-]+""")
    private val COMBINING_MARKS_REGEX = Regex("""\p{M}+""")

    /**
     * Detects whether [spanishWord] and [englishGloss] are cognates.
     *
     * @param spanishWord The Spanish word or surface token (e.g., "comunicación", "natural").
     * @param englishGloss The English definition or gloss (e.g., "communication", "to decide",
     *   "rapidly, quickly").
     * @return `true` if a transparent cognate relationship is detected, `false` otherwise.
     */
    fun detectCognate(spanishWord: String, englishGloss: String): Boolean {
        val es = normalize(spanishWord)
        if (es.length <= 2) return false

        val candidates = extractCandidateWords(englishGloss)
        if (candidates.isEmpty()) return false

        return candidates.any { en -> matchesCognate(es, en) }
    }

    /** Checks if a normalized Spanish word and a candidate English word are cognates. */
    fun matchesCognate(spanishWord: String, englishWord: String): Boolean {
        val es = normalize(spanishWord)
        val en = normalize(englishWord)

        if (es.length <= 2 || en.length <= 2) return false

        // Direct identical match for words > 3 characters (e.g. "natural" == "natural")
        if (es.length > 3 && es == en) {
            return true
        }

        // Direct match after accent normalization (e.g. "café" -> "cafe")
        val esUnaccented = stripAccents(es)
        if (esUnaccented.length > 3 && esUnaccented == en) {
            return true
        }

        // Suffix mapping and stem comparison
        for ((esSuffix, enSuffix) in SUFFIX_MAP) {
            if (es.endsWith(esSuffix) && en.endsWith(enSuffix)) {
                val esStem = stripAccents(es.removeSuffix(esSuffix))
                val enStem = en.removeSuffix(enSuffix)

                // Exact match on short stems (e.g. "nación" -> "na" == "nation" -> "na")
                if (esStem == enStem && esStem.length >= 2) {
                    return true
                }

                // Levenshtein distance <= 1 for stems of length >= 3
                if (esStem.length >= 3 && levenshteinDistance(esStem, enStem) <= 1) {
                    return true
                }
            }
        }

        return false
    }

    /** Normalizes a word by lowercasing and trimming whitespace and non-letter edge characters. */
    fun normalize(word: String): String {
        return word.trim().lowercase(Locale.ROOT).trim { !it.isLetter() }
    }

    /** Strips combining diacritical marks (e.g., accents, tildes) from a string. */
    fun stripAccents(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace(COMBINING_MARKS_REGEX, "")
    }

    /** Extracts the primary word from an English gloss. */
    fun extractPrimaryWord(englishGloss: String): String {
        return extractCandidateWords(englishGloss).firstOrNull().orEmpty()
    }

    /**
     * Extracts candidate English words from a gloss, stripping parenthetical notes, leading
     * grammatical markers ("to ", "a ", "an ", "the "), and splitting synonyms.
     */
    fun extractCandidateWords(englishGloss: String): List<String> {
        val cleanGloss = englishGloss.replace(PARENTHETICAL_REGEX, " ")
        val parts = cleanGloss.split(GLOSS_DELIMITERS_REGEX)
        val result = mutableListOf<String>()

        for (part in parts) {
            val trimmed = part.trim().lowercase(Locale.ROOT)
            val withoutParticle = trimmed.replace(LEADING_PARTICLES_REGEX, "")
            val words = WORD_TOKEN_REGEX.findAll(withoutParticle).map { it.value }
            for (word in words) {
                val normalized = normalize(word)
                if (normalized.length >= 2 && normalized !in result) {
                    result.add(normalized)
                }
            }
        }
        return result
    }

    /** Computes the Levenshtein distance between two strings using an O(min(N, M)) space buffer. */
    fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val (shorter, longer) = if (s1.length < s2.length) s1 to s2 else s2 to s1
        var prev = IntArray(shorter.length + 1) { it }
        var curr = IntArray(shorter.length + 1)

        for (i in 1..longer.length) {
            curr[0] = i
            val ch2 = longer[i - 1]
            for (j in 1..shorter.length) {
                val cost = if (ch2 == shorter[j - 1]) 0 else 1
                curr[j] =
                    min(
                        curr[j - 1] + 1,
                        min(prev[j] + 1, prev[j - 1] + cost),
                    )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[shorter.length]
    }
}
