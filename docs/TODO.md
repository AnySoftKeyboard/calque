# Calque TODO

Derived from the other documents in `docs/` (CODING, DATA, PAGES_AND_FLOWS, INGESTION, DESIGN)
compared with the state of the repo when this file was written.

Legend: `[x]` done (verified in the repo), `[ ]` open, `[~]` partially done (note says what is
missing). Every item must follow AGENTS.md: small scoped PRs, unit tests with each algorithm, and
the three gates (`//:format.check`, `build --config=lint //...`, `test //...`).

---

## 0. Already implemented

### Build, tooling, CI
- [x] Bazel (Bzlmod) build, 100% Kotlin, no Gradle.
- [x] Mabel dependency management with checked-in `maven_install.json`; hierarchical
      `@maven//group/artifact` labels.
- [x] ktfmt formatting (`//:format`, `//:format.check`) and detekt + buildifier lint aspect
      (`--config=lint`).
- [x] GitHub Actions CI: format check, lint, build, test (`.github/workflows/ci.yml`).
- [x] Hermetiq remote cache / BEP setup.
- [x] Compose compiler plugin and Room compiler plugin Bazel targets (`tools/compose`, `tools/room`).
- [x] Module split: `core/linguistics`, `core/data`, `core/reader`, `core/toolbox` (pure JVM, no
      `android.*`), `tools/preprocessor` (JVM CLI), `app` (Android).

### Linguistics engines (`core/linguistics`, with unit tests)
- [x] `NumberConverter` (gender-aware Spanish number words).
- [x] `CliticDeconstructor` (pronoun surgery with accent restoration).
- [x] `CognateDetector` (suffix map + Levenshtein rule).
- [x] `SentenceScorer` (BreakIterator es-ES tokenization, level bucketing).
- [x] `ReaderEnricher` and enrichment models (`TokenSpan`, `CliticBreakdown`, `DictionaryEntry`,
      `FalseCognateEntry`, `UserWordStatus`).

### Data layer (`core/data`, with unit tests)
- [x] JDBC `SeedDatabase` / `UserDatabase` DDL and SQLite repositories for all six tables.
- [x] Models: `CuratedLesson`, `CognateRule`, `UserWordState`, `UserCustomText`.
- [x] `CalqueWordLookupProvider` (batch dictionary + false-cognate + user-state lookup).
- [x] Packaged-asset test (`PackagedSeedAssetTest`).

### Reader core (`core/reader`, with unit tests)
- [x] `SentenceSegmenter`, `InterlinearAligner`, `SessionSentenceBuilder`, `TokenInspector`,
      `ReaderSessionEngine`, reader models (`EnrichedSentence`, `TokenInspection`, `ReaderMode`,
      `ReadingSource` curated / custom / raw).

### Toolbox engines (`core/toolbox`, with unit tests)
- [x] `TrapDetectorEngine`: High Risk / Mild Nuance / All / Seen-in-Reading filtering and 2-choice
      "Spot the Trap" quiz generation (deterministic `correctFirst` for tests).
- [x] `WordTransformerEngine`: cognate suffix bridge both directions with stem, suffixes, gender
      rule and spelling note.
- [x] `TenseShifterEngine`: 4 reading tenses, 5 subject pronouns, regular + irregular conjugation
      tables, stem/suffix decomposition (`ConjugatedVerbForm`), sentence shifting.
- [x] `NumberOdometerEngine`: masculine/feminine agreement, Spanish clock time and date phrases.
- [x] `CliticSurgeryEngine`: root + pronoun breakdown and pre-verb (proclitic) equivalent.
- [x] `OmniBarEngine`: routes digits / suffix / Spanish word to the right result type.

### Preprocessor (`tools/preprocessor`)
- [x] CLI (`--db`, `--kaikki`, `--sublex`, `--dictionary-seed`, `--false-cognates`,
      `--curated-lessons`, `--tatoeba`, `--cognate-rules`).
- [x] Processors: Kaikki, SUBTLEX-ESP reader, dictionary seed, false-cognate inflection expander,
      Tatoeba, curated lessons, cognate rules; `DatabaseOptimizer` (ANALYZE + VACUUM);
      `PreprocessorTest`.
- [x] Sets `PRAGMA user_version` to the Room schema version and emits `NOT NULL` primary keys.

### Seed data (hand-curated, checked in)
- [x] `data/seeds/*.json` and packaged `seed.db`: 174 dictionary rows, 25 false-cognate rows,
      12 cognate rules, 8 lessons (6 level-0 lessons covering Modules 0.0 to 0.5, 1 level-1 story,
      1 level-2 story).

### Persistence (Room, `app/data/room`)
- [x] `CalqueSeedDatabase` (`createFromAsset("databases/seed.db")`) and `CalqueUserDatabase`
      (no destructive migration).
- [x] Entities, DAOs (`suspend` + `Flow`), and Room-backed repositories for all six tables with
      batch chunking, LIKE-escaped prefix search, serialized vault read-modify-write.
