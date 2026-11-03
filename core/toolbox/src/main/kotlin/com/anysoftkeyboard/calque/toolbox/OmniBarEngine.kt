package com.anysoftkeyboard.calque.toolbox

import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import java.util.Locale

/**
 * Domain engine powering the Toolbox Smart Omni-Bar.
 *
 * Routes user queries to Numbers/Times, Cognate Suffix Rules, False Cognates, Clitic Surgery, or
 * Dictionary Lookups.
 */
class OmniBarEngine(
    private val cognateRulesRepository: CognateRulesRepository,
    private val falseCognatesRepository: FalseCognatesRepository,
    private val wordLookupProvider: WordLookupProvider,
) {

    private companion object {
        private const val MAX_VALID_HOUR = 23
        private const val MAX_VALID_MINUTE = 59
        private const val MAX_SUPPORTED_NUMBER = 999_999L
        private const val MINUTES_PER_HOUR = 60
    }

    /** Parses and routes [query] to the most appropriate [OmniBarResult]. */
    fun route(query: String): OmniBarResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return OmniBarResult.None(trimmed)

        return findCandidateResult(trimmed) ?: OmniBarResult.None(trimmed)
    }

    private fun findCandidateResult(input: String): OmniBarResult? {
        val lower = input.lowercase(Locale.ROOT)
        return tryRouteNumberOrTime(input)
            ?: tryRouteSuffixRule(input)
            ?: tryRouteFalseCognate(input, lower)
            ?: tryRouteClitic(input, lower)
            ?: tryRouteWord(input, lower)
    }

    private fun tryRouteFalseCognate(input: String, lower: String): OmniBarResult? {
        val trapCard = falseCognatesRepository.getBySurfaceForm(lower) ?: return null
        return OmniBarResult.FalseCognateResult(input, trapCard)
    }

    private fun tryRouteClitic(input: String, lower: String): OmniBarResult? {
        val surgery = CliticSurgeryEngine.performSurgery(lower, wordLookupProvider) ?: return null
        return OmniBarResult.CliticResult(input, surgery)
    }

    private fun tryRouteWord(input: String, lower: String): OmniBarResult? {
        val word = wordLookupProvider.lookupSingleWord(lower) ?: return null
        return OmniBarResult.WordLookupResult(input, word)
    }

    private fun tryRouteNumberOrTime(input: String): OmniBarResult? {
        val clockResult = tryRouteClockTime(input)
        if (clockResult != null) return clockResult

        val parsedLong = input.toLongOrNull()
        if (parsedLong != null && parsedLong in 0L..MAX_SUPPORTED_NUMBER) {
            val agreement = NumberOdometerEngine.formatNumberWithAgreement(parsedLong)
            return OmniBarResult.NumberResult(
                input = input,
                number = parsedLong,
                masculineWords = agreement.masculineText,
                feminineWords = agreement.feminineText,
            )
        }

        return null
    }

    private fun tryRouteClockTime(input: String): OmniBarResult? {
        val timeParts = input.split(":")
        if (timeParts.size != 2) return null

        val h = timeParts[0].toIntOrNull()
        val m = timeParts[1].toIntOrNull()
        if (!isValidTime(h, m)) return null

        val timeResult = NumberOdometerEngine.formatClockTime(h!!, m!!)
        return OmniBarResult.NumberResult(
            input = input,
            number = (h * MINUTES_PER_HOUR + m).toLong(),
            masculineWords = timeResult.phrase,
            feminineWords = timeResult.phrase,
        )
    }

    private fun isValidTime(h: Int?, m: Int?): Boolean {
        if (h == null || m == null) return false
        return h in 0..MAX_VALID_HOUR && m in 0..MAX_VALID_MINUTE
    }

    private fun tryRouteSuffixRule(input: String): OmniBarResult? {
        val cleanSuffix = if (input.startsWith("-")) input else "-$input"
        val allRules = cognateRulesRepository.getAll()

        val matchingRule =
            allRules.firstOrNull { rule ->
                rule.englishSuffix.equals(cleanSuffix, ignoreCase = true) ||
                    rule.spanishSuffix.equals(cleanSuffix, ignoreCase = true)
            }

        return if (matchingRule != null) {
            OmniBarResult.SuffixRuleResult(input, matchingRule)
        } else {
            null
        }
    }
}
