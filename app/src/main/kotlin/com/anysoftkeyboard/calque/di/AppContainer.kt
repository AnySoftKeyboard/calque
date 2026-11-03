package com.anysoftkeyboard.calque.di

import com.anysoftkeyboard.calque.data.seed.CognateRulesRepository
import com.anysoftkeyboard.calque.data.seed.CuratedLessonsRepository
import com.anysoftkeyboard.calque.data.seed.DictionaryRepository
import com.anysoftkeyboard.calque.data.seed.FalseCognatesRepository
import com.anysoftkeyboard.calque.data.user.UserCustomTextRepository
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

/** Dependency injection container providing all data repositories and domain engines for Calque. */
interface AppContainer {
    val dictionaryRepository: DictionaryRepository
    val falseCognatesRepository: FalseCognatesRepository
    val curatedLessonsRepository: CuratedLessonsRepository
    val cognateRulesRepository: CognateRulesRepository
    val userWordStateRepository: UserWordStateRepository
    val userCustomTextRepository: UserCustomTextRepository
    val wordLookupProvider: WordLookupProvider

    val trapDetectorEngine: TrapDetectorEngine
    val wordTransformerEngine: WordTransformerEngine
    val tenseShifterEngine: TenseShifterEngine
    val numberOdometerEngine: NumberOdometerEngine
    val cliticSurgeryEngine: CliticSurgeryEngine
    val omniBarEngine: OmniBarEngine

    val navStateManager: NavStateManager

    /** Creates a new active reading session for the specified [source]. */
    fun createReaderSession(source: ReadingSource): ReaderSessionEngine
}
