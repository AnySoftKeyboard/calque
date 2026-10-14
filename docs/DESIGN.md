# Calque — Design System & UX Philosophy

## UX Philosophy: "Editorial Clarity, Zero Friction"
The interface is modeled after high-craft editorial typography combined with tactile, precision reference tools. The Spanish text is always the hero. Annotations, grammatical hints, and translations act as a translucent tracing-paper layer (*calque*) that appears on demand and recedes immediately when no longer needed.

### Core Interaction Principles
1. **Protect the Reading Flow**: Never navigate away from an active reading screen to show a definition or grammar rule. Use peek sheets, inline expansions, and press-and-hold overlays.
2. **Progressive Disclosure**: Do not clutter the paragraph with permanent interlinear English text. Let typography subtly hint at word roles (via delicate underlines), and reveal deeper layers only when the user taps or holds.
3. **Tactile Sandboxes Over Static Tables**: Reference concepts (numbers, verb tenses, cognates) must be interactive sliders, wheels, or exploded views—never static spreadsheets.
4. **Effortless Reversibility**: Any inspection state (opening a word card, revealing a literal gloss, jumping to a toolbox sandbox) must be dismissible with a single tap or back gesture.

## Typography
- **Reading Canvas (Spanish Text)**: High-legibility editorial Serif (e.g., *Literata* or *Merriweather*). Generous line height (`1.6x`) and comfortable word spacing to accommodate inline underlines and interlinear expansion.
- **UI Controls, Glosses & Metadata**: Clean, neutral Neo-Grotesque Sans-Serif (e.g., *Inter* or *Roboto*). Used at smaller sizes for grammatical tags, literal word-for-word glosses, and navigation labels.
- **Morphological Breakdowns & Numbers**: Monospace or tabular-numeral Sans-Serif for alignment inside the Number Odometer and Clitic Unpacker.

## Visual Token Cues
Inside the reading canvas, words carry subtle typographic markers:
- **Known / Standard Words**: Crisp primary ink, no decoration.
- **True Cognates**: Delicate dotted underline in **Cognate Emerald**. Signals the reader to trust their English intuition.
- **False Cognates (Traps)**: Double solid underline in **Warning Amber**. Signals the reader to stop and inspect rather than guess.
- **Actively Learning Words**: Soft background pill tint in **Study Indigo** to reinforce recognition across different stories.

## Color Palette

### Light Theme ("Warm Paper & Ink")
Designed to mimic warm, low-glare book paper for extended reading comfort.

- **Background Primary (Paper)**: `#FBF9F5` (Warm alabaster)
- **Background Secondary (Cards / Sheets)**: `#F3EFE6` (Soft parchment)
- **Surface Elevated (Modals / Popovers)**: `#FFFFFF` (Pure crisp white)
- **Text Primary (Main Ink)**: `#1C1B18` (Deep charcoal ink)
- **Text Secondary (Glosses / Metadata)**: `#605C54` (Muted umber)
- **Border / Divider**: `#E4DFD3` (Subtle warm stone)
- **Primary Accent (Interactive / Focus)**: `#2B5B84` (Editorial slate blue)
- **Semantic - Cognate (Emerald)**: `#1E7A56` (Text/Line), `#E6F4EE` (Soft Pill Fill)
- **Semantic - False Cognate Trap (Amber)**: `#B85D00` (Text/Line), `#FFF3E0` (Soft Alert Fill)
- **Semantic - Learning / Saved (Indigo)**: `#4A55A2` (Text/Line), `#ECEFF9` (Soft Pill Fill)
- **Semantic - Error / Mismatch (Crimson)**: `#BA1A1A` (Text), `#FFDAD6` (Fill)

### Dark Theme ("Late Night Study")
Designed for low-light OLED screens, avoiding harsh pure black (`#000000`) to prevent text halation during reading.

- **Background Primary (Night Slate)**: `#141413` (Warm obsidian)
- **Background Secondary (Cards / Sheets)**: `#1F1E1B` (Elevated dark stone)
- **Surface Elevated (Modals / Popovers)**: `#2A2925` (Charcoal surface)
- **Text Primary (Main Ink)**: `#EAE6DF` (Soft bone white)
- **Text Secondary (Glosses / Metadata)**: `#A39E93` (Warm ash gray)
- **Border / Divider**: `#33312C` (Muted graphite)
- **Primary Accent (Interactive / Focus)**: `#7FB3DF` (Luminous sky slate)
- **Semantic - Cognate (Emerald)**: `#5CD6A5` (Text/Line), `#133326` (Soft Pill Fill)
- **Semantic - False Cognate Trap (Amber)**: `#FFB76B` (Text/Line), `#3A230A` (Soft Alert Fill)
- **Semantic - Learning / Saved (Indigo)**: `#9BA8FF` (Text/Line), `#1F2547` (Soft Pill Fill)
- **Semantic - Error / Mismatch (Crimson)**: `#FFB4AB` (Text), `#410E0B` (Fill)
