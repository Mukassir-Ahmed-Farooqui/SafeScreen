package com.example.safescreen.monitoring

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.safescreen.enforcement.BlockerAccessibilityService

data class PermissionState(
    val usageAccess: Boolean,
    val accessibility: Boolean,
)

/** Reads the two special accesses SafeScreen needs and deep-links to the right system screens. */
class PermissionChecker(private val context: Context) {

    fun read() = PermissionState(
        usageAccess = hasUsageAccess(),
        accessibility = isBlockingServiceEnabled(),
    )

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
        fun openUsageAccessSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }

        fun openAccessibilitySettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
