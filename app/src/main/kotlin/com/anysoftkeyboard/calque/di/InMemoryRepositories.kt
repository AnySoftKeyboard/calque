package com.anysoftkeyboard.calque.di

import com.anysoftkeyboard.calque.data.models.CognateRule
import com.anysoftkeyboard.calque.data.models.CuratedLesson
import com.anysoftkeyboard.calque.data.models.UserCustomText
import com.anysoftkeyboard.calque.data.models.UserWordState
import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.seed.CuratedLessonsRepository
import com.anysoftkeyboard.calque.data.seed.DictionaryRepository
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.data.user.UserCustomTextRepository
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.DictionaryEntry
import com.anysoftkeyboard.calque.linguistics.FalseCognateEntry
import com.anysoftkeyboard.calque.linguistics.UserWordStatus
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** In-memory fallback implementation of [DictionaryRepository]. */
class InMemoryDictionaryRepository(
    initialEntries: List<DictionaryEntry> = emptyList(),
) : DictionaryRepository {
    private val entriesBySurface =
        initialEntries.associateBy { it.surfaceForm.lowercase(Locale.ROOT) }

    override fun getBySurfaceForm(surfaceForm: String): DictionaryEntry? =
        entriesBySurface[surfaceForm.lowercase(Locale.ROOT).trim()]

    override fun getBySurfaceForms(surfaceForms: Collection<String>): Map<String, DictionaryEntry> =
        surfaceForms
            .mapNotNull { form ->
                val normalized = form.lowercase(Locale.ROOT).trim()
                entriesBySurface[normalized]?.let { normalized to it }
            }
            .toMap()

    override fun getSurfacesForLemma(lemma: String): List<String> =
        entriesBySurface.values
            .filter { it.lemma.equals(lemma, ignoreCase = true) }
            .map { it.surfaceForm }

    override fun getEntriesForLemma(lemma: String): List<DictionaryEntry> =
        entriesBySurface.values.filter { it.lemma.equals(lemma, ignoreCase = true) }

    override fun searchByPrefix(prefix: String, limit: Int): List<DictionaryEntry> {
        val normalized = prefix.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty() || limit <= 0) return emptyList()
        return entriesBySurface.values
            .filter { it.surfaceForm.lowercase(Locale.ROOT).startsWith(normalized) }
            .take(limit)
    }
}

/** In-memory fallback implementation of [FalseCognatesRepository]. */
class InMemoryFalseCognatesRepository(
    initialEntries: List<FalseCognateEntry> = emptyList(),
) : FalseCognatesRepository {
    private val entriesBySurface =
        initialEntries.associateBy { it.surfaceForm.lowercase(Locale.ROOT) }

    override fun getBySurfaceForm(surfaceForm: String): FalseCognateEntry? =
        entriesBySurface[surfaceForm.lowercase(Locale.ROOT).trim()]

    override fun getBySurfaceForms(
        surfaceForms: Collection<String>
    ): Map<String, FalseCognateEntry> =
        surfaceForms
            .mapNotNull { form ->
                val normalized = form.lowercase(Locale.ROOT).trim()
                entriesBySurface[normalized]?.let { normalized to it }
            }
            .toMap()

    override fun getByLemma(lemma: String): List<FalseCognateEntry> =
        entriesBySurface.values.filter { it.lemma.equals(lemma, ignoreCase = true) }

    override fun getAll(): List<FalseCognateEntry> = entriesBySurface.values.toList()
}

/** In-memory fallback implementation of [CognateRulesRepository]. */
class InMemoryCognateRulesRepository(
    initialRules: List<CognateRule> = emptyList(),
) : CognateRulesRepository {
    private val rulesById = initialRules.associateBy { it.ruleId }

    override fun getAll(): List<CognateRule> = rulesById.values.toList()

    override fun getById(ruleId: String): CognateRule? = rulesById[ruleId.trim()]

    override fun findRuleForSpanishWord(spanishWord: String): CognateRule? {
        val normalized = spanishWord.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return null
        return rulesById.values
            .filter { rule ->
                val cleanSuffix = rule.spanishSuffix.lowercase(Locale.ROOT).trim().removePrefix("-")
                cleanSuffix.isNotEmpty() && normalized.endsWith(cleanSuffix)
            }
            .maxByOrNull { it.spanishSuffix.removePrefix("-").length }
    }
}

