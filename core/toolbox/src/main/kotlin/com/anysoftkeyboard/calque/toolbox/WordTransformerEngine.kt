package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.data.models.CognateRule
import java.util.Locale

/** Domain engine for the Word Transformer (Cognate Suffix Bridge Sandbox). */
object WordTransformerEngine {

    /** Transforms an English word to its Spanish cognate counterpart using [rules]. */
    fun transformEnglishToSpanish(
        englishWord: String,
        rules: List<CognateRule>,
    ): TransformedCognateWord? {
        val normalized = englishWord.trim().lowercase(Locale.ROOT)
        // Find longest matching english suffix
        val matchingRule =
            rules
                .filter { normalized.endsWith(it.englishSuffix.removePrefix("-")) }
                .maxByOrNull { it.englishSuffix.length } ?: return null

        val rawSuffix = matchingRule.englishSuffix.removePrefix("-")
        val stem = normalized.removeSuffix(rawSuffix)
        if (stem.length < 2) return null

        val adjustedStem = adjustEnglishStemForSpanish(stem)
        val spanishSuffix = matchingRule.spanishSuffix.removePrefix("-")
        val result = adjustedStem + spanishSuffix

        return TransformedCognateWord(
            sourceWord = englishWord,
            resultWord = result,
            ruleId = matchingRule.ruleId,
            stem = stem,
            sourceSuffix = matchingRule.englishSuffix,
            targetSuffix = matchingRule.spanishSuffix,
            genderRule = matchingRule.genderRule,
            spellingNote = matchingRule.spellingNote,
        )
    }

    /** Transforms a Spanish word to its English cognate counterpart using [rules]. */
    fun transformSpanishToEnglish(
        spanishWord: String,
        rules: List<CognateRule>,
    ): TransformedCognateWord? {
        val normalized = spanishWord.trim().lowercase(Locale.ROOT)
        // Find longest matching spanish suffix
        val matchingRule =
            rules
                .filter { normalized.endsWith(it.spanishSuffix.removePrefix("-")) }
                .maxByOrNull { it.spanishSuffix.length } ?: return null

        val rawSuffix = matchingRule.spanishSuffix.removePrefix("-")
        val stem = normalized.removeSuffix(rawSuffix)
        if (stem.length < 2) return null

        val englishSuffix = matchingRule.englishSuffix.removePrefix("-")
        val result = stem + englishSuffix

        return TransformedCognateWord(
            sourceWord = spanishWord,
            resultWord = result,
            ruleId = matchingRule.ruleId,
            stem = stem,
            sourceSuffix = matchingRule.spanishSuffix,
            targetSuffix = matchingRule.englishSuffix,
            genderRule = matchingRule.genderRule,
            spellingNote = matchingRule.spellingNote,
        )
    }

    private fun adjustEnglishStemForSpanish(stem: String): String {
        return stem
            .replace("ph", "f")
            .replace("th", "t")
            .replace("tt", "t")
            .replace("ss", "s")
            .replace("mm", "m")
            .replace("pp", "p")
            .replace("ff", "f")
            .replace("bb", "b")
            .replace("dd", "d")
    }
}
