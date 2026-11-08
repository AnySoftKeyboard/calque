package com.anysoftkeyboard.calque.preprocessor

import java.io.BufferedReader
import java.io.InputStream
import java.text.Normalizer
import java.util.Locale

/**
 * Reduces the Kaikki Spanish dump (~1.1 GB) to the entries the app needs, see
 * `tools/preprocessor/README.md` and `docs/TODO.md` section 1.1.
 * - Lemma entries are kept. Entries whose senses are all `form_of` / `alt_of` (such as `pies` =
 *   "plural of pie") are dropped, because their lemma lists the same forms.
 * - Only parts of speech that occur in running text are kept; multi-word entries are dropped.
 * - Per lemma only the word, the part of speech, the first real gloss and the usable forms are
 *   kept. Forms that are conjugation-table metadata, clitic-combined (handled at runtime by
 *   `CliticDeconstructor`), multi-word, or equal to the lemma are dropped.
 * - A lemma is in scope when its word, or one of its kept forms, is in `wantedWords`, or when it is
 *   the target of an alias.
 * - Several lemma entries with the same word and part of speech (one per etymology) are merged: the
 *   first gloss wins and the forms are united.
 * - A dropped form-of entry for an in-scope word that no kept lemma lists (`tu` = "apocopic form of
 *   tuyo", `dame` = "imperative of dar combined with me") is kept as an *alias* that names its
 *   target lemma, so these very frequent words still resolve.
 *
 * Aliases need two passes over the dump, hence the `openInput` supplier. The line rules live in
 * [KaikkiEntryParser]; writing the result is done by [KaikkiSubsetFiles].
 */
object KaikkiSubsetExtractor {
    /** Reads the dump twice (via [openInput]) and returns the in-scope lemmas and aliases. */
    fun extract(openInput: () -> InputStream, wantedWords: Set<String>): KaikkiSubset {
        val candidates = openInput().use { collectAliasCandidates(it, wantedWords) }
        val collector = SubsetCollector(wantedWords, candidates)
        openInput().use { input ->
            input.bufferedReader(Charsets.UTF_8).forEachNonBlankLine {
                collector.accept(KaikkiEntryParser.parseLine(it))
            }
        }
        return collector.result()
    }

    /** Lowercases and NFC-normalizes like the SUBTLEX rank file, so words compare equal. */
    fun normalize(word: String): String =
        Normalizer.normalize(word, Normalizer.Form.NFC).lowercase(Locale.ROOT)

    private fun collectAliasCandidates(
        input: InputStream,
        wantedWords: Set<String>,
    ): List<SubsetEntry> {
        val candidates = mutableListOf<SubsetEntry>()
        input.bufferedReader(Charsets.UTF_8).forEachNonBlankLine { line ->
            val alias = (KaikkiEntryParser.parseLine(line) as? KaikkiLine.FormOf)?.alias
            if (alias != null && normalize(alias.word) in wantedWords) candidates.add(alias)
        }
        return candidates
    }

    private fun BufferedReader.forEachNonBlankLine(action: (String) -> Unit) {
        for (rawLine in lineSequence()) {
            val line = rawLine.trim()
            if (line.isNotEmpty()) action(line)
        }
    }

    /** Accumulates the classified lines: scope filtering, duplicate merging and the counters. */
    private class SubsetCollector(
        private val wantedWords: Set<String>,
        private val aliasCandidates: List<SubsetEntry>,
    ) {
        private val aliasTargets =
            aliasCandidates.mapTo(HashSet()) { normalize(it.formOf.orEmpty()) }
        private val merged = LinkedHashMap<Pair<String, String>, SubsetEntry>()
        private var lines = 0L
        private var malformed = 0L
        private var formOf = 0L
        private var excluded = 0L
        private var outOfScope = 0L
        private var duplicates = 0L

        fun accept(line: KaikkiLine) {
            lines++
            when (line) {
                KaikkiLine.Malformed -> malformed++
                is KaikkiLine.FormOf -> formOf++
                KaikkiLine.Excluded -> excluded++
                is KaikkiLine.Lemma -> acceptLemma(line.entry)
            }
        }

        private fun acceptLemma(entry: SubsetEntry) {
            if (!isInScope(entry)) {
                outOfScope++
                return
            }
            val key = entry.word to entry.pos
            val existing = merged[key]
            if (existing != null) duplicates++
            merged[key] = existing?.let { mergeForms(it, entry) } ?: entry
        }

        private fun isInScope(entry: SubsetEntry): Boolean {
            val word = normalize(entry.word)
            return word in wantedWords ||
                word in aliasTargets ||
                entry.forms.any { normalize(it.form) in wantedWords }
        }

        private fun mergeForms(first: SubsetEntry, other: SubsetEntry): SubsetEntry =
            first.copy(forms = (first.forms + other.forms).distinct())

        fun result(): KaikkiSubset {
            val lemmas = merged.values.toList()
            val aliases = uncoveredAliases(lemmas)
            val all =
                (lemmas + aliases).sortedWith(
                    compareBy({ it.word }, { it.pos }, { it.formOf.orEmpty() }),
                )
            val stats =
                KaikkiSubsetStats(
                    linesRead = lines,
                    malformedLines = malformed,
                    formOfEntries = formOf,
                    excludedEntries = excluded,
                    outOfScopeEntries = outOfScope,
                    duplicateEntries = duplicates,
                    entries = lemmas.size,
                    aliases = aliases.size,
                    forms = lemmas.sumOf { it.forms.size },
                )
            return KaikkiSubset(all, stats)
        }

        /** Alias candidates whose word is neither a kept lemma nor listed as a form of one. */
        private fun uncoveredAliases(lemmas: List<SubsetEntry>): List<SubsetEntry> {
            val covered = HashSet<String>()
            for (lemma in lemmas) {
                covered.add(normalize(lemma.word))
                lemma.forms.mapTo(covered) { normalize(it.form) }
            }
            return aliasCandidates
                .filter { normalize(it.word) !in covered }
                .distinctBy { Triple(it.word, it.pos, it.formOf) }
        }
    }
}
