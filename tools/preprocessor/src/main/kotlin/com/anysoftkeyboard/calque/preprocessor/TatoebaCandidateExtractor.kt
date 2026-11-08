package com.anysoftkeyboard.calque.preprocessor

import com.anysoftkeyboard.calque.linguistics.SentenceScorer
import java.io.InputStream
import java.text.Normalizer
import java.util.EnumMap
import java.util.Locale

/** A Spanish sentence with the English translation Tatoeba links to it. */
data class TatoebaPair(
    val spanishId: Long,
    val englishId: Long,
    val spanish: String,
    val english: String,
)

/** Why a Spanish sentence was not taken as a lesson candidate. */
enum class TatoebaRejection {
    MALFORMED_LINE,
    NO_TRANSLATION,
    TOO_LONG,
    MULTIPLE_SENTENCES,
    WORD_COUNT,
    UNSUPPORTED_TOKEN,
    PROPER_NAME,
    OUT_OF_SCOPE_WORD,
    DUPLICATE,
    MISSING_ENGLISH_TEXT,
}

/** What happened to the Spanish sentences, and how many pairs were available and selected. */
data class TatoebaStats(
    val spanishLinesRead: Long,
    val rejections: Map<TatoebaRejection, Long>,
    val candidates: Int,
    val selected: Int,
)

/** The selected pairs, sorted by Spanish sentence id, with the statistics. */
data class TatoebaCandidates(val pairs: List<TatoebaPair>, val stats: TatoebaStats)

/**
 * Reduces the Tatoeba exports (hundreds of thousands of sentences) to a small, deterministic pool
 * of single-sentence candidates for the reading lessons; scoring and bucketing into levels is done
 * later at build time.
 *
 * A Spanish sentence is a candidate when it
 * - has an English translation (the lowest English id when there are several),
 * - is one sentence of at most [MAX_CHARACTERS] characters with [SentenceScorer.MIN_WORD_COUNT] to
 *   [SentenceScorer.MAX_WORD_COUNT] words (counted like the scorer counts them),
 * - consists only of plain words (no digits, hyphens or apostrophes),
 * - contains no capitalized word after the first (proper names and acronyms), and
 * - uses only words in the in-scope list (the SUBTLEX top-N), so every word resolves in the
 *   dictionary subset built from the same list.
 *
 * Identical sentences are kept once (lowest id). When more than `maxPairs` candidates remain, a
 * deterministic hash of the sentence id picks which to keep, which is unbiased with respect to
 * length and difficulty.
 */
object TatoebaCandidateExtractor {
    const val DEFAULT_MAX_PAIRS = 20_000
    const val MAX_CHARACTERS = 200

    private const val LINK_COLUMNS = 2
    private const val SENTENCE_COLUMNS = 3
    private const val SPANISH_LANGUAGE = "spa"
    private const val INTERNAL_BREAKS = ".!?\u2026;:"
    private val LETTERS_ONLY = Regex("""\p{L}+""")
    private val TRAILING_PUNCTUATION = Regex("""[\s.!?\u2026"'\u00bb\u201d)]+$""")

