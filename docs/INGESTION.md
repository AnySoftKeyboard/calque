# Calque — Data Ingestion & Processing Pipelines

## Overview
To guarantee zero-latency UI rendering and shared logic, all linguistic algorithms live in a pure-JVM module (`:core:linguistics`). These exact same algorithms run in two places:
1. **Build-Time Preprocessor CLI (`:tools:preprocessor`)**: Streams raw open-source corpora on a workstation to build `seed.db`.
2. **Runtime Ingestion Pipeline (`:app`)**: Processes pasted user text or AI-generated sentences on the Android device.

---

## Pipeline 1: Build-Time `seed.db` Generation

### Step 1: Kaikki Dictionary Flattening & Cognate Tagging
Streams the Kaikki JSONL dump line-by-line (never loading the full file into memory) and flattens both base lemmas and inflected forms into the `dictionary` table.

```text
FUNCTION process_kaikki_stream(jsonl_file, sublex_frequencies, db):
  FOR EACH line IN jsonl_file:
    entry = parse_json(line)
    lemma = entry.word
    pos = entry.pos
    primary_gloss = entry.senses[0].glosses[0]
    freq_rank = sublex_frequencies.get(lemma.lowercase())
    is_cognate = detect_cognate(lemma, primary_gloss)

    # 1. Insert base lemma row
    db.insert_dictionary(
      surface = lemma.lowercase(),
      lemma = lemma,
      pos = pos,
      grammar = "base",
      gloss = primary_gloss,
      is_cognate = is_cognate,
      freq = freq_rank
    )

    # 2. Expand all inflected forms (conjugations, plurals)
    FOR EACH form IN entry.forms:
      db.insert_dictionary(
        surface = form.surface.lowercase(),
        lemma = lemma,
        pos = pos,
        grammar = join(form.tags, ", "),
        gloss = primary_gloss,
        is_cognate = is_cognate,
        freq = freq_rank
      )
```

**Frequency.** A row's `frequency_rank` is the better (lower) of its lemma's SUBTLEX rank and the surface form's own rank. The lemma rank is what the Step 4 thresholds are tuned for; the surface rank only wins for frequent variants that Kaikki lists separately, such as `tu` (rank 31) next to `tuyo`.