- [x] First-star-wins `saved_context_sentence` in SQLite, Room and in-memory repositories.
- [x] Tests: schema parity vs JDBC DDL, Room-generated schema validation on the JVM (build-time
      DDL + packaged asset + `user_version` + negative controls), fake-DAO repository tests.
- [x] Application wiring (`CalqueApplication`, `DefaultAppContainer`, in-memory fallbacks).

### App shell, theme, navigation
- [x] Single-Activity Jetpack Compose app, Material 3, no XML layouts / Fragments / AppCompat views.
- [x] Light and dark themes following the system (`CalqueTheme`, `CalqueColors`), shapes and
      serif/sans typography (system families).
- [x] 3-destination bottom navigation (Read / Toolbox / Vault) with `NavStateManager`.
- [x] Manifest has no `INTERNET` permission.

### Reader screen (MVI)
- [x] `ReaderUiState` / `ReaderUiEvent` / `ReaderViewModel` (`StateFlow`), created through the
      `ViewModelStore`; IO on `Dispatchers.IO`, enrichment on `Dispatchers.Default`.
- [x] Curriculum screen: lesson list with level/module/title/concept, sample "Sentence Zero" entry.
- [x] Reading canvas: tokenized flow layout, sentence progress, natural-translation card, grammar
      callout, Gloss toggle, Translate toggle, Next / Close, completion card, back handling.
- [x] Press-and-hold "Hold for Calque" mirror (reveals glosses + translation only while held).
- [x] Word peek card: lemma, POS, status, star toggle, meaning, cognate-bridge note, false-cognate
      alert, clitic surgery line, dismiss.
- [x] Star / status updates persisted through `UserWordStateRepository`; stale-inspection
      cancellation; session cleared on close.

---

## 1. Data & ingestion (docs/INGESTION.md, docs/DATA.md)

### 1.1 Real seed data (highest impact)

Decisions (agreed):
- `seed.db` is generated, never checked in. It is the output of a Bazel target and the app and
  the tests consume that output. The checked-in `app/src/main/assets/databases/seed.db` goes away.
- The build is offline and reproducible: it downloads nothing. The raw corpora (Kaikki Spanish
  dump, ~1.1 GB; SUBTLEX-ESP; Tatoeba exports) are fetched by hand only to run the extractors and
  are never committed. The trimmed subsets the extractors produce are committed.
- Extractors live in `tools/preprocessor` (manual, rare runs; not part of the Bazel build).
- Kaikki subset = lemma entries (no `form_of` / `alt_of` senses). Keep POS: verb, noun, adj,
  adv, pron, det, article, prep, conj, num, contraction, intj, name, particle. Drop prefix, suffix,
  character, symbol, phrase, proverb and multi-word entries. Per entry keep `word`, `pos`, first
  non-empty gloss and `forms` (form + tags); drop clitic-combined forms (runtime
  `CliticDeconstructor` handles them), multi-word forms and conjugation metadata items
  (`source: conjugation` with `table-tags` / `inflection-template` / `class`). Same field names as
  Kaikki so `KaikkiProcessor` can read both the subset and a full dump.
