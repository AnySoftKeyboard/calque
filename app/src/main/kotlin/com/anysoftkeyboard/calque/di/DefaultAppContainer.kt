package com.anysoftkeyboard.calque.di

import com.anysoftkeyboard.calque.data.CalqueWordLookupProvider
import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.seed.CuratedLessonsRepository
import com.anysoftkeyboard.calque.data.seed.DictionaryRepository
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.data.seed.SeedDatabase
import com.anysoftkeyboard.calque.data.user.UserCustomTextRepository
import com.anysoftkeyboard.calque.data.user.UserDatabase
import com.anysoftkeyboard.calque.data.user.UserWordStateRepository
import com.anysoftkeyboard.calque.linguistics.WordLookupProvider
import com.anysoftkeyboard.calque.reader.ReaderSessionEngine
import com.anysoftkeyboard.calque.reader.ReadingSource
import com.anysoftkeyboard.calque.toolbox.CliticSurgeryEngine
import com.anysoftkeyboard.calque.toolbox.NumberOdometerEngine
import com.anysoftkeyboard.calque.toolbox.OmniBarEngine
import com.anysoftkeyboard.calque.toolbox.TenseShifterEngine
import com.anysoftkeyboard.calque.toolbox.TrapDetectorEngine
import com.anysoftkeyboard.calque.toolbox.WordTransformerEngine
import com.anysoftkeyboard.calque.ui.nav.NavStateManager
import java.io.File
import java.io.InputStream
import java.sql.Connection

/** Default implementation of [AppContainer] managing database connections and engine lifecycle. */
class DefaultAppContainer(
    databaseDir: File? = null,
    assetProvider: ((String) -> InputStream?)? = null,
    seedConnectionSupplier: (() -> Connection)? = null,
    userConnectionSupplier: (() -> Connection)? = null,
) : AppContainer {

    private val seedDatabase: SeedDatabase? =
        initSeedDatabase(databaseDir, assetProvider, seedConnectionSupplier)

    private val userDatabase: UserDatabase? = initUserDatabase(databaseDir, userConnectionSupplier)

    override val dictionaryRepository: DictionaryRepository =
        seedDatabase?.dictionary ?: InMemoryDictionaryRepository()

    override val falseCognatesRepository: FalseCognatesRepository =
        seedDatabase?.falseCognates ?: InMemoryFalseCognatesRepository()

    override val curatedLessonsRepository: CuratedLessonsRepository =
        seedDatabase?.curatedLessons ?: InMemoryCuratedLessonsRepository()

    override val cognateRulesRepository: CognateRulesRepository =
        seedDatabase?.cognateRules ?: InMemoryCognateRulesRepository()

    override val userWordStateRepository: UserWordStateRepository =
        userDatabase?.wordStates ?: InMemoryUserWordStateRepository()

    override val userCustomTextRepository: UserCustomTextRepository =
        userDatabase?.customTexts ?: InMemoryUserCustomTextRepository()

    override val wordLookupProvider: WordLookupProvider =
        CalqueWordLookupProvider(
            dictionaryRepository = dictionaryRepository,
            falseCognatesRepository = falseCognatesRepository,
            userWordStateRepository = userWordStateRepository,
        )

    override val trapDetectorEngine: TrapDetectorEngine = TrapDetectorEngine

    override val wordTransformerEngine: WordTransformerEngine = WordTransformerEngine

    override val tenseShifterEngine: TenseShifterEngine = TenseShifterEngine

    override val numberOdometerEngine: NumberOdometerEngine = NumberOdometerEngine

    override val cliticSurgeryEngine: CliticSurgeryEngine = CliticSurgeryEngine

    override val omniBarEngine: OmniBarEngine =
        OmniBarEngine(
            cognateRulesRepository = cognateRulesRepository,
            falseCognatesRepository = falseCognatesRepository,
            wordLookupProvider = wordLookupProvider,
        )

    override val navStateManager: NavStateManager = NavStateManager()

    override fun createReaderSession(source: ReadingSource): ReaderSessionEngine {
        return ReaderSessionEngine(
            source = source,
            lookupProvider = wordLookupProvider,
            cognateRulesRepository = cognateRulesRepository,
            userWordStateRepository = userWordStateRepository,
        )
    }

    companion object {
        private fun initSeedDatabase(
            dir: File?,
            assetProvider: ((String) -> InputStream?)?,
            connectionSupplier: (() -> Connection)?,
        ): SeedDatabase? {
            return runCatching {
                    if (connectionSupplier != null) {
                        return@runCatching SeedDatabase(connectionSupplier())
                    }
                    if (dir != null && assetProvider != null) {
                        val dbFile = File(dir, "seed.db")
                        if (!dbFile.exists()) {
                            dir.mkdirs()
                            assetProvider("databases/seed.db")?.use { input ->
                                dbFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                        if (dbFile.exists()) {
                            return@runCatching SeedDatabase.open(
                                "jdbc:sqlite:${dbFile.absolutePath}"
                            )
                        }
                    }
                    null
                }
                .getOrNull()
        }

        private fun initUserDatabase(
            dir: File?,
            connectionSupplier: (() -> Connection)?,
        ): UserDatabase? {
            return runCatching {
                    if (connectionSupplier != null) {
                        val db = UserDatabase(connectionSupplier())
                        UserDatabase.createTables(db.connection)
                        return@runCatching db
                    }
                    if (dir != null) {
                        dir.mkdirs()
                        val dbFile = File(dir, "user.db")
                        val db = UserDatabase.open("jdbc:sqlite:${dbFile.absolutePath}")
                        UserDatabase.createTables(db.connection)
                        return@runCatching db
                    }
                    null
                }
                .getOrNull()
        }
    }
}