    /** Reads `spanishId<TAB>englishId` links, keeping the lowest English id per Spanish id. */
    fun readLinks(input: InputStream): Map<Long, Long> {
        val links = HashMap<Long, Long>()
        input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
            val parts = line.split('\t')
            val spanishId = parts.getOrNull(0)?.trim()?.toLongOrNull()
            val englishId = parts.getOrNull(1)?.trim()?.toLongOrNull()
            if (parts.size >= LINK_COLUMNS && spanishId != null && englishId != null) {
                links.merge(spanishId, englishId) { old, new -> minOf(old, new) }
            }
        }
        return links
    }

    /**
     * Selects the candidates. [openSpanish] and [openEnglish] return `id<TAB>lang<TAB>text` files;
     * [links] comes from [readLinks]; [wantedWords] are the in-scope words (see
     * [KaikkiSubsetExtractor.normalize]).
     */
    fun extract(
        openSpanish: () -> InputStream,
        openEnglish: () -> InputStream,
        links: Map<Long, Long>,
        wantedWords: Set<String>,
        maxPairs: Int = DEFAULT_MAX_PAIRS,
    ): TatoebaCandidates {
        require(maxPairs > 0) { "maxPairs must be positive but was $maxPairs" }
        val pass = SpanishPass(links, wantedWords)
        openSpanish().use { pass.read(it) }
        val english = openEnglish().use { readEnglishTexts(it, pass.englishIds()) }
        val resolved = pass.resolve(english)
        val selected = sample(resolved, maxPairs)
        val stats = TatoebaStats(pass.linesRead, pass.rejections(), resolved.size, selected.size)
        return TatoebaCandidates(selected, stats)
    }

    /**
     * Returns why [text] is not usable as a lesson sentence, or null when it is. Checks run from
     * the cheapest to the most specific, and the first failing check wins.
     */
    fun rejectionFor(text: String, wantedWords: Set<String>): TatoebaRejection? {
        val words = SentenceScorer.tokenizeWords(text)
        return when {
            text.length > MAX_CHARACTERS -> TatoebaRejection.TOO_LONG
            isMultipleSentences(text) -> TatoebaRejection.MULTIPLE_SENTENCES
            words.size !in SentenceScorer.MIN_WORD_COUNT..SentenceScorer.MAX_WORD_COUNT ->
                TatoebaRejection.WORD_COUNT
            text.any { it.isDigit() } || words.any { !LETTERS_ONLY.matches(it) } ->
                TatoebaRejection.UNSUPPORTED_TOKEN
            hasCapitalizedLaterWord(text) -> TatoebaRejection.PROPER_NAME
            words.any { KaikkiSubsetExtractor.normalize(it) !in wantedWords } ->
                TatoebaRejection.OUT_OF_SCOPE_WORD
            else -> null
        }
    }

    private fun isMultipleSentences(text: String): Boolean =
        text.trim().replace(TRAILING_PUNCTUATION, "").any { it in INTERNAL_BREAKS }

    private fun hasCapitalizedLaterWord(text: String): Boolean =
        LETTERS_ONLY.findAll(text).drop(1).any { it.value.first().isUpperCase() }

    /** Dedupe key: same sentence up to case, Unicode form and spacing. */
    private fun duplicateKey(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .replace(Regex("""\s+"""), " ")
            .trim()

    private class SpanishSentence(val id: Long, val englishId: Long, val text: String)

    /** First pass: classifies the Spanish file line by line and remembers the accepted ones. */
    private class SpanishPass(
        private val links: Map<Long, Long>,
        private val wantedWords: Set<String>,
    ) {
        var linesRead = 0L
            private set

        private val rejected = EnumMap<TatoebaRejection, Long>(TatoebaRejection::class.java)
        private val accepted = HashMap<String, SpanishSentence>()

        fun read(input: InputStream) {
            input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
                if (line.isNotBlank()) {
                    linesRead++
                    accept(line)
                }
            }
        }

        private fun accept(line: String) {
            val parts = line.split('\t')
            val id = parts.getOrNull(0)?.trim()?.toLongOrNull()
            val text = parts.getOrNull(2)?.trim().orEmpty()
            val englishId = id?.let { links[it] }
            val isSpanish = parts.getOrNull(1) == SPANISH_LANGUAGE
            val rejection =
                when {
                    id == null || parts.size < SENTENCE_COLUMNS || !isSpanish || text.isEmpty() ->
                        TatoebaRejection.MALFORMED_LINE
                    englishId == null -> TatoebaRejection.NO_TRANSLATION
                    else -> rejectionFor(text, wantedWords)
                }
            if (rejection != null) {
                reject(rejection)
            } else if (id != null && englishId != null) {
                keepLowestId(SpanishSentence(id, englishId, text))
            }
        }

        private fun keepLowestId(sentence: SpanishSentence) {
            val key = duplicateKey(sentence.text)
            val existing = accepted[key]
            if (existing != null) reject(TatoebaRejection.DUPLICATE)
            if (existing == null || sentence.id < existing.id) accepted[key] = sentence
        }

        private fun reject(reason: TatoebaRejection) {
            rejected.merge(reason, 1L, Long::plus)
        }

        fun englishIds(): Set<Long> = accepted.values.mapTo(HashSet()) { it.englishId }

        fun rejections(): Map<TatoebaRejection, Long> = rejected.toMap()

        /** Pairs each accepted sentence with its English text; those without one are dropped. */
        fun resolve(english: Map<Long, String>): List<TatoebaPair> =
            accepted.values
                .sortedBy { it.id }
                .mapNotNull { sentence ->
                    val text = english[sentence.englishId]
                    if (text == null) reject(TatoebaRejection.MISSING_ENGLISH_TEXT)
                    text?.let { TatoebaPair(sentence.id, sentence.englishId, sentence.text, it) }
                }
    }

    /** Second pass: the English text of the linked sentences only. */
    private fun readEnglishTexts(input: InputStream, ids: Set<Long>): Map<Long, String> {
        val texts = HashMap<Long, String>()
        input.bufferedReader(Charsets.UTF_8).forEachLine { line ->
            val parts = line.split('\t')
            val id = parts.getOrNull(0)?.trim()?.toLongOrNull()
            val text = parts.getOrNull(2)?.trim().orEmpty()
            if (id != null && id in ids && isUsableEnglish(text)) texts.putIfAbsent(id, text)
        }
        return texts
    }

    private fun isUsableEnglish(text: String): Boolean =
        text.isNotEmpty() && text.length <= MAX_CHARACTERS

    /** Keeps [maxPairs] pairs chosen by a hash of the sentence id, returned in id order. */
    private fun sample(pairs: List<TatoebaPair>, maxPairs: Int): List<TatoebaPair> =
        if (pairs.size <= maxPairs) {
            pairs
        } else {
            pairs.sortedBy { mix(it.spanishId) }.take(maxPairs).sortedBy { it.spanishId }
        }

    /** SplitMix64 finalizer: a fixed, well-distributed hash so the sample never changes. */
    private fun mix(id: Long): Long {
        var z = id + GOLDEN_GAMMA
        z = (z xor (z ushr SHIFT_1)) * MULTIPLIER_1
        z = (z xor (z ushr SHIFT_2)) * MULTIPLIER_2
        return z xor (z ushr SHIFT_3)
    }

    private const val GOLDEN_GAMMA = -0x61c8864680b583ebL
    private const val MULTIPLIER_1 = -0x40a7b892e31b1a47L
    private const val MULTIPLIER_2 = -0x6b2fb644ecceee15L
    private const val SHIFT_1 = 30
    private const val SHIFT_2 = 27
    private const val SHIFT_3 = 31
}
