package com.example.safescreen.monitoring

/** One raw entry from UsageStatsManager.queryEvents, copied out so the logic below needs no Android classes. */
data class RawUsageEvent(val type: Int, val packageName: String?, val timestamp: Long)

/**
 * Turns a day of raw usage events into foreground time per app.
 *
 * Model: exactly one app is in the foreground at a time. A session ends when
 *  - that app is paused/stopped,
 *  - the screen turns off, the keyguard appears, or the device shuts down, or
 *  - ANOTHER app comes to the foreground (so a missing "paused" event can never
 *    make an app look open for hours; this was the overcounting risk).
 * A session still open at [endMillis] counts up to that moment.
 */
object UsageSessions {
    // Values of UsageEvents.Event.*; several only exist on newer Android, so they are named here.
    const val ACTIVITY_RESUMED = 1
    const val ACTIVITY_PAUSED = 2
    const val SCREEN_NON_INTERACTIVE = 16
    const val KEYGUARD_SHOWN = 17
    const val ACTIVITY_STOPPED = 23
    const val DEVICE_SHUTDOWN = 26

    fun totals(events: List<RawUsageEvent>, endMillis: Long): Map<String, Long> {
        val totals = HashMap<String, Long>()
        var current: String? = null
        var since = 0L

        fun closeSession(at: Long) {
            val pkg = current ?: return
            totals.merge(pkg, (at - since).coerceAtLeast(0L), Long::plus)
            current = null
        }

        for (event in events) {
            val pkg = event.packageName
            when (event.type) {
                ACTIVITY_RESUMED -> if (pkg != null && pkg != current) {
                    closeSession(event.timestamp)
                    current = pkg
                    since = event.timestamp
                }
                ACTIVITY_PAUSED, ACTIVITY_STOPPED ->
                    if (pkg != null && pkg == current) closeSession(event.timestamp)
                SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN, DEVICE_SHUTDOWN -> closeSession(event.timestamp)
            }
        }
        closeSession(endMillis)
        return totals
    }
}
