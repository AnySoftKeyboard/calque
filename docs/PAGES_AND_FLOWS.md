# Calque — Application Screens & User Flows

## Top-Level Navigation Structure
Calque uses a flat, persistent 3-destination bottom navigation bar (`NavigationBar`). Deep nesting is avoided; contextual inspections open in modal bottom sheets over the current workspace.

1. **📖 Read**: The primary curriculum path, custom text importer, and active reading canvas.
2. **🧰 Toolbox**: Interactive sandboxes for cognates, false friends, numbers, verb tenses, and pronoun surgery.
3. **🗂️ Vault**: The learner's personal vocabulary bank and comprehension milestones.

---

## Screen 1: Read (Home & Reading Canvas)

### Sub-View A: The Curriculum Path & Importer (Landing State)
- **Header**: Displays current progression tier (e.g., *Phase 1: Single-Sentence Decoding*) and a quick-resume card for the last active lesson.
- **Linear Lesson Track**: Vertical list of bite-sized modules grouped by syntactic milestone:
  - *Module 0.0*: The Cognate Unlock & Vowel Soundboard (Onboarding).
  - *Module 0.1*: Basic Subject-Verb-Object with Cognates.
  - *Module 0.2*: Adjectives After Nouns.
  - *Module 0.3*: Negation & Question Markers (`¿...`).
  - *Module 0.4*: Omitted Subjects (Verb Endings as Pronouns).
  - *Module 0.5*: Clitic Object Pronouns (`le`, `lo`, `se`).
  - *Module 1.0+*: Connected Micro-Stories (3–6 sentences).
- **Floating Action Button (`+ Paste Text`)**: Opens a sheet where the user can paste any Spanish text from their clipboard. Shows an instant readability badge and an optional **"Simplify to My Level"** toggle powered by the on-device LLM.

### Sub-View B: The Layered Reading Canvas (Active Lesson)
Displays either a single focus sentence (Level 0) or a short multi-sentence passage (Level 1+).
- **Top Bar**: Lesson title, progress indicator, full-sentence `[🔊 Listen]` button, and a global `[👁️ Gloss]` toggle.
- **Tokenized Spanish Text**: Rendered in a wrapping flow layout. Words display subtle underlines (Emerald dotted for cognates, Amber double-line for false cognates).
- **Bottom Action Bar**:
  - **`[🪞 Hold for Calque]` (Interlinear Mirror)**: While the user presses and holds this button, the Spanish lines smoothly expand vertically to reveal literal word-for-word English glosses underneath each token, plus the idiomatic translation at the bottom. Releasing the button hides the English immediately.
  - **`[✍️ Test Translation]`**: Opens the AI Translation Grader drawer.

### Contextual Overlays on the Reading Canvas
1. **The Peek Sheet (Single Tap on Any Word)**:
   - Slides up a half-height bottom sheet while keeping the sentence visible above.
   - Displays the exact surface word, `[🔊]` slow/normal audio button, and `[⭐ Save to Vault]` toggle.
   - **If Standard Word/Verb**: Shows dictionary lemma, part of speech, tense/person breakdown, and contextual English meaning.
   - **If False Cognate**: Renders the amber **4-Part Card** (`word ≠ lookalike`, actual meaning, how to say the lookalike in Spanish, and the mnemonic tip).
   - **If Compound Verb with Clitics**: Shows the exploded pronoun blocks (`diciendo` + `me` + `lo`).
   - **Deep-Link Footer**: A pill button at the bottom of the sheet (`[Open in Tense Shifter →]` or `[Open in Number Odometer →]`) jumps directly to the relevant Toolbox sandbox pre-loaded with that word.
2. **The Translation Grader Drawer (Tap `Test Translation`)**:
   - User types or voice-dictates their English translation of the current Spanish sentence.
   - On-device LLM evaluates comprehension and returns a color-coded verdict pill (**Spot On**, **Minor Issue**, **Missed Trap**), a concise explanation of any misread syntax or false friend, and the literal vs. natural translation.

---

## Screen 2: Toolbox (Interactive Sandboxes)

Organized as a **2-Column Bento Grid** with a **Smart Omni-Bar** at the top.

### The Smart Omni-Bar
A universal input field at the top of the Toolbox:
- Typing digits (`492`) immediately renders the spelled-out Spanish number with audio and gender toggles.
- Typing a suffix (`-tion` or `-dad`) opens the matching Cognate Transformer rule.
- Typing any Spanish word opens its morphological breakdown or False Cognate card.