**Aliases.** Kaikki lists some very frequent words only as variants of another word (`tu`, `muy`, `buen` are "apocopic form of ..."; `dame`, `dime` are "imperative of ... combined with ..."). The lemma does not list them in its `forms`, so each becomes a row of its own: `lemma` is the target, `grammar_info` the variant's tags, and `english_gloss` Kaikki's description (with the target's gloss appended when Kaikki cut it off after a colon). A variant with no single-word target, such as the acronym `ONU`, is its own lemma.

**Surface-form collisions.** `surface_form` is the primary key, so words that are several things at once (`fue` is *ser* and *ir*, `como` is an adverb and a form of *comer*) compete for one row. The builder keeps the best row: a lemma row (`grammar_info = "base"`) beats an inflected row, which beats a proper noun (`grammar_info = "proper noun"`); otherwise the lower `frequency_rank` wins, and a missing rank loses to any rank; on a tie the row seen first stays. The curated `dictionary_seed.json` is applied after Kaikki and always replaces the Kaikki row for the same word.

**Proper nouns.** Kaikki has `Amigo` (a surname), `Bueno`, `Claro` and `Gracias` (a town) next to the words `amigo`, `bueno`, `claro` and `gracias`. Surface forms are lowercased, so they collide, and before this rule existed the capitalized entry won ties because uppercase sorts first: 167 of the 3,000 most frequent words resolved to a name. A lemma that starts with a capital letter is now a proper noun: it never hides a common word, is not flagged as a cognate and does not list its forms, but it is still stored when no common word has the same spelling (`madrid`).

**Known limit: one part of speech per spelling.** When a spelling has several lemma entries with different parts of speech (`padre` is a noun and an informal adjective, `claro` an adjective, a noun and an adverb), only one row can be stored and the tie is broken by input order, which is alphabetical by part of speech. That picks the intended entry for about two thirds of the common words checked. Fixing it needs either a data model with one row per (spelling, part of speech) or part-of-speech frequencies; until then the curated `dictionary_seed.json` is the override for words that matter.

### Step 2: Algorithmic Cognate Detection
Identifies whether a Spanish word and its English definition share a transparent root.

```text
FUNCTION detect_cognate(spanish_word, english_gloss):
  es = normalize_lowercase(spanish_word)
  en = extract_primary_word(english_gloss)

  IF length(es) > 3 AND es == en:
    RETURN TRUE

  SUFFIX_MAP = [
    ("ción", "tion"), ("sión", "sion"), ("dad", "ty"),
    ("mente", "ly"), ("oso", "ous"), ("ico", "ic"),
    ("al", "al"), ("cto", "ct"), ("ncia", "nce")
  ]

  FOR EACH (es_suffix, en_suffix) IN SUFFIX_MAP:
    IF es.ends_with(es_suffix) AND en.ends_with(en_suffix):
      es_stem = strip_accents(es.remove_suffix(es_suffix))
      en_stem = en.remove_suffix(en_suffix)
      IF length(es_stem) >= 3 AND levenshtein_distance(es_stem, en_stem) <= 1:
        RETURN TRUE

  RETURN FALSE
```

**Implementation notes (audited against the pseudocode above).** `CognateDetector` follows the rule but is deliberately a little broader, and the tests (`CognateDetectorSpecTest`) pin each difference:
- The suffix map has more pairs than the nine above: `osa`, `ica`, `cta`, `ble`, `ario`/`aria`, `ivo`/`iva`, `ido`/`ida`, `ura`, `or`, `ar`, `er`, `ir`, `izar`.
- Every word of the gloss is tried (synonyms separated by `,` `;` `/`), not only the primary one, after dropping parenthetical notes and a leading `to` / `a` / `an` / `the`.
- A stem that is exactly equal needs 2 letters (`ac` + `ción` / `ac` + `tion`), the fuzzy distance-1 rule needs 3. A 1-letter stem never matches.
- Accents are stripped from the Spanish stem only.

### Step 3: False Cognate Inflection Expansion
Takes the curated `false_cognates_seed.json` file (which defines lemmas and 4-Part Card metadata) and expands it across every matching inflection in the `dictionary` table.

```text
FUNCTION expand_false_cognates(seed_json_file, db):
  seeds = parse_json(seed_json_file)
  FOR EACH seed IN seeds:
    # Find all conjugated/plural surface forms of this trap lemma
    matching_surfaces = db.query("SELECT surface_form FROM dictionary WHERE lemma = ?", seed.lemma)
    
    FOR EACH surface IN matching_surfaces:
      db.insert_false_cognate(surface, seed)
```

A seed whose `lemma` has no dictionary rows is still stored under its own lowercased form, and the preprocessor prints a warning naming it. Typical causes: the word is too rare for the dictionary subset (add it to the extractor's `--extra-words`), or the seed is keyed by an inflected form instead of the dictionary lemma (`embarazado`, not `embarazada`).

### Step 4: Sentence Scoring & Level Bucketing
Scores bilingual sentences from Tatoeba to populate `curated_lessons`.

```text
FUNCTION score_sentence(spanish_sentence, db):
  tokens = tokenize(spanish_sentence)
  word_tokens = filter_words_only(tokens)
  
  IF count(word_tokens) < 4 OR count(word_tokens) > 14:
    RETURN DISCARD

  rare_word_ratio = count(w WHERE w.freq_rank > 1500) / count(word_tokens)
  cognate_ratio = count(w WHERE w.is_cognate == TRUE) / count(word_tokens)
  has_complex_tense = any(w WHERE w.grammar CONTAINS "subjunctive" OR "conditional")

  score = (count(word_tokens) * 0.25) + (rare_word_ratio * 5.0) - (cognate_ratio * 2.5)
  IF has_complex_tense:
    score += 3.0

  IF score <= 2.0: RETURN LEVEL_0
  ELSE IF score <= 4.0: RETURN LEVEL_1
  ELSE IF score <= 6.5: RETURN LEVEL_2
  ELSE: RETURN DISCARD
```

---

**Packing.** Tatoeba sentences are not stored one per lesson. The candidates (already filtered to single sentences made of words from the frequency list) are scored as above against the dictionary; a sentence with a word the dictionary does not know is skipped so that every shipped word resolves. Per level a fixed number of sentences is picked with a fixed hash of the Tatoeba id (an unbiased, reproducible sample), ordered from easiest to hardest and cut into lessons of 8, so the lessons of a level get harder as the learner goes on. A lesson's `content_json` is `{"sentences":[{"sentence_es", "translation_en", "tatoeba_spanish_id", "tatoeba_english_id", "score"}]}`; the interlinear gloss is generated at runtime, and the ids keep every sentence attributable (CC BY).

## Pipeline 2: On-Device Runtime Text Ingestion

When a user opens a lesson or pastes custom Spanish text, the app runs this pipeline to produce interactive `TokenSpan` objects in under 15ms.

```text
FUNCTION enrich_text_for_reader(raw_spanish_text, db, llm_service):
  # 1. Unicode-aware word boundary segmentation (ICU BreakIterator)
  raw_spans = icu_word_break_iterator(raw_spanish_text, Locale("es", "ES"))
  unique_words = extract_unique_lowercase_words(raw_spans)

  # 2. Single-pass SQLite batch queries
  dict_map = db.batch_lookup_dictionary(unique_words)
  trap_map = db.batch_lookup_false_cognates(unique_words)
  user_state_map = db.batch_lookup_user_state(unique_words)

  # 3. Assemble enriched TokenSpans
  enriched_spans = []
  FOR EACH span IN raw_spans:
    IF NOT span.is_word:
      enriched_spans.append(PunctuationOrWhitespaceSpan(span))
      CONTINUE

    norm = span.text.lowercase()
    dict_entry = dict_map.get(norm)
    trap_entry = trap_map.get(norm)
    clitic_info = NULL

    # 4. If missing from dictionary, attempt Clitic Pronoun Surgery
    IF dict_entry IS NULL:
      clitic_info = deconstruct_clitics(norm, db)
      IF clitic_info IS NOT NULL:
        dict_entry = clitic_info.root_dictionary_entry

    enriched_spans.append(
      TokenSpan(
        text = span.text,
        dictionary = dict_entry,
        false_cognate = trap_entry,
        clitic = clitic_info,
        user_status = user_state_map.get(dict_entry.lemma) ?? NEW
      )
    )

  RETURN enriched_spans
```

### Algorithm: Clitic Pronoun Deconstruction ("Pronoun Surgery")
Unpacks Spanish verbs with attached object pronouns (e.g., *diciéndomelo* -> *diciendo* + *me* + *lo*).

```text
FUNCTION deconstruct_clitics(word, db):
  PRONOUN_SUFFIXES = [
    "melo", "telo", "selo", "noslo", "mela", "tela", "sela", "nosla",
    "melos", "telos", "selos", "melas", "telas", "selas",
    "me", "te", "se", "nos", "le", "les", "lo", "la", "los", "las"
  ]

  FOR EACH suffix IN PRONOUN_SUFFIXES:
    IF word.ends_with(suffix) AND length(word) > length(suffix) + 3:
      stripped_stem = word.remove_suffix(suffix)
      # Restore orthographic accent added for stress preservation (diciéndo -> diciendo)
      unaccented_stem = strip_written_accent_from_last_syllable(stripped_stem)
      
      root_entry = db.lookup_single_word(unaccented_stem)
      IF root_entry IS NOT NULL AND root_entry.pos == "verb":
        pronoun_parts = split_compound_pronoun(suffix) # e.g., "melo" -> ["me", "lo"]
        RETURN CliticBreakdown(root_entry, pronoun_parts)

  RETURN NULL
```

### Algorithm: Deterministic Number Converter
Converts any integer (`0..999,999`) into gender-aware written Spanish words at runtime without database lookups.
- Handles exact units (`0..15`), fused tens (`16..29`: *dieciséis*, *veintiuno*), separated conjunction tens (`31..99`: *treinta y uno*), irregular hundreds (*cien* vs *ciento*, *quinientos*, *setecientos*, *novecientos*), apocope before masculine nouns (*veintiún*), and feminine agreement (*doscientas*).
