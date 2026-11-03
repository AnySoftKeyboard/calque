package com.anysoftkeyboard.calque.reader

import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.TokenSpan
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import com.anysoftkeyboard.calque.toolbox.CliticSurgeryEngine

/**
 * Builds comprehensive [TokenInspection] cards for tapped tokens and manages user encounter
 * logging.
 */
object TokenInspector {

    /** Inspects [tokenSpan] and records the user encounter if a vault repository is supplied. */
    fun inspectToken(
        tokenSpan: TokenSpan,
        lookupProvider: WordLookupProvider,
        cognateRulesRepository: CognateRulesRepository? = null,
        userWordStateRepository: UserWordStateRepository? = null,
    ): TokenInspection? {
        if (!tokenSpan.isWord) return null

        val lemma = tokenSpan.dictionaryEntry?.lemma ?: tokenSpan.normalizedWord.orEmpty()
        val userWordState = userWordStateRepository?.getWordState(lemma)

        if (userWordStateRepository != null && lemma.isNotEmpty()) {
            userWordStateRepository.recordEncounter(lemma)
        }

        val cognateRuleNote =
            cognateRulesRepository
                ?.findRuleForSpanishWord(tokenSpan.normalizedWord.orEmpty())
                ?.let { "${it.englishSuffix} \u2192 ${it.spanishSuffix}: ${it.spellingNote}" }

        val cliticSurgery =
            if (tokenSpan.cliticBreakdown != null) {
                CliticSurgeryEngine.performSurgery(tokenSpan.text, lookupProvider)
            } else {
                null
            }

        return TokenInspection(
            tokenSpan = tokenSpan,
            lemma = lemma,
            pos = tokenSpan.dictionaryEntry?.pos,
            englishGloss = tokenSpan.dictionaryEntry?.englishGloss,
            cognateRuleNote = cognateRuleNote,
            falseCognateTrap = tokenSpan.falseCognateEntry,
            cliticSurgery = cliticSurgery,
            userStatus = userWordState?.status ?: tokenSpan.userStatus,
            isStarred = userWordState?.starred ?: false,
        )
    }
}