### The 5 Sandbox Modules
1. **⚠️ The Trap Detector (False Cognates)**:
   - Filterable by *All*, *High Risk*, *Mild Nuance*, and *Seen in My Reading*.
   - Toggle between **Browsable 4-Part Cards** and **"Spot the Trap" Quiz Mode** (2-choice comprehension check on sentences containing a false friend).
2. **🔄 The Word Transformer (Cognate Suffix Bridge)**:
   - Interactive selector for the 8 core Latin suffix bridges (`-tion` $\rightarrow$ `-ción`, `-ty` $\rightarrow$ `-dad`, `-ous` $\rightarrow$ `-oso`, `-ly` $\rightarrow$ `-mente`, etc.).
   - Highlights stems vs. morphing suffixes, gender rules, and orthographic quirks (such as dropping double letters).
3. **⏳ The Tense Shifter (Verb Timeline)**:
   - Lets the learner pick a common verb and subject pronoun, then drag a horizontal timeline slider across four reading tenses: **Past Completed (*Pretérito*)**, **Past Ongoing (*Imperfecto*)**, **Present (*Presente*)**, and **Near Future (*Ir a + Infinitive*)**.
   - Morphs the verb inside a live example sentence, highlighting only the changing suffix in bold color.
4. **🔢 The Number Odometer (Counting, Dates & Time)**:
   - Live scrollable odometer (`0` to `999,999`) with quick increment chips (`+1`, `+10`, `+100`, `Random`).
   - Includes a `[Masculine / Feminine]` noun switch that visually highlights gender agreement shifts (*doscientos un libros* vs. *doscientas una páginas*) and a secondary tab for Spanish clock time and date conventions.
5. **🧩 The Clitic Unpacker ("Pronoun Surgery")**:
   - Interactive workbench where compound verbs (*explicárselo*, *comprártelo*) animate apart into color-coded root + indirect + direct pronoun blocks, alongside a toggle showing the equivalent pre-verb structure (*se lo va a explicar*).

---

## Screen 3: Vault (Vocabulary & Progress)

- **Mastery Summary Header**: Clean count of recognized cognates, words in active study (`Learning`), and words marked `Mastered`.
- **Filterable Word Cards**: List of words the user has tapped or starred during reading.
  - Each card displays the word, its lemma, and the **exact sentence from the story where the user encountered it**.
  - Users can swipe right on any card to promote it to `Mastered` (which removes its highlight tint in future readings).

---

## Core User Flows

### Flow 1: Complete Beginner First Launch (0-to-1 Flow)
1. User launches Calque $\rightarrow$ Lands directly on **Module 0.0: You Already Know 3,000 Words**.
2. Swipes through 3 interactive cards transforming `-tion` $\rightarrow$ `-ción`, `-al` $\rightarrow$ `-al`, and `-ic` $\rightarrow$ `-ico`.
3. Taps through the 5-Vowel Soundboard (`A, E, I, O, U`) to calibrate internal reading pronunciation.
4. Enters **Sentence Zero** (*"El hotel moderno es excelente."*). Sees emerald cognate underlines, holds the `[🪞 Hold for Calque]` button to confirm the word-for-word skeleton, and completes their first reading in under 2 minutes.

### Flow 2: Encountering & Resolving a Reading Blocker
1. User is reading a Level 1 micro-story and hits an unfamiliar sentence: *"Elena no soporta el ruido y decide explicárselo al director."*
2. User notices `soporta` has a double amber underline (False Cognate Trap) and taps it.
3. **Peek Sheet** opens showing the 4-Part Card: `soportar ≠ support` $\rightarrow$ means *to tolerate/endure*.
4. User taps `explicárselo`. **Peek Sheet** displays the Clitic Surgery breakdown: `explicar` (to explain) + `se` (to him) + `lo` (it).
5. User taps `[✍️ Test Translation]`, types *"Elena can't stand the noise and decides to explain it to the director"*, and receives a green **Spot On** verification from the on-device LLM.

### Flow 3: Custom Clipboard Reading & Level Simplification
1. User copies a paragraph from a Spanish news site or message and taps `+ Paste Text` on the **Read** tab.
2. App runs the on-device ingestion pipeline and reports: *Readability: High Complexity (32% unfamiliar words)*.
3. User taps **"Simplify to Level 1"**. On-device LLM rewrites the paragraph into short, present/simple-past sentences while preserving the facts.
4. User reads the simplified version on the Layered Reading Canvas with full tooltip, cognate, and calque mirror support, then toggles back to compare it against the original Spanish text.
