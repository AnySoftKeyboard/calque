package com.anysoftkeyboard.calque.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anysoftkeyboard.calque.data.room.dao.CognateRulesDao
import com.anysoftkeyboard.calque.data.room.dao.CuratedLessonsDao
import com.anysoftkeyboard.calque.data.room.dao.DictionaryDao
import com.anysoftkeyboard.calque.data.room.dao.FalseCognatesDao
import com.anysoftkeyboard.calque.data.room.entities.CognateRuleEntity
import com.anysoftkeyboard.calque.data.room.entities.CuratedLessonEntity
import com.anysoftkeyboard.calque.data.room.entities.DictionaryEntity
import com.anysoftkeyboard.calque.data.room.entities.FalseCognateEntity

/** Room database holding pre-packaged read-only seed tables for Calque. */
@Database(
    entities =
        [
            DictionaryEntity::class,
            FalseCognateEntity::class,
            CuratedLessonEntity::class,
            CognateRuleEntity::class,
        ],
    version = 1,
    exportSchema = false,
)
abstract class CalqueSeedDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao

    abstract fun falseCognatesDao(): FalseCognatesDao

    abstract fun curatedLessonsDao(): CuratedLessonsDao

    abstract fun cognateRulesDao(): CognateRulesDao

    companion object {
        fun build(context: Context, databaseName: String = "seed.db"): CalqueSeedDatabase {
            return Room.databaseBuilder(context, CalqueSeedDatabase::class.java, databaseName)
                .createFromAsset("databases/seed.db")
                .fallbackToDestructiveMigration(true)
                .build()
        }

        fun inMemory(context: Context): CalqueSeedDatabase {
            return Room.inMemoryDatabaseBuilder(context, CalqueSeedDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }
}