- Plus *aliases* (found on the real dump): 85% of the dump's lines are form-of entries and the
  lemmas list almost all of those forms, but Kaikki lists very frequent words ONLY as variants:
  `tu`, `muy`, `buen`, `algún`, `quizás` (apocopic / alternative forms) and short imperatives with
  a clitic (`dame`, `dime`, `hazlo`). Dropping every form-of entry left 9% of the top 25,000
  words unresolved, including ranks 31 and 43. So an in-scope form-of word that no kept lemma
  lists is kept as an alias (`form_of` target + Kaikki's gloss and tags) and its target lemma is
  pulled in. Coverage of the top 25,000 words: 98.6% (99.8% of the top 1,000); the rest is almost
  all proper names and abbreviations. Aliases need a second pass over the dump (13 s total).
- Lemma selection (frequency-gated, N = 25,000, set by a flag): keep a lemma if its best SUBTLEX
  rank over the lemma and its forms is <= N; keep every POS entry of a selected word; add the
  lemmas needed by the selected Tatoeba sentences, the curated lessons and the false-cognate
  seeds, so every shipped lesson resolves with no unknown words.
- Stored as `data/kaikki/spanish_lemmas.jsonl.gz`, sorted by word, deterministic (fixed gzip
  mtime), plus a metadata file (dump date, source `Last-Modified` / ETag, N). Committed
  (2026-10-30 dump, N = 25,000, plus the words of the curated seeds): 14,551 lemmas + 1,973
  aliases + 197,610 forms, 17 MB raw / 1.17 MB gzipped.
- SUBTLEX-ESP is approved for use (but its license turned out to be non-commercial, see the open
  decision under PR 6). Keep only the top-N ranks (< 1 MB). Tatoeba: keep a pre-filtered set of
  candidate Spanish-English pairs with sentence ids (needed for CC BY attribution).

PR 1 - inputs, extractors, licensing:
- [x] `tools/preprocessor/README.md`: download URLs, input formats, licenses (Kaikki CC BY-SA 4.0,
      Tatoeba CC BY 2.0 FR, SUBTLEX-ESP citation) and the exact extractor and preprocessor commands.
- [x] `NOTICE` with attribution and the share-alike requirement for the generated database; cites
      Ylonen's Wiktextract paper as Kaikki asks. (A `data/kaikki/` note lands with the data in PR 6.)
- [x] `extract-kaikki` mode: stream the full dump, apply the rules above, write the sorted gzip
      subset + metadata (also reads `.gz` input and an optional `--extra-words` list). Unit tests
      with fixtures taken from real entries (form-of entry, alias, lemma with clitic-combined and
      metadata forms, multi-word entry, multi-POS word). Verified on the real dump.
- [x] SUBTLEX extractor (`extract-sublex`): reads the official `SUBTLEX-ESP.xlsx` directly (no
      manual conversion; 3 side-by-side column groups) and writes the top-N rank TSV that
      `SublexReader` consumes. Verified on the real file: 25,000 words, ~450 KB, deterministic.
- [x] Tatoeba extractor (`extract-tatoeba`): joins the per-language `sentences`, English
      `sentences` and `links` exports into single-sentence candidates with both Tatoeba ids.
      Filters: translation, 4-14 words, one sentence, plain words, no proper names, only words
      from the rank file, dedupe; a fixed id hash samples down to `--max-pairs`. Verified on the
      real export: 443,166 sentences -> 140,741 candidates -> 20,000 pairs (750 KB gzipped).
      The pool is not scored or levelled; `TatoebaProcessor` does that at build time (PR 3).

PR 4 - generated `seed.db` (done; end-to-end test in 1.3):
- [x] Bazel macro `seed_db` (`tools/preprocessor/seed_db.bzl`) runs the preprocessor in a genrule
      from checked-in files only. `//app:seed_db_asset` is now generated at
      `app/src/main/assets/databases/seed.db`, the path Android and the tests already use, so
      `PackagedSeedAssetTest`, `RoomSchemaParityTest` and `RoomGeneratedSchemaValidationTest`
      run unchanged against the generated file, and the APK contains it. `android_binary` lists
      the target explicitly (a `glob` only sees source files). PR 6 added the `kaikki`, `sublex`
      and `tatoeba` inputs to that one rule.
- [x] The checked-in binary asset is deleted and git-ignored so it cannot come back. The build is
      byte-for-byte reproducible, which is why no "is the checked-in file up to date" check is
      needed any more.
- [x] `PRAGMA user_version` stays aligned with `CalqueSeedDatabase` (the preprocessor writes
      `ROOM_SCHEMA_VERSION`; `RoomGeneratedSchemaValidationTest` guards it on the generated file).

PR 6 - real data (done, except the open decisions marked below):
- [x] **Decided - SUBTLEX-ESP license.** The official file is CC BY-NC-SA 4.0 (non-commercial,
      share-alike). Accepted: the app is open source (Apache-2.0 code) and is not sold. The code
      stays Apache-2.0; the generated `seed.db` and `data/subtlex/` carry the data licenses, and
      anyone redistributing a build inherits the non-commercial and share-alike terms (stated in
      `NOTICE`). The credits screen (below) must say so. The pipeline only needs a `word, rank`
      file, so the source can still be swapped if the distribution plan changes.
- [x] Ran the extractors with N = 25,000 and committed the subsets and metadata (`data/`, with
      a `data/README.md`): ranks 25,000 words (450 KB); Kaikki 14,546 lemmas + 1,973 aliases +
      197,526 forms (1.2 MB gzipped, 98.6% of the ranked words resolve); Tatoeba 20,000 pairs
      (750 KB). Source versions are in the `*.meta.json` files. New command `extract-seed-words`
      generates `data/kaikki/extra_words.txt` (308 words, only 10 beyond the top 25,000) so the
      words of the curated seeds always resolve; `constipado` now does.
- [x] `//app:seed_db_asset` builds from the committed subsets: 3 s, 160,019 dictionary readings,
      676 false-cognate rows, 60 lessons (12 hand-written + 48 Tatoeba), 12 rules. The Room schema
      tests, the parity test and `PackagedSeedAssetTest` (updated for the real contents) pass on
      it, and the APK ships it.
- [x] **Bug found by the guard tests after building from the real data, and fixed:** the
      capitalized Kaikki entries (`Amigo` a surname, `Bueno`, `Claro`, `Gracias` a town) won the
      row of the common word because surface forms are lowercased and uppercase sorts first, so
      167 of the 3,000 most frequent words resolved to a name (`amigo` = "a surname"). A proper
      noun now loses to any common-word row (`grammar_info = "proper noun"`, no cognate flag,
      no forms); 0 capitalized lemma rows win a common word now and 404 survive where nothing
      competes. `PackagedSeedAssetTest` pins `amigo`, `bueno`, `grande`, `claro`, `estado`, ...
- [x] **One part of speech per spelling - fixed (option 2: store every reading).** `surface_form`
      was the primary key, so a spelling with several readings kept one. Measured: 23% of the
      25,000 most frequent spellings (about half of all running text) have more than one part of
      speech, and no cheap rule picks the right one (62-71% on a hand-made key). Now the key is
      (`surface_form`, `lemma`, `pos`), `reading_order` numbers the readings (0 = primary),
      `DictionaryEntry.alternatives` and `TokenInspection.alternativeReadings` carry the others,
      the peek card shows an "Also:" line, schema version 2. 160,019 readings of 149,038
      spellings; 9,882 spellings have several readings (up to 8). `reading_overrides.json` (69
      entries) fixes the primary of the common words the automatic order got wrong; the build
      fails when an override stops matching. Noise dropped: proper nouns next to a common word,
      and the other gender of a word listed among its forms (`padre` as a form of `madre`).
      Details in INGESTION.md "Readings".
  - [ ] Follow-up: choose the reading from the sentence (an article before the word suggests a
        noun, a subject pronoun a verb). The alternatives are stored, so this needs no data
        change. Today `vino` is always *wine*, with "Also: verb (venir)" on the card.
  - [ ] Follow-up: extend `reading_overrides.json` beyond the 900 most frequent words, and fix
        the missing noun entry for `amigo` in the Kaikki data if it matters (the override uses
        the noun that does exist).
  - [ ] Follow-up: `vault` / `user_word_state` are keyed by lemma; with alternatives, decide
        whether starring a word should star the primary lemma only (today) or ask which reading.
- [x] **Decided - size.** Up to 40 MB is acceptable, so everything is kept: the rare unranked
      inflections (about 66,000 rows) give the best lookup coverage for pasted text. The
      generated `seed.db` is now 23.9 MB (20.3 MB before the readings were stored, +3.6 MB for
      the ~11,000 extra rows). Budget left: about 16 MB. If a later change needs it, the first
      lever is dropping unranked inflections of rare lemmas.
- [ ] Attribution/credits screen in the app for CC BY-SA / CC BY data (the `NOTICE` file is PR 1).

### 1.2 Pedagogical content (curated JSON seeds)
- [x] Tatoeba-bucketed sentences: 48 generated lessons (PR 3). Micro-stories: level 1 and level 2
      each went from 1 to 3 stories of 3-4 sentences (`La Cena de mi Hermana`, `Un Día en el
      Colegio`, `El Viaje Inesperado`, `La Fiesta de Cumpleaños`), with hand-written interlinear
      glosses and callouts that reuse the false cognates. They were written without a native
      review, so a native check is still wanted. `PackagedSeedAssetTest` now also checks that every
      word of the hand-written lessons resolves in the dictionary.
- [ ] More micro-stories: three per level is still thin for a curriculum.
- [ ] Module 0.0 "You Already Know 3,000 Words": 3 swipe cards (`-tion`/`-cion`, `-al`/`-al`,
      `-ic`/`-ico`) and the 5-vowel soundboard content (needs a lesson content schema for
      non-sentence cards).
- [x] Two seed inconsistencies found by building from the real data are fixed: `embarazada` was
      keyed by the feminine form in the dictionary and the false-cognate seeds (now the lemma
      `embarazado`, which expands to all four forms), and the dictionary seed filed `actualmente`
      under the lemma `actual` while the false-cognate seed has its own card (now its own
      lemma). Guard tests on the generated database: every false-cognate row matches a
      dictionary row of the same lemma, and every lemma expands to all of its dictionary forms.
- [x] `false_cognates_seed.json` grew from 25 to 38 lemmas (`ropa`, `vaso`, `colegio`, `idioma`,
      `contestar`, `decepcionar`, `disgusto`, `casualidad`, `firma`, `gripe`, `arena`, `sano`,
      `mayor`); they expand to 664 cards across all forms in the generated database, and the guard
      tests (structure, lemma-vs-dictionary, complete expansion) pass. Only 6 of their words were
      beyond the top 25,000 and came in through `extra_words.txt`.
- [ ] More false cognates are welcome (candidates not yet added: `argumento`, `exquisito`,
      `ignorar`, `atender`, `conferencia`, `bizarro`, `injuria`); each needs a native check.
- [ ] Confirm the 8 core suffix bridges (`-tion`, `-ty`, `-ous`, `-ly`, ...) each have gender rule,
      spelling note and 5-10 examples (12 rules exist today).
- [x] (PR 5) Data-validation tests for the seed JSON files (`SeedFilesValidationTest`): required
      fields, unique ids and surface forms, lowercase surfaces, allowed parts of speech, lesson id
      vs level, sequence ordering, one interlinear gloss entry per sentence word, example
      suffixes per cognate rule, severity enum, inclusive language in the English text. Example
      counts (5-10) and detector alignment were already tested in `PreprocessorTest`. The new
      test found a real violation: `dummy auxiliary` in a grammar callout (now "helper verb").
      Content items above are PR 7; propose the non-sentence lesson content schema in
      DATA.md before building the Module 0.0 cards.

### 1.3 Preprocessor correctness (docs/INGESTION.md pseudocode)

PR 2 - algorithm audit (parameterized tests, fixes where code differs from the spec):
- [x] Audit `KaikkiProcessor` against Step 1 (stream line by line, base lemma row + every inflected
      form, `grammar_info` joined tags, SUBTLEX rank lookup). Bugs found on real data, fixed in
      PR 2b: the processor now shares `KaikkiEntryParser` with the extractor, so a full dump and
      the subset follow the same rules.
  - [x] It read every `forms` item as a surface form, so conjugation metadata (`es-conj`,
        `irregular`, `o-ue alternation`) became bogus dictionary rows. Metadata, clitic-combined
        and multi-word forms are now skipped.
  - [x] It imported `form_of` / `alt_of` entries as their own lemma (e.g. `pies` with the gloss
        "plural of pie"). They are skipped, except aliases: lemma = the `form_of` target (gloss
        from the target lemma when Kaikki cut it off after a colon), `grammar_info` from the
        sense tags without `form-of`; an alias with no single-word target (`ONU`) is its own
        lemma. Aliases resolve after the stream ends, so their order does not matter.
  - [x] `frequency_rank` is now the better of the lemma's rank and the form's own rank. The
        lemma rank (what the scoring thresholds were designed for) still decides for ordinary
        forms; the form's own rank only wins for frequent variants listed separately (`tu`
        31 vs `tuyo`, `muy` vs `mucho`).
  - [x] `SublexReader` treated any row whose word is `palabra` (a real, frequent Spanish word) as
        a header line: on the real data 24,999 of 25,000 words loaded and the rank column was
        reset. Now only the first line can be a header, and only when no other column is a
        number.
  - [ ] The runtime clitic rule `length > suffix + 3` cannot resolve short imperatives such as
        `dame` / `dime` / `dile` / `hazlo`; the aliases now cover the frequent ones, but decide
        whether the rule should change (see 1.4).
  - [x] Duplicate surface forms (`fue` = ser/ir, `como` = verb/adverb) overwrote each other via
        `INSERT OR REPLACE`. Now an upsert keeps the best row: base lemma row first, then the
        better frequency rank (missing loses), then the first one seen. The curated dictionary
        seed is applied after Kaikki and always replaces. Documented in INGESTION.md Step 1.
  - [x] Apply the POS keep-list; skip entries without a gloss; count and skip malformed lines
        instead of aborting (`KaikkiProcessor.processStreamWithStats` reports the counts).
