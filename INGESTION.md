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
