# Calque — Data Architecture & Schemas

## Overview
Calque relies on a two-part local SQLite database managed by Room:
1. **Pre-packaged Read-Only Seed Tables**: Built offline during compilation and shipped in `assets/databases/seed.db`. Provides $O(1)$ morphological lookups, cognate flags, false-friend alerts, and progressive reading lessons.
2. **Mutable User State Tables**: Tracks the learner's vocabulary mastery, reading progress, saved words, and dynamically cached AI analyses.

## External Data Sources (Offline Seed Inputs)
- **Kaikki.org (Structured Wiktionary Spanish Dump)**: Provides lemmas, part-of-speech tags, inflected surface forms (verb conjugations, noun plurals, gender variants), and English definitions. Licensed under CC BY-SA.
- **Tatoeba Project (Spanish–English Sentence Pairs)**: Provides human-verified bilingual sentences used to seed Level 0 through Level 2 reading progressions. Licensed under CC BY 2.0.
- **SUBTLEX-ESP Word Frequencies**: Film and television subtitle frequency counts used to rank how common a Spanish word is in everyday life and calculate sentence readability scores.
- **Curated Pedagogical JSON Seeds**: Checked-in repository files containing handcrafted false-cognate 4-part cards, cognate suffix rules, and core syntactic progression notes.

---

## Database Tables

### 1. `dictionary` (Seed Table)
Maps every lowercased Spanish surface form (including conjugated verbs and plurals) directly to its root lemma and grammatical role.
- `surface_form` (Text, Primary Key): Exact lowercase word as it appears in text (e.g., `hablábamos`).
- `lemma` (Text, Indexed): Base dictionary form (e.g., `hablar`).
- `pos` (Text): Part of speech (`verb`, `noun`, `adj`, `adv`, `pron`, `prep`, `conj`).
- `grammar_info` (Text, Nullable): Concise morphological tag (e.g., `1st pl. imperfect`, `fem. plural`).
- `english_gloss` (Text): Concise English meaning in context.
- `is_cognate` (Boolean/Integer): `1` if the word shares a transparent Latin/English root.
- `frequency_rank` (Integer, Nullable): Rank from SUBTLEX-ESP (lower number = more common).

### 2. `false_cognates` (Seed Table)
Stores the "4-Part Card" structure for every surface form of known Spanish-English false friends.
- `surface_form` (Text, Primary Key): Lowercased token (e.g., `carpetas`, `éxito`).
- `lemma` (Text, Indexed): Base dictionary form (e.g., `carpeta`).
- `apparent_english` (Text): The false lookalike an English speaker assumes (e.g., `carpet`).
- `actual_english` (Text): The true meaning of the Spanish word (e.g., `folder / binder`).
- `real_spanish_for_apparent` (Text): How to actually say the English lookalike in Spanish (e.g., `alfombra`).
- `severity` (Text): `HIGH_RISK` (total semantic trap) or `MILD_NUANCE` (partial overlap).
- `disambiguation_tip` (Text): One-sentence cognitive hook or mnemonic.
- `example_es` (Text): Sample Spanish sentence demonstrating true usage.
- `example_en` (Text): English translation of the sample sentence.

### 3. `curated_lessons` (Seed Table)
Stores the structured reading progression from single sentences (Level 0) to short stories (Level 1–2).
- `lesson_id` (Text, Primary Key): Unique slug (e.g., `lvl0_04_negation`).
- `level` (Integer, Indexed): Difficulty tier (`0`, `1`, `2`).
- `sequence_order` (Integer): Linear ordering within the level.
- `title` (Text): Lesson title (e.g., `Adjectives After Nouns`).
- `target_concept` (Text): One-line explanation of the syntactic pattern introduced.
- `content_json` (Text): Serialized list of sentences, interlinear word-for-word gloss mappings, idiomatic translations, and grammar callouts.

### 4. `cognate_rules` (Seed Table)
Reference data for the Word Transformer toolbox module.
- `rule_id` (Text, Primary Key): Identifier (e.g., `tion_to_cion`).
- `english_suffix` (Text): e.g., `-tion`.
- `spanish_suffix` (Text): e.g., `-ción`.
- `gender_rule` (Text, Nullable): e.g., `Always feminine (la)`.
- `spelling_note` (Text): Notes on orthographic shifts (e.g., `Drop double consonants`).
- `examples_json` (Text): Array of 5–10 high-frequency English/Spanish word pairs.

### 5. `user_word_state` (User State Table)
Tracks the learner's personal relationship with vocabulary items.
- `lemma` (Text, Primary Key): Tracked at the lemma level so learning `comer` updates all its inflections.
- `status` (Text, Indexed): `NEW`, `LEARNING`, or `MASTERED`.
- `starred` (Boolean/Integer): `1` if explicitly saved to the Vault by the user.
- `saved_context_sentence` (Text, Nullable): The exact Spanish sentence where the user first starred the word.
- `encounter_count` (Integer): Number of times seen in completed readings.
- `last_seen_timestamp` (Long): Epoch milliseconds.

### 6. `user_custom_texts` (User State Table)
Stores articles or passages pasted by the user, along with their AI-simplified variants.
- `text_id` (Integer, Primary Key, AutoIncrement).
- `title` (Text).
- `original_spanish` (Text).
- `simplified_spanish` (Text, Nullable): Level-adjusted version generated by on-device LLM.
- `readability_level` (Integer): Calculated difficulty tier.
- `created_timestamp` (Long).

---

## Runtime In-Memory Domain Models
- **`TokenSpan`**: Represents a slice of a rendered sentence (`rawText`, `normalizedWord`, `startIndex`, `endIndex`, `isWord`, `dictionaryEntry`, `falseCognateEntry`, `cliticBreakdown`, `userStatus`).
- **`CliticBreakdown`**: Represents a compound verb unpacked at runtime (`rootSurface`, `rootLemma`, `attachedPronouns`, `combinedEnglishGloss`).
- **`TranslationGrade`**: Structured evaluation returned by the on-device LLM (`verdict`: `CORRECT` | `MINOR_ISSUE` | `INCORRECT`, `score`, `feedback`, `literalBreakdown`, `suggestedTranslation`).