- [x] Audit `CognateDetector` against Step 2 (`SUFFIX_MAP`, accent stripping, Levenshtein <= 1,
      stem length >= 3, identical-word rule for length > 3): `CognateDetectorSpecTest` has one
      real pair per rule and every threshold. The implementation is deliberately broader than the
      pseudocode (more suffix pairs, any gloss word, 2-letter exact stems); documented in
      INGESTION.md Step 2.
- [x] Audit false-cognate expansion (Step 3: all inflections of each lemma; lemma missing from the
      dictionary): `FalseCognatesExpanderTest`; the preprocessor now warns about seeds with no
      dictionary rows. Two such seeds exist today (found by building from the real data):
  - [ ] `constipado` has no dictionary rows (too rare for the top-25,000 subset): pass the seed
        lemmas to the extractor's `--extra-words` (PR 6).
  - [ ] `embarazada` is keyed by the feminine form, so only that form gets a card; rekey the
        seed to the dictionary lemma `embarazado` (also covers `embarazados` / `embarazadas`).
- [x] Audit `SentenceScorer` (Step 4: 4-14 word filter, score formula, level thresholds
      2.0 / 4.0 / 6.5 at their boundaries, +3.0 complex tense bonus, rare rank 1500/1501):
      `SentenceScorerBoundaryTest`. No deviation from the pseudocode was found.

