package com.example.safescreen.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppRule::class], version = 1, exportSchema = false)
abstract class SafeScreenDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao

    companion object {
        fun build(context: Context): SafeScreenDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                SafeScreenDatabase::class.java,
                "safescreen.db",
            ).build()
    }
}
