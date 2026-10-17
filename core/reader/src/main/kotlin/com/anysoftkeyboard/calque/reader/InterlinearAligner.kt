package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.data.models.InterlinearGlossToken
import com.anysoftkeyboard.calque.linguistics.TokenSpan

/**
 * Generates word-for-word interlinear literal glosses from enriched [TokenSpan]s.
 *
 * Provides instant literal scaffolding beneath Spanish words for custom texts without requiring
 * curated gloss datasets.
 */
object InterlinearAligner {

    private const val MAX_GLOSS_LENGTH = 20

    /**
     * Produces a list of [InterlinearGlossToken]s matching the input [tokens].
     *
     * @param tokens List of enriched tokens from [ReaderEnricher].
     * @return List of gloss pairs aligned 1-to-1 with input tokens.
     */
    fun generateGloss(tokens: List<TokenSpan>): List<InterlinearGlossToken> {
        return tokens.map { token ->
            val english = if (token.isWord) deriveWordGloss(token) else ""
            InterlinearGlossToken(es = token.text, en = english)
        }
    }

    private fun deriveWordGloss(token: TokenSpan): String {
        val trap = token.falseCognateEntry
        if (trap != null) {
            return simplifyGloss(trap.actualEnglish)
        }

        val clitic = token.cliticBreakdown
        if (clitic != null) {
            return simplifyGloss(clitic.combinedEnglishGloss)
        }

        val dict = token.dictionaryEntry
        if (dict != null) {
            return simplifyGloss(dict.englishGloss)
        }

        return ""
    }

    private fun simplifyGloss(gloss: String): String {
        val firstTerm = gloss.split(";", "/", ",").firstOrNull()?.trim().orEmpty()

        return if (firstTerm.length > MAX_GLOSS_LENGTH) {
            firstTerm.take(MAX_GLOSS_LENGTH).trimEnd()
        } else {
            firstTerm
        }
    }
}