/** In-memory fallback implementation of [CuratedLessonsRepository]. */
class InMemoryCuratedLessonsRepository(
    initialLessons: List<CuratedLesson> = emptyList(),
) : CuratedLessonsRepository {
    private val lessons = initialLessons.sortedWith(compareBy({ it.level }, { it.sequenceOrder }))

    override fun getAll(): List<CuratedLesson> = lessons

    override fun getByLevel(level: Int): List<CuratedLesson> = lessons.filter { it.level == level }

    override fun getById(lessonId: String): CuratedLesson? =
        lessons.firstOrNull { it.lessonId == lessonId.trim() }
}

/** In-memory fallback implementation of [UserWordStateRepository]. */
class InMemoryUserWordStateRepository : UserWordStateRepository {
    private val store = ConcurrentHashMap<String, UserWordState>()

    override fun getWordState(lemma: String): UserWordState? =
        store[lemma.lowercase(Locale.ROOT).trim()]

    override fun batchGetWordStates(lemmas: Collection<String>): Map<String, UserWordState> =
        lemmas
            .mapNotNull { raw ->
                val normalized = raw.lowercase(Locale.ROOT).trim()
                store[normalized]?.let { normalized to it }
            }
            .toMap()

    override fun upsertWordState(state: UserWordState) {
        val normalized = state.lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isNotEmpty()) store[normalized] = state
    }

    override fun recordEncounter(lemma: String, timestamp: Long) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        val current = store[normalized]
        val updated =
            current?.copy(
                encounterCount = current.encounterCount + 1,
                lastSeenTimestamp = timestamp,
            )
                ?: UserWordState(
                    lemma = normalized,
                    status = UserWordStatus.LEARNING,
                    starred = false,
                    savedContextSentence = null,
                    encounterCount = 1,
                    lastSeenTimestamp = timestamp,
                )
        store[normalized] = updated
    }

    override fun setWordStatus(lemma: String, status: UserWordStatus, timestamp: Long) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        val current = store[normalized]
        val updated =
            current?.copy(status = status, lastSeenTimestamp = timestamp)
                ?: UserWordState(
                    lemma = normalized,
                    status = status,
                    starred = false,
                    savedContextSentence = null,
                    encounterCount = 1,
                    lastSeenTimestamp = timestamp,
                )
        store[normalized] = updated
    }

    override fun setStarred(
        lemma: String,
        starred: Boolean,
        contextSentence: String?,
        timestamp: Long,
    ) {
        val normalized = lemma.lowercase(Locale.ROOT).trim()
        if (normalized.isEmpty()) return
        val current = store[normalized]
        val updated =
            current?.copy(
                starred = starred,
                savedContextSentence = contextSentence ?: current.savedContextSentence,
                lastSeenTimestamp = timestamp,
            )
                ?: UserWordState(
                    lemma = normalized,
                    status = UserWordStatus.LEARNING,
                    starred = starred,
                    savedContextSentence = contextSentence,
                    encounterCount = 1,
                    lastSeenTimestamp = timestamp,
                )
        store[normalized] = updated
    }

    override fun getStarredWords(): List<UserWordState> =
        store.values.filter { it.starred }.sortedByDescending { it.lastSeenTimestamp }

    override fun getWordsByStatus(status: UserWordStatus): List<UserWordState> =
        store.values.filter { it.status == status }.sortedByDescending { it.lastSeenTimestamp }

    override fun deleteWordState(lemma: String): Boolean =
        store.remove(lemma.lowercase(Locale.ROOT).trim()) != null
}

/** In-memory fallback implementation of [UserCustomTextRepository]. */
class InMemoryUserCustomTextRepository : UserCustomTextRepository {
    private val store = ConcurrentHashMap<Long, UserCustomText>()
    private val nextId = AtomicLong(1L)

    override fun insert(text: UserCustomText): Long {
        val id = nextId.getAndIncrement()
        val saved = text.copy(textId = id)
        store[id] = saved
        return id
    }

    override fun update(text: UserCustomText): Boolean {
        if (text.textId <= 0L || !store.containsKey(text.textId)) return false
        store[text.textId] = text
        return true
    }

    override fun getById(textId: Long): UserCustomText? = store[textId]

    override fun getAll(): List<UserCustomText> =
        store.values.sortedByDescending { it.createdTimestamp }

    override fun delete(textId: Long): Boolean = store.remove(textId) != null
}