PR 3 - Tatoeba selection and packing (done):
- [x] Quality filters: dedupe, no proper names (extractor), every word resolves in the
      dictionary (builder), Tatoeba sentence ids kept in `content_json` for attribution.
- [x] Group accepted sentences into lessons of 8 (`--tatoeba-pack-size`), at most 16 per level
      (`--tatoeba-packs-per-level`), using the multi-sentence `{"sentences":[...]}` format of the
      curated lessons. On the real data: 48 lessons, 384 sentences, 71 KB of JSON; score ranges
      0.25-2.0 / 2.11-4.0 / 4.04-6.5 for levels 0 / 1 / 2.
- [x] Deterministic selection (a fixed hash of the Tatoeba id), lessons ordered easiest first
      within a level, ids `tatoeba_l<level>_<nnn>` and `sequence_order` continuing after the
      curated lessons of the level.
- [x] Fixed a bug found on the way: the old `TatoebaProcessor` wrote one flat object per lesson,
      which `CuratedLesson.parseSentences()` reads as an empty lesson. A test now parses the
      packs with the app's own parser.
- [ ] Follow-up: the cognate flag is accurate on the samples checked (19% of base words), but
      proper-noun glosses give false positives (`segura` = "Segura (river)", `vuelta` = "the
      Vuelta a España"). Ignore glosses that start with a capital letter in `CognateDetector`.

PR 4 - end-to-end (done):
- [x] `EndToEndPipelineTest` runs the whole chain on small fixtures (SUBTLEX counts -> ranks,
      Kaikki dump -> subset, Tatoeba exports -> pool, subsets + seeds -> `seed.db`) and checks the
      dictionary rows, the false-cognate expansion, the lesson numbering, `user_version`,
      `integrity_check`, NOT NULL primary keys and that two runs give identical bytes. Room's own
      validation is not duplicated in the pure-JVM tool: it runs on the generated asset in
      `//app:room_generated_schema_validation_test`, which is the same file the APK ships.

### 1.4 Runtime ingestion pipeline (Pipeline 2)
- [~] `ReaderEnricher` + `SessionSentenceBuilder` batch-resolve words. Remaining:
  - [ ] Verify es-ES word segmentation edge cases (`¿ ¡`, hyphens, accents, contractions).
  - [ ] Verify clitic fallback when dictionary lookup misses (accent restoration, pronoun suffix
        table, `length > suffix + 3`) with parameterized tests.
  - [ ] Performance budget: tap/paragraph load under 16 ms main-thread cost; add a benchmark-style
        JVM test with a 1,000-word paste.
  - [ ] Dictionary lemma fallback for unknown words (slang) with deterministic fallback text.

### 1.5 Persistence follow-ups (DATA.md)
- [ ] Room schema export (`exportSchema = true`) plus migration tests once `user.db` version 2 is
      needed (user.db must never use destructive migration).
- [ ] Align semantics of `user_word_state` rows created by `setWordStatus` on unseen words
      (`encounter_count = 1`) across SQLite/Room/in-memory and document in DATA.md.
- [ ] `encounter_count` is defined as "seen in completed readings"; currently incremented on token
      taps via `TokenInspector`. Move to session completion (`isCompleted`) and add tests.
- [ ] Use the `Flow` DAO methods (starred words, words by status, custom texts) for Vault screens
      instead of one-shot `suspend` reads.
- [ ] `user_custom_texts`: wire insert/update/delete into the paste flow (see 4.2).
- [ ] Back-up/restore policy for `user.db` (`allowBackup=true` today): decide and document.

---

## 2. Core logic (docs/PAGES_AND_FLOWS.md toolbox + reader engines)

Engines exist (see section 0); remaining spec gaps:

- [ ] Verify each engine against the spec with parameterized edge-case tests (number irregulars,
      stem-changing / irregular verbs, `diciéndomelo` to `decir`, ambiguous Omni-Bar inputs).
- [ ] Trap Detector "Seen in My Reading": data-layer query feeding `seenLemmas` from
      `user_word_state` (engine filter exists).
- [ ] Number Odometer: quick-increment helpers (`+1`, `+10`, `+100`, `Random`) as pure logic with
      range clamping to 0..999,999.
- [ ] Readability scorer for pasted text: percent of unfamiliar words, difficulty tier badge
      (`High Complexity (32% unfamiliar words)`), maps to `readability_level`.
- [ ] Progress model: current phase/tier and "last active lesson" for the resume card.

---

## 3. On-device AI and audio (docs/CODING.md hard constraints)

### 3.1 LLM (Android AICore / Google AI Edge, Gemini Nano only)
- [ ] Define `LlmService` interface in `core/*` (pure JVM) with a deterministic fallback
      implementation for devices without AICore or while weights download.
- [ ] Android implementation in `app/` using AICore; capability and download-state detection.
- [ ] Prompt + strict JSON schema for structured output; strip markdown fences before parsing;
      parse with `kotlinx.serialization` (no Gson/Jackson).
- [ ] `TranslationGrade` model (`CORRECT | MINOR_ISSUE | INCORRECT`, `score`, `feedback`,
      `literalBreakdown`, `suggestedTranslation`) and the deterministic fallback grader (token
      overlap against the lesson's idiomatic translation).
- [ ] "Simplify to My Level" use case with fallback (sentence splitting + vocabulary hints).
- [ ] Unknown-slang analysis fallback.
- [ ] Fake LLM service for unit tests; malformed-JSON and fenced-output test cases.

### 3.2 Text-to-speech
- [ ] `PronunciationService` interface (core) + `android.speech.tts.TextToSpeech` implementation
      (app) for offline `es-ES` / `es-MX`; slow and normal rates.
- [ ] Handle missing voice data: detect and show an unobtrusive "voice not installed" state.
- [ ] Lifecycle: init lazily, release in `onCleared()` / application scope.

### 3.3 Offline guarantee
- [ ] Add a test/CI check that the merged manifest has no `INTERNET` permission (it has none today).

---

## 4. UI (docs/PAGES_AND_FLOWS.md, docs/DESIGN.md)

### 4.1 Read tab: curriculum path and landing
- [~] Curriculum list exists. Remaining:
  - [ ] Header with current progression tier (e.g. "Phase 1: Single-Sentence Decoding").
  - [ ] Quick-resume card for the last active lesson.
  - [ ] Lessons grouped by syntactic milestone (Modules 0.0 to 1.0+) with completion state.
  - [ ] First-launch flow: land directly on Module 0.0 (Flow 1), swipeable cognate cards and the
        vowel soundboard.
- [ ] Persist per-lesson progress (completed sentences / completed flag) in `user.db`
      (needs a new table and a migration, see 1.5).

### 4.2 Custom text / paste flow (Flow 3)
- [~] Raw-text sessions work (`StartRawText`), but only the sample sentence invokes them.
  Remaining:
  - [ ] FAB "+ Paste Text" opening a sheet (clipboard paste), save to `user_custom_texts`.
  - [ ] Instant readability badge (see section 2).
  - [ ] "Simplify to Level N" toggle (needs 3.1) and original/simplified comparison toggle.
  - [ ] List saved custom texts in Read (and/or Vault); delete and rename.

### 4.3 Reading canvas
- [~] Tokenized canvas, translation, Gloss toggle, Hold for Calque (see section 0). Remaining:
  - [ ] Token markers from DESIGN.md: dotted Emerald underline (cognates), double Amber underline
        (false cognates), Study Indigo background pill for actively learning words
        (`UserWordStatus.LEARNING`). Currently only text color is changed.
  - [ ] Hold-to-reveal should animate the vertical expansion of lines (spec: smooth expand).
  - [ ] Top bar: progress indicator, full-sentence `[Listen]` button (3.2), global Gloss toggle.
  - [ ] Bottom bar: `[Test Translation]` button opening the grader drawer (3.1).
  - [ ] Level 1+ multi-sentence passage layout (`ReaderMode.PARAGRAPH`): UI switch and layout.
  - [ ] Accessibility: content descriptions, minimum 48dp touch targets, TalkBack labels for
        underline semantics (do not rely on color alone).
  - [ ] Line height 1.6x and comfortable word spacing for the Spanish serif text.

### 4.4 Peek sheet (single tap on a word)
- [~] `WordPeekCard` exists (see section 0). Remaining:
  - [ ] Make it a half-height modal bottom sheet with the sentence visible above; dismiss by tap
        or back.
  - [ ] `[Listen]` slow/normal audio buttons (3.2).
  - [ ] Tense/person breakdown for conjugated verbs.
  - [ ] Amber 4-Part Card layout for false cognates (`word != lookalike`, actual meaning, how to
        say the lookalike, mnemonic); currently a text alert.
  - [ ] Clitic compound as exploded pronoun blocks (`diciendo + me + lo`); currently a text line.
  - [ ] Deep-link footer pill (`Open in Tense Shifter` / `Open in Number Odometer`) navigating to
        the Toolbox sandbox pre-loaded with that word (navigation args).

### 4.5 Translation Grader drawer
- [ ] Drawer with text input and voice dictation (`RecognizerIntent`, offline only).
- [ ] Verdict pill: Spot On / Minor Issue / Missed Trap, explanation, literal vs natural.
- [ ] Loading, unavailable-model, and fallback states.

### 4.6 Toolbox tab (currently a placeholder; engines done)
- [ ] 2-column bento grid layout + Smart Omni-Bar at the top.
- [ ] Omni-Bar UI: digits / suffix / Spanish word routing with result previews.
- [ ] Trap Detector screen: filter chips, browsable 4-Part Cards, "Spot the Trap" quiz mode.
- [ ] Word Transformer screen: suffix selector, stem vs morphing suffix highlight, gender rule,
      spelling quirks.
- [ ] Tense Shifter screen: verb + pronoun pickers, draggable 4-stop timeline slider, live
      example sentence with the changing suffix (`ConjugatedVerbForm.suffix`) in bold color.
- [ ] Number Odometer screen: scrollable 0..999,999 odometer, quick-increment chips,
      masculine/feminine switch, clock/date tab, audio playback.
- [ ] Clitic Unpacker screen: animated explode into color-coded blocks and pre-verb toggle.
- [ ] One ViewModel + `UiState` + `UiEvent` per sandbox (MVI), with tests.

### 4.7 Vault tab (currently a placeholder)
- [ ] Mastery summary header: recognized cognates count, Learning count, Mastered count.
- [ ] Filterable word cards (all / starred / learning / mastered): word, lemma, original context
      sentence.
- [ ] Swipe right to promote to `Mastered` (removes highlight tint in future readings); undo
      snackbar.
- [ ] Empty state and delete/unstar action.
- [ ] VaultViewModel (MVI) backed by the `Flow` DAO queries.

### 4.8 Navigation, app shell, polish
- [ ] Replace manual `NavStateManager` listener plumbing with state-hoisted navigation, preserve
      per-tab state and back stack; support deep links from the peek sheet into Toolbox.
- [ ] Surface `ReaderUiState.errorMessage` (snackbar) - it is set (e.g. lesson not found) but never
      shown.
- [ ] Loading and error states for curriculum and session creation.
- [ ] Process-death handling (`SavedStateHandle` for the active lesson/sentence); configuration
      changes already survive through the `ViewModelStore`.
- [ ] App icon (currently placeholder drawables), splash, move literals to `strings.xml`, basic
      localization hygiene.

### 4.9 Design system (docs/DESIGN.md)
- [ ] Bundle fonts: Literata (or Merriweather) for the reading canvas, Inter/Roboto for UI, a
      monospace/tabular face for the Odometer and Clitic Unpacker (currently `FontFamily.Serif` /
      `SansSerif` defaults).
- [ ] Verify all palette tokens (light and dark) against DESIGN.md hex values and add a test
      asserting each token. Known mismatch: dark Study Indigo is `0xFF818CF8` in `CalqueColors`,
      docs say `#9BA8FF`. Check Crimson/Error and the Amber/Emerald fill tokens.
- [ ] Material 3 color scheme mapping (primary = slate blue accent).
- [ ] Reading canvas typography scale (line height 1.6x) and glosses at small sans sizes.
- [ ] Motion: tracing-paper reveal (hold overlay), odometer roll, clitic explode; respect reduced
      motion setting.
- [ ] Contrast audit (WCAG AA) for text on pill fills in both themes.

---

## 5. Tooling, CI and quality

- [ ] CI: confirm `bazelisk build //app:app` APK build is covered by `build //...` and, if
      feasible, add an emulator/instrumented smoke test job.
- [ ] Robolectric (or instrumented) test opening `CalqueSeedDatabase.build()` end to end; the JVM
      tests validate Room's generated schema but not Android's asset file-copy path.
- [ ] Compose UI tests for Reader flows (tap word, hold mirror, back dismisses sheet) and
      screenshot tests for light/dark themes.
- [ ] Coverage reporting for `core/*`; target 80%+ of tests as fast JVM tests (CODING.md).
- [ ] Detekt: review active rules vs Compose conventions (`LongParameterList` for composables,
      `TooManyFunctions` in ViewModels) and tune `detekt.yml` instead of adding suppressions.
- [~] Naming audit for inclusive language across code, fixtures and seed JSON (AGENTS.md): a scan
      of the repository found only the seed hit above (fixed), and `SeedFilesValidationTest` now
      guards the seed JSON. Still open: an automated guard for code and test fixtures.
- [ ] Startup performance: baseline profile, StrictMode in debug builds to catch main-thread disk
      access.
- [ ] Release engineering: signing config, versioning, R8/proguard rules for Room and
      kotlinx.serialization, release APK/AAB Bazel target.
- [ ] Documentation: keep `docs/` in sync (e.g. add a Repository Contracts section to DATA.md,
      which AGENTS.md references, and a section on the Room/JDBC schema-parity workflow).
- [ ] Repo hygiene: remove stray `t3.json` at repo root if unused; confirm `data/` vs `core/data`
      naming is intentional.

---

## 6. Suggested order (small PRs, stacked per AGENTS.md)

1. Real seed data + docs for the pipeline (1.1, 1.3) - unblocks everything visual. Done as a stack
   of 14 PRs (extractors, collision rules, Kaikki processor, audits, Tatoeba lessons, seed
   validation, generated `seed.db`, real data, fixes found on it, content) plus the readings
   change (schema v2). Decided: SUBTLEX-ESP stays, up to 40 MB is fine. Open: choosing the
   reading from the sentence, native review of the content, the credits screen, and sections
   1.4 / 1.5 (runtime ingestion and persistence follow-ups), which were not part of this batch.
2. Token markers + peek sheet completion + design tokens/fonts (4.3, 4.4, 4.9).
3. Vault tab with Flow-backed ViewModel (4.7, 1.5) and encounter-count semantics.
4. TTS service + Listen buttons (3.2).
5. Toolbox shell + Omni-Bar, then one sandbox per PR (4.6).
6. Custom text paste flow + readability scorer (4.2, section 2).
7. On-device LLM service with fallbacks, then Translation Grader and Simplify (3.1, 4.5).
8. Lesson progress persistence + resume card + first-launch flow (4.1).
9. CI/instrumented tests, release engineering (section 5).
