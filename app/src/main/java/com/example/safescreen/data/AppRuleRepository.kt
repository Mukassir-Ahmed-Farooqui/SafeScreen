package com.example.safescreen.data

import kotlinx.coroutines.flow.Flow

class AppRuleRepository(
    private val dao: AppRuleDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val rules: Flow<List<AppRule>> = dao.observeAll()

    suspend fun find(packageName: String): AppRule? = dao.find(packageName)

    /**
     * Saves a rule after normalising the limit. A rule that no longer does
     * anything (not blocked, no limit) is removed instead of stored.
     */
    suspend fun save(rule: AppRule) {
        val normalised = rule.copy(
            dailyLimitMinutes = rule.effectiveLimitMinutes,
            updatedAt = clock(),
        )
        if (normalised.hasNoEffect) {
            dao.delete(normalised.packageName)
        } else {
            dao.upsert(normalised)
        }
    }
}
