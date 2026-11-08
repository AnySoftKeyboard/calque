# Calque preprocessor

JVM command line tool that builds the read-only `seed.db` shipped with the app (see
[docs/INGESTION.md](../../docs/INGESTION.md), Pipeline 1, and [docs/DATA.md](../../docs/DATA.md)).

The build never downloads anything. The large raw corpora are reduced **once, by hand,** to small
subsets that are checked in; `seed.db` is generated from those subsets and the curated seeds in
`data/seeds/`.

```
raw corpora (not committed)          extractors (manual)        checked-in subsets      build
Kaikki Spanish dump (~1.1 GB)   -->  extract-kaikki        -->  data/kaikki/...    -->
SUBTLEX-ESP.xlsx (3.7 MB)       -->  extract-sublex        -->  data/subtlex/...   -->  seed.db
Tatoeba exports                 -->  extract-tatoeba       -->  data/tatoeba/...   -->
```

## Inputs

### SUBTLEX-ESP (word frequencies)

- **What:** Spanish word frequencies from film subtitles (Cuetos, Gonzalez-Nacher and Perea, 2011).
  Used for `dictionary.frequency_rank` and for the "rare word" part of sentence scoring, and to
  decide which dictionary lemmas are kept.
- **Download:** `SUBTLEX-ESP.xlsx` from <https://osf.io/xp6sz/> (direct file:
  <https://osf.io/download/fxt57/>).
- **License:** CC BY-NC-SA 4.0 (see `license.txt` on the OSF page). See "License notes" below.
- **Citation:** Cuetos, F., Glez-Nosti, M., Barbón, A. and Brysbaert, M. (2011). SUBTLEX-ESP:
  Spanish word frequencies based on film subtitles. *Psicológica*, 32, 133-143.

### Kaikki (dictionary)

- **What:** Machine-readable Spanish Wiktionary (English edition) extracted by Wiktextract.
- **Download:** <https://kaikki.org/dictionary/Spanish/kaikki.org-dictionary-Spanish.jsonl>
  (about 1.1 GB; it is regenerated regularly and older versions are overwritten).
- **License:** CC BY-SA 4.0 (Wiktionary) with the Wiktextract attribution below.
- **Citation:** Tatu Ylonen, *Wiktextract: Wiktionary as Machine-Readable Structured Data*,
  Proceedings of LREC 2022, pp. 1317-1325.

### Tatoeba (sentence pairs)

