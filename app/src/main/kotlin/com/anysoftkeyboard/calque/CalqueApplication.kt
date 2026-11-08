package com.anysoftkeyboard.calque

import android.app.Application
import com.anysoftkeyboard.calque.data.room.CalqueSeedDatabase
import com.anysoftkeyboard.calque.data.room.CalqueUserDatabase
import com.anysoftkeyboard.calque.di.AppContainer
import com.anysoftkeyboard.calque.di.DefaultAppContainer

/** Application class providing access to the shared [AppContainer]. */
class CalqueApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val seedDb = CalqueSeedDatabase.build(this)
        val userDb = CalqueUserDatabase.build(this)
        appContainer =
            DefaultAppContainer(
                seedRoomDatabase = seedDb,
                userRoomDatabase = userDb,
            )
    }
}
