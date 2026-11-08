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
- [ ] Obtain and document inputs: Kaikki Spanish JSONL dump, SUBTLEX-ESP frequencies, Tatoeba
      Spanish-English pairs. Add a `tools/preprocessor/README` (or `docs/` section) with download
      URLs, licenses (CC BY-SA, CC BY 2.0) and the exact preprocessor command.
- [ ] Run the preprocessor against the full corpora and regenerate
      `app/src/main/assets/databases/seed.db` (currently the small hand-made seed).
- [ ] Keep `PRAGMA user_version` aligned with `CalqueSeedDatabase` version when regenerating
      (guarded by `RoomGeneratedSchemaValidationTest`).
- [ ] Decide asset size budget; consider keeping the full `seed.db` out of git (LFS or release
      artifact fetched by a Bazel rule) so the repo does not grow with every regeneration.
- [ ] Add attribution/credits screen or `NOTICE` file for CC BY-SA / CC BY data.

### 1.2 Pedagogical content (curated JSON seeds)
- [ ] More level 1-2 micro-stories (3-6 sentences) and Tatoeba-bucketed sentences; only one story
      per level exists today.
- [ ] Module 0.0 "You Already Know 3,000 Words": 3 swipe cards (`-tion`/`-cion`, `-al`/`-al`,
      `-ic`/`-ico`) and the 5-vowel soundboard content (needs a lesson content schema for
      non-sentence cards).
- [ ] Expand `false_cognates_seed.json` (25 rows today); verify every lemma expands to all inflections.
- [ ] Confirm the 8 core suffix bridges (`-tion`, `-ty`, `-ous`, `-ly`, ...) each have gender rule,
      spelling note and 5-10 examples (12 rules exist today).
- [ ] Add data-validation tests/lint for the seed JSON files (required fields, unique ids,
      sequence ordering, example counts).

### 1.3 Preprocessor correctness (docs/INGESTION.md pseudocode)
- [ ] Audit `KaikkiProcessor` against Step 1 (stream line by line, base lemma row + every inflected
      form, `grammar_info` joined tags, SUBTLEX rank lookup).
- [ ] Audit `CognateDetector` against Step 2 (`SUFFIX_MAP`, accent stripping, Levenshtein <= 1,
      stem length >= 3, identical-word rule for length > 3).
- [ ] Audit false-cognate expansion (Step 3) and `SentenceScorer` (Step 4: 4-14 word filter, score
      formula, level thresholds 2.0 / 4.0 / 6.5, complex tense bonus).
- [ ] End-to-end preprocessor test that builds a tiny `seed.db` from fixture inputs and validates
      it with Room's generated schema validation.
- [ ] Bazel target/genrule to build `seed.db` reproducibly and a check that the checked-in asset
      is up to date.

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
- [ ] Naming audit for inclusive language across code, fixtures and seed JSON (AGENTS.md).
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

1. Real seed data + docs for the pipeline (1.1, 1.3) - unblocks everything visual.
2. Token markers + peek sheet completion + design tokens/fonts (4.3, 4.4, 4.9).
3. Vault tab with Flow-backed ViewModel (4.7, 1.5) and encounter-count semantics.
4. TTS service + Listen buttons (3.2).
5. Toolbox shell + Omni-Bar, then one sandbox per PR (4.6).
6. Custom text paste flow + readability scorer (4.2, section 2).
7. On-device LLM service with fallbacks, then Translation Grader and Simplify (3.1, 4.5).
8. Lesson progress persistence + resume card + first-launch flow (4.1).
9. CI/instrumented tests, release engineering (section 5).
