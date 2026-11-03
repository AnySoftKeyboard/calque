package com.anysoftkeyboard.calque.data

import com.anysoftkeyboard.calque.data.seed.DictionaryRepository
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.data.user.batchGetWordStatuses
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider

/**
 * Composite [WordLookupProvider] implementation bridging [DictionaryRepository],
 * [FalseCognatesRepository], and [UserWordStateRepository] for reader enrichment and linguistic
 * lookups.
 */
class CalqueWordLookupProvider(
    private val dictionaryRepository: DictionaryRepository,
    private val falseCognatesRepository: FalseCognatesRepository,
    private val userWordStateRepository: UserWordStateRepository,
) : WordLookupProvider {

    override fun batchLookupDictionary(words: Set<String>): Map<String, DictionaryEntry> {
        return dictionaryRepository.getBySurfaceForms(words)
    }

    override fun batchLookupFalseCognates(words: Set<String>): Map<String, FalseCognateEntry> {
        return falseCognatesRepository.getBySurfaceForms(words)
    }

    override fun batchLookupUserState(lemmas: Set<String>): Map<String, UserWordStatus> {
        return userWordStateRepository.batchGetWordStatuses(lemmas)
    }

    override fun lookupSingleWord(word: String): DictionaryEntry? {
        return dictionaryRepository.getBySurfaceForm(word)
    }
}
