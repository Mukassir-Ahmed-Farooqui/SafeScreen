package com.example.safescreen.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads raw usage events and hands them to [UsageSessions] to compute today's
 * foreground time per package.
 *
 * Counting starts at local midnight, or at [baselineMillis] if the parent has
 * pressed "Reset today's usage" since then.
 *
 * Returns an empty map when Usage Access is not granted: callers must treat
 * that as "unknown", never as "zero usage".
 */
class UsageMonitor(
    context: Context,
    private val baselineMillis: suspend () -> Long = { 0L },
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    suspend fun todayUsageMillis(): Map<String, Long> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val start = maxOf(startOfDay, baselineMillis())
        try {
            UsageSessions.totals(readEvents(start, now), now)
        } catch (_: SecurityException) {
            emptyMap()
        }
    }

    private fun readEvents(start: Long, end: Long): List<RawUsageEvent> {
        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val list = ArrayList<RawUsageEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            list += RawUsageEvent(event.eventType, event.packageName, event.timeStamp)
        }
        return list
    }
}
