# Calque — Engineering Standards & Technical Constraints

## Build System & Repository Architecture
- **Build System**: **Bazel** (using **Bzlmod** via `MODULE.bazel`).
- **Language**: **100% Kotlin**.
- **Strict Module Boundary**:
  - `//core/linguistics`: Pure Kotlin JVM library (`kt_jvm_library`). Zero Android dependencies (`android.*` imports are strictly forbidden, except standard `java.text.BreakIterator` or injected interfaces). Houses all tokenizers, cognate matchers, clitic stemmers, readability scorers, and number converters.
  - `//tools/preprocessor`: Pure Kotlin JVM CLI binary (`kt_jvm_binary`). Depends on `//core/linguistics` and `sqlite-jdbc`. Runs on the developer's host workstation to compile raw corpora into `seed.db`.
  - `//app`: Android application (`kt_android_library` + `android_binary`). Houses Jetpack Compose UI, Room database bindings, Android TTS, and on-device AICore integration.

## Frameworks & Libraries
- **UI Framework**: **Jetpack Compose** with **Material 3**.
  - No XML layouts, no Fragments, no legacy `AppCompat` views.
  - Single-Activity architecture (`MainActivity`).
- **Local Persistence**: **AndroidX Room (SQLite)**.
  - Initialized from pre-packaged asset (`createFromAsset("databases/seed.db")`).
  - All DAO queries returning reading state must expose `Flow<T>` or `suspend` functions.
- **Asynchronous Concurrency**: **Kotlin Coroutines & Flow** (`StateFlow`, `SharedFlow`).
  - Disk I/O and database batch queries must run on `Dispatchers.IO`.
  - Token enrichment and string parsing must run on `Dispatchers.Default`.
- **Serialization**: `kotlinx.serialization` (JSON). Do not use Gson or Jackson.
- **Audio / Pronunciation**: Native Android `android.speech.tts.TextToSpeech` configured for offline Spanish locales (`es-ES` / `es-MX`).

## Hard Architectural Constraints
1. **100% Offline-First**: The app must never require network permissions (`android.permission.INTERNET` should not be required for core functionality). All dictionaries, lessons, audio synthesis, and AI inference run locally on the device.
2. **On-Device LLM Only**: AI capabilities (grading user translations, simplifying pasted Spanish text, fallback analysis for unknown slang) must execute via **Android AICore / Google AI Edge SDK** (Gemini Nano on-device).
   - Every LLM call must have a deterministic fallback in case the user's device does not support AICore or weights are still downloading.
   - All LLM prompts requiring structured output must enforce a strict JSON schema and strip markdown fences before parsing.
3. **Zero Main-Thread Blocking**: Tapping a word or loading a paragraph must respond in under 16ms (1 frame). Never run individual SQLite queries per token inside a `@Composable` loop; always batch-resolve tokens at the paragraph or lesson level inside the ViewModel.

## State Management Pattern (MVI / Unidirectional Data Flow)
Every feature screen consists of:
- An immutable `@Stable` or `@Immutable` `UiState` data class.
- A sealed interface of `UiEvent` (or `UiAction`) representing user intents.
- A `ViewModel` exposing `val uiState: StateFlow<UiState>`.

## Testing Philosophy
- **Fast Pure-JVM Unit Tests First**: Because `//core/linguistics` and `//tools/preprocessor` are pure JVM targets, 80%+ of tests must run as standard `kt_jvm_test` targets via `bazel test //...` in milliseconds without starting an Android emulator.
- **Deterministic Linguistic Suites**: Every edge case in `NumberConverter`, `CognateDetector`, and `CliticDeconstructor` (e.g., accent restoration on *diciéndomelo* -> *decir*) must have parameterized unit tests.
- **In-Memory SQLite Verification**: Preprocessor and DAO queries should be tested against an in-memory or temporary file SQLite instance verifying schema parity between `sqlite-jdbc` (build-time) and Android `Room` (runtime).
