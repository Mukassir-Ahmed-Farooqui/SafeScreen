package com.example.safescreen.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Computes today's foreground time per package from raw usage *events*
 * (resume/pause pairs). Events are far more accurate than the pre-aggregated
 * buckets from queryUsageStats, which can overlap day boundaries.
 *
 * Returns an empty map when Usage Access is not granted: callers must treat
 * that as "unknown", never as "zero usage".
 */
class UsageMonitor(context: Context) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    suspend fun todayUsageMillis(): Map<String, Long> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        try {
            foregroundTotals(startOfDay, now)
        } catch (_: SecurityException) {
            emptyMap()
        }
    }

    @Suppress("DEPRECATION") // MOVE_TO_* are the only constants available below API 29.
    private fun foregroundTotals(start: Long, end: Long): Map<String, Long> {
        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val openSince = HashMap<String, Long>()
        val totals = HashMap<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> openSince.putIfAbsent(pkg, event.timeStamp)
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val since = openSince.remove(pkg) ?: continue
                    totals.merge(pkg, (event.timeStamp - since).coerceAtLeast(0L), Long::plus)
                }
            }
        }
        // Apps still in the foreground right now count up to this moment.
        openSince.forEach { (pkg, since) ->
            totals.merge(pkg, (end - since).coerceAtLeast(0L), Long::plus)
        }
        return totals
    }
}
