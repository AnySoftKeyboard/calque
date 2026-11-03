package com.anysoftkeyboard.calque

import android.app.Application
import com.anysoftkeyboard.calque.di.AppContainer
import com.anysoftkeyboard.calque.di.DefaultAppContainer
import java.io.File

/** Application class providing access to the shared [AppContainer]. */
class CalqueApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val dbDir = getDatabasePath("dummy.db").parentFile ?: File(filesDir, "databases")
        appContainer =
            DefaultAppContainer(
                databaseDir = dbDir,
                assetProvider = { path -> runCatching { assets.open(path) }.getOrNull() },
            )
    }
}