- **What:** Human-written Spanish sentences with English translations.
- **Download** (from <https://tatoeba.org/en/downloads>, all `.bz2`; decompress with `bzip2 -dk`):
  - `https://downloads.tatoeba.org/exports/per_language/spa/spa_sentences.tsv.bz2`
  - `https://downloads.tatoeba.org/exports/per_language/eng/eng_sentences.tsv.bz2`
  - `https://downloads.tatoeba.org/exports/per_language/spa/spa-eng_links.tsv.bz2`
- **License:** CC BY 2.0 FR. Keep the sentence ids with every sentence so it can be attributed.

## Running the extractors

Run from the repository root. Relative paths are resolved against the directory you ran the command
from. Raw downloads can live anywhere outside the repository, for example `/tmp`.

### `extract-sublex`

Reads the official workbook and writes the `N` most frequent words as `word<TAB>rank<TAB>count`
(rank 1 = most frequent). Words are lowercased and NFC-normalized, case variants are merged, and
tokens that are not purely letters are dropped. Ties are broken alphabetically, so the output is
deterministic.

```
bazelisk run //tools/preprocessor:preprocessor -- extract-sublex \
    --input /tmp/SUBTLEX-ESP.xlsx \
    --output data/subtlex/subtlex_esp_top25000.tsv \
    --limit 25000
```

`--limit` defaults to 25000. The output is read by `SublexReader` (the `--sublex` build option) and
by the other extractors to decide which words are in scope.

### `extract-kaikki`

Streams the full Kaikki dump (plain or `.gz`) and writes the entries the app needs as a sorted,
deterministic gzip of JSON lines, plus a metadata file with the provenance and counts. An entry
is in scope when its word or one of its forms is in the rank file (the `extract-sublex` output).

```
bazelisk run //tools/preprocessor:preprocessor -- extract-kaikki \
    --input /tmp/kaikki.org-dictionary-Spanish.jsonl \
    --ranks data/subtlex/subtlex_esp_top25000.tsv \
    --extra-words data/kaikki/extra_words.txt \
    --output data/kaikki/spanish_lemmas.jsonl.gz \
    --source-last-modified "Fri, 30 Oct 2026 23:17:33 GMT" \
    --source-etag '"6ae5258d-42bcda85"'
```

Optional: `--extra-words <file>` (one word per line, `#` comments) adds words that must resolve
even when they are not in the rank file; `--metadata <path>` overrides the default
`<output>.meta.json`. Take `--source-last-modified` and `--source-etag` from the response headers of
the download (`curl -I`).

What is kept:

- **Lemma entries** with the parts of speech that occur in running text (verb, noun, adj, adv,
  pron, det, article, prep, conj, num, contraction, intj, name, particle), single words only.
  Per entry: `word`, `pos`, the first real gloss and the usable `forms` (form + tags). Kaikki's
  field names are kept, so `KaikkiProcessor` reads the output like a dump.
- **Dropped forms:** conjugation-table metadata (`es-conj`, `irregular`, ...), clitic-combined
  forms (`absolverme`; resolved at runtime by `CliticDeconstructor`), multi-word forms and forms
  equal to the lemma.
- **Dropped entries:** form-of entries (`pies` = "plural of pie") because the lemma lists the
  same forms; entries with several etymologies are merged (first gloss wins, forms united).
- **Aliases:** Kaikki lists some very frequent words only as variants of another word (`tu`,
  `muy`, `buen` = apocopic forms; `dame`, `dime` = imperative + clitic). An in-scope form-of
  word that no kept lemma lists is kept as an alias with a `form_of` target, Kaikki's gloss and
  tags, and its target lemma is pulled in. A target that is not a single word (the acronym
  `ONU`) is kept as a standalone entry with its own gloss.

On the 2026-10-30 dump with the top 25,000 SUBTLEX words and the seed words this reads 814,137
lines in about 13 seconds (two passes) and keeps 14,546 lemmas, 1,973 aliases and 197,526 forms:
17 MB of JSON, 1.2 MB gzipped. 98.6% of the 25,000 words resolve (99.8% of the top 1,000); what
is left is almost only proper names and abbreviations. The output is byte-identical across runs.

### `extract-seed-words`

Lists the words used by the curated seeds (every lesson sentence, every false-cognate lemma, real
Spanish word and example, and the dictionary seed) so `extract-kaikki --extra-words` can include
them even when they are not among the most frequent words. Without it, a false-cognate lemma such
as `constipado` would have no dictionary row.

```
bazelisk run //tools/preprocessor:preprocessor -- extract-seed-words \
    --curated-lessons data/seeds/curated_lessons_seed.json \
    --false-cognates data/seeds/false_cognates_seed.json \
    --dictionary-seed data/seeds/dictionary_seed.json \
    --output data/kaikki/extra_words.txt
```

On the current seeds it lists 308 words, of which only 10 are not already in the top 25,000.
Regenerate it whenever the seeds change.

### `extract-tatoeba`

Joins the three Tatoeba exports into a pool of single-sentence lesson candidates: Spanish sentence,
its English translation, and both Tatoeba ids (CC BY needs each sentence to stay attributable).
Decompress the `.bz2` downloads first (`bzip2 -dk`); `.gz` files are also accepted.

```
bazelisk run //tools/preprocessor:preprocessor -- extract-tatoeba \
    --spanish /tmp/spa_sentences.tsv \
    --english /tmp/eng_sentences.tsv \
    --links /tmp/spa-eng_links.tsv \
    --ranks data/subtlex/subtlex_esp_top25000.tsv \
    --output data/tatoeba/spa_eng_candidates.tsv.gz \
    --max-pairs 20000 \
    --source-last-modified "Sat, 07 Nov 2026 07:59:52 GMT"
```

The output is a deterministic gzip of `spanishId<TAB>englishId<TAB>spanish<TAB>english` lines
sorted by Spanish id, plus `<output>.meta.json` with the counts per rejection reason. A Spanish
sentence is kept when it:

- has an English translation (the lowest English id when there are several);
- is one sentence of at most 200 characters with 4 to 14 words, counted like
  `SentenceScorer` counts them;
- has only plain words (no digits, hyphens or apostrophes) and no capitalized word after the
  first, which removes proper names ("Tom", "Mary") and acronyms;
- uses only words from the rank file, so every word resolves in the Kaikki subset built from
  the same file;
- is not a repeat of an earlier sentence (same text up to case, Unicode form and spacing).

When more than `--max-pairs` candidates remain, a fixed hash of the sentence id picks which to
keep, so the sample is unbiased with respect to length and difficulty and never changes between
runs. Scoring and bucketing into levels happens later, at `seed.db` build time.

On the 2026-11-07 export with the top 25,000 SUBTLEX words: 443,166 Spanish sentences, 181,305
without a translation, 43,045 with an out-of-scope word, 41,797 outside 4-14 words, 26,560 with
a proper name, 140,741 candidates, 20,000 selected (1.7 MB, 750 KB gzipped).

## How the app gets `seed.db`

`seed.db` is generated, not stored in git. The Bazel target `//app:seed_db_asset` (macro `seed_db` in
[`seed_db.bzl`](seed_db.bzl)) runs this tool on files from the repository only, writes
`app/src/main/assets/databases/seed.db` in `bazel-bin`, and the APK, the Room schema tests and
`PackagedSeedAssetTest` all read that file:

```
bazelisk build //app:seed_db_asset      # just the database
bazelisk build //app:app                # the APK, which contains assets/databases/seed.db
```

The output is byte-for-byte reproducible, so a changed hash always means changed inputs. To add the
extracted subsets to the build, pass `kaikki`, `sublex` and `tatoeba` to the `seed_db` rule in
`app/BUILD.bazel`.

## Building `seed.db` by hand

```
bazelisk run //tools/preprocessor:preprocessor -- --db /tmp/seed.db \
    --dictionary-seed data/seeds/dictionary_seed.json \
    --false-cognates data/seeds/false_cognates_seed.json \
    --curated-lessons data/seeds/curated_lessons_seed.json \
    --cognate-rules data/seeds/cognate_rules_seed.json
```

Run without arguments to print every option.

To build from the extracted subsets instead of only the curated seeds, add `--kaikki` (the
`.jsonl.gz` subset, or a full dump), `--sublex` (the rank TSV) and `--tatoeba` (the candidate pool):

```
bazelisk run //tools/preprocessor:preprocessor -- --db /tmp/seed.db \
    --kaikki data/kaikki/spanish_lemmas.jsonl.gz \
    --sublex data/subtlex/subtlex_esp_top25000.tsv \
    --tatoeba data/tatoeba/spa_eng_candidates.tsv.gz \
    --dictionary-seed data/seeds/dictionary_seed.json \
    --false-cognates data/seeds/false_cognates_seed.json \
    --curated-lessons data/seeds/curated_lessons_seed.json \
    --cognate-rules data/seeds/cognate_rules_seed.json
```

Rules worth knowing:

- **Collisions:** `surface_form` is the primary key, so homographs (`fue` = *ser* / *ir*) compete
  for one row. A lemma row beats an inflected row, which beats a proper noun (a capitalized lemma
  such as the surname `Amigo`, which must never hide `amigo`); then the lower `frequency_rank`
  wins, then the first row seen stays. The curated dictionary seed is applied last and always
  replaces. Only one part of speech per spelling can be stored, see INGESTION.md.
- **Frequency:** a row's rank is the better of its lemma's rank and its own, see INGESTION.md.
- **Aliases** from the Kaikki subset (`tu`, `muy`, `dame`) become rows of their target lemma.
- Malformed or unwanted Kaikki lines are counted and skipped, never fatal.
- **Tatoeba lessons:** with `--tatoeba <pool.tsv.gz>` (the `extract-tatoeba` output) each sentence
  is scored with `SentenceScorer` against the dictionary built so far; sentences with a word the
  dictionary does not know are skipped. Per level, `--tatoeba-packs-per-level` (default 16) times
  `--tatoeba-pack-size` (default 8) sentences are picked by a fixed hash of the Tatoeba id,
  ordered from easiest to hardest and cut into lessons `tatoeba_l<level>_<nnn>` in the same
  `{"sentences":[...]}` format as the curated lessons, each sentence keeping its two Tatoeba ids.
  Their `sequence_order` continues after the curated lessons of the level.

With the 2026-10-30 subset and the top-25,000 ranks this takes about 1.5 seconds and produces
148,880 dictionary rows (213,958 processed before collisions were resolved) in a 20 MB file.

## License notes

- The code in this repository is Apache-2.0. The data is not: the generated `seed.db` and the
  checked-in subsets are derived from CC BY-SA 4.0 (Kaikki), CC BY 2.0 FR (Tatoeba) and
  CC BY-NC-SA 4.0 (SUBTLEX-ESP) sources and keep those terms. See [NOTICE](../../NOTICE).
- SUBTLEX-ESP is licensed **non-commercial**. Before distributing an app build that contains
  SUBTLEX-derived ranks, confirm that this is acceptable for the distribution channel, or replace
  the frequency source.
