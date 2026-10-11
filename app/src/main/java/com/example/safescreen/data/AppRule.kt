package com.example.safescreen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val MAX_DAILY_LIMIT_MINUTES = 24 * 60

/**
 * One parental rule for one app.
 * A limit of 0 means "no limit". A rule that neither blocks nor limits is
 * never stored (see [AppRuleRepository.save]).
 */
@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val dailyLimitMinutes: Int = 0,
    val updatedAt: Long = 0L,
)

/** Limit after normalisation: negative -> 0 (no limit), oversized -> 24h. */
val AppRule.effectiveLimitMinutes: Int
    get() = dailyLimitMinutes.coerceIn(0, MAX_DAILY_LIMIT_MINUTES)

val AppRule.hasLimit: Boolean
    get() = effectiveLimitMinutes > 0

val AppRule.hasNoEffect: Boolean
    get() = !isBlocked && !hasLimit
