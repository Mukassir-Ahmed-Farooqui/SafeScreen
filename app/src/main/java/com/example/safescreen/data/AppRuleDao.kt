package com.example.safescreen.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY appName COLLATE NOCASE")
    fun observeAll(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun find(packageName: String): AppRule?

    @Upsert
    suspend fun upsert(rule: AppRule)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}
