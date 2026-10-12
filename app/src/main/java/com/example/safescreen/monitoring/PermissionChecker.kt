package com.example.safescreen.monitoring

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.example.safescreen.enforcement.BlockerAccessibilityService

data class PermissionState(
    val usageAccess: Boolean,
    val accessibility: Boolean,
    val notifications: Boolean = true,
)

/**
 * Reads the accesses SafeScreen needs and deep-links to the most specific
 * system screen each device will accept.
 *
 * Android deliberately does not let an app grant Usage Access or Accessibility
 * to itself, so the best possible experience is: explain, jump straight to the
 * right switch, detect the change when the parent comes back.
 */
class PermissionChecker(private val context: Context) {

    fun read() = PermissionState(
        usageAccess = hasUsageAccess(),
        accessibility = isBlockingServiceEnabled(),
        notifications = hasNotificationAccess(),
    )

    fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isBlockingServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        if (enabled.isEmpty()) return false
        val component = ComponentName(context, BlockerAccessibilityService::class.java)
        val accepted = setOf(component.flattenToString(), component.flattenToShortString())
        return enabled.split(':').any { entry -> accepted.any { it.equals(entry, ignoreCase = true) } }
    }

    companion object {
        /** Opens SafeScreen's own Usage Access page when the device supports it, else the full list. */
        fun openUsageAccessSettings(context: Context) {
            startFirstThatWorks(
                context,
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            )
        }

        /**
         * Tries, in order: the service's own detail page, the accessibility list
         * with SafeScreen highlighted, and finally the plain accessibility list.
         */
        fun openAccessibilitySettings(context: Context) {
            val component = ComponentName(context, BlockerAccessibilityService::class.java).flattenToString()
            val detail = Intent("android.settings.ACCESSIBILITY_DETAIL_SETTINGS")
                .putExtra("android.provider.extra.ACCESSIBILITY_SERVICE_COMPONENT_NAME", component)
            val highlighted = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .putExtra(":settings:fragment_args_key", component)
                .putExtra(
                    ":settings:show_fragment_args",
                    Bundle().apply { putString(":settings:fragment_args_key", component) },
                )
            startFirstThatWorks(context, detail, highlighted, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        fun openNotificationSettings(context: Context) {
            startFirstThatWorks(
                context,
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
            )
        }

        /** App info page: where "Allow restricted settings" lives for sideloaded apps on Android 13+. */
        fun openAppInfo(context: Context) {
            startFirstThatWorks(
                context,
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        }

        private fun startFirstThatWorks(context: Context, vararg intents: Intent) {
            for (intent in intents) {
                try {
                    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return
                } catch (_: ActivityNotFoundException) {
                    // try the next, less specific screen
                } catch (_: SecurityException) {
                    // some OEM screens refuse extras; fall through to the next
                }
            }
        }
    }
}
