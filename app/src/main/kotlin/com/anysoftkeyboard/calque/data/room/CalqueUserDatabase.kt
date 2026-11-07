package com.anysoftkeyboard.calque.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anysoftkeyboard.calque.data.room.dao.UserCustomTextDao
import com.anysoftkeyboard.calque.data.room.dao.UserWordStateDao
import com.anysoftkeyboard.calque.data.room.entities.UserCustomTextEntity
import com.anysoftkeyboard.calque.data.room.entities.UserWordStateEntity

/** Room database holding mutable user vocabulary learning and custom text tables. */
@Database(
    entities =
        [
            UserWordStateEntity::class,
            UserCustomTextEntity::class,
        ],
    version = 1,
    exportSchema = false,
)
abstract class CalqueUserDatabase : RoomDatabase() {
    abstract fun userWordStateDao(): UserWordStateDao

    abstract fun userCustomTextDao(): UserCustomTextDao

    companion object {
        fun build(context: Context, databaseName: String = "user.db"): CalqueUserDatabase {
            // Never fall back to destructive migration here: user.db holds the learner's vault and
            // progress. Schema bumps must ship an explicit Migration.
            return Room.databaseBuilder(context, CalqueUserDatabase::class.java, databaseName)
                .build()
        }
    }
}
