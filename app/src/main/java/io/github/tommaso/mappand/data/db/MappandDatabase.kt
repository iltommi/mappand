package io.github.tommaso.mappand.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = false)
abstract class MappandDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile private var INSTANCE: MappandDatabase? = null

        fun getInstance(context: Context): MappandDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, MappandDatabase::class.java, "mappand.db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
