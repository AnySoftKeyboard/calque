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

## Building `seed.db`

```
bazelisk run //tools/preprocessor:preprocessor -- --db /tmp/seed.db \
    --dictionary-seed data/seeds/dictionary_seed.json \
    --false-cognates data/seeds/false_cognates_seed.json \
    --curated-lessons data/seeds/curated_lessons_seed.json \
    --cognate-rules data/seeds/cognate_rules_seed.json
```

Run without arguments to print every option.

## License notes

- The code in this repository is Apache-2.0. The data is not: the generated `seed.db` and the
  checked-in subsets are derived from CC BY-SA 4.0 (Kaikki), CC BY 2.0 FR (Tatoeba) and
  CC BY-NC-SA 4.0 (SUBTLEX-ESP) sources and keep those terms. See [NOTICE](../../NOTICE).
- SUBTLEX-ESP is licensed **non-commercial**. Before distributing an app build that contains
  SUBTLEX-derived ranks, confirm that this is acceptable for the distribution channel, or replace
  the frequency source.
