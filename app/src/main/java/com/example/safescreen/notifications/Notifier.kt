package com.example.safescreen.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.safescreen.MainActivity
import com.example.safescreen.R
import com.example.safescreen.domain.Verdict
import kotlin.math.ceil

/**
 * Local notifications only. Three kinds:
 *  - blocked / limit reached (one per app, updated in place, never stacked)
 *  - five minutes left (sent once per app per day by the accessibility service)
 *  - protection off: fired the moment the blocking service is switched off
 *
 * [alertsEnabled] is the parent's in-app switch for the first two kinds. The
 * protection-off warning ignores it on purpose: someone turning blocking off
 * is exactly what a parent needs to hear about.
 */
class Notifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    @Volatile
    var alertsEnabled: Boolean = true

    init {
        manager.createNotificationChannelsCompat(
            listOf(
                NotificationChannelCompat.Builder(CHANNEL_ALERTS, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName("App alerts")
                    .setDescription("When an app is blocked or almost out of time")
                    .build(),
                NotificationChannelCompat.Builder(CHANNEL_PROTECTION, NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName("Protection status")
                    .setDescription("Warns when blocking is switched off")
                    .build(),
            ),
        )
    }

    fun notifyBlocked(appName: String, packageName: String, verdict: Verdict) {
        if (!alertsEnabled) return
        val title = when (verdict) {
            Verdict.BLOCKED_BY_RULE -> "$appName was blocked"
            Verdict.DAILY_LIMIT_REACHED -> "$appName reached its daily limit"
            Verdict.ALLOW -> return
        }
        post(
            id = "blocked:$packageName".hashCode(),
            channel = CHANNEL_ALERTS,
            title = title,
            text = "Someone just tried to open it. Tap to review the rules.",
        )
    }

    fun notifyLowTime(appName: String, packageName: String, remainingMillis: Long) {
        if (!alertsEnabled) return
        val minutes = ceil(remainingMillis / 60_000.0).toInt().coerceAtLeast(1)
        post(
            id = "low:$packageName".hashCode(),
            channel = CHANNEL_ALERTS,
            title = "$appName: $minutes min left today",
            text = "It will be blocked when the time is up.",
        )
    }

    fun notifyProtectionOff() {
        post(
            id = ID_PROTECTION,
            channel = CHANNEL_PROTECTION,
            title = "Blocking is switched off",
            text = "SafeScreen can't enforce any rules until the blocking service is turned back on. Tap to fix it.",
            autoCancel = false,
        )
    }

    fun cancelProtectionOff() = manager.cancel(ID_PROTECTION)

    fun sendTest() {
        post(
            id = ID_TEST,
            channel = CHANNEL_ALERTS,
            title = "SafeScreen alerts are working",
            text = "You'll see messages like this when an app is blocked or almost out of time.",
        )
    }

    @SuppressLint("MissingPermission") // guarded by areNotificationsEnabled()
    private fun post(id: Int, channel: String, title: String, text: String, autoCancel: Boolean = true) {
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent())
            .setAutoCancel(autoCancel)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        manager.notify(id, notification)
    }

    private fun openAppIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val CHANNEL_ALERTS = "alerts"
        const val CHANNEL_PROTECTION = "protection"
        const val ID_PROTECTION = 1001
        const val ID_TEST = 1002
    }
}
