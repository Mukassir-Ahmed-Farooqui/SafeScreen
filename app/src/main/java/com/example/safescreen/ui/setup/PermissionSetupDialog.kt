package com.example.safescreen.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.safescreen.monitoring.PermissionChecker
import com.example.safescreen.monitoring.PermissionState
import com.example.safescreen.ui.components.WarningColor

/**
 * Guided "allow everything" pop-up. Android will not let an app grant these
 * accesses to itself, so each button jumps straight to the right switch and
 * the dialog updates by itself when the parent comes back (the host screen
 * re-reads permissions on resume and passes the new [permissions] in).
 */
@Composable
fun PermissionSetupDialog(
    permissions: PermissionState,
    onChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onChanged() }
    val doneCount = listOf(permissions.usageAccess, permissions.accessibility, permissions.notifications).count { it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow SafeScreen to protect this phone") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "$doneCount of 3 allowed. Each Allow button opens the right Android screen. " +
                        "Switch SafeScreen on, then come back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StepRow(
                    number = 1,
                    title = "Usage access",
                    body = "Lets SafeScreen measure how long each app is used.",
                    done = permissions.usageAccess,
                    onAllow = { PermissionChecker.openUsageAccessSettings(context) },
                )
                StepRow(
                    number = 2,
                    title = "Blocking service",
                    body = "Lets SafeScreen notice which app is open and cover restricted ones.",
                    done = permissions.accessibility,
                    onAllow = { PermissionChecker.openAccessibilitySettings(context) },
                )
                StepRow(
                    number = 3,
                    title = "Notifications",
                    body = "Alerts when an app is blocked, almost out of time, or blocking is switched off.",
                    done = permissions.notifications,
                    onAllow = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotifications) {
                            askedNotifications = true
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            PermissionChecker.openNotificationSettings(context)
                        }
                    },
                )
                if (!permissions.accessibility) {
                    RestrictedSettingsHint(onOpenAppInfo = { PermissionChecker.openAppInfo(context) })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
}

@Composable
private fun StepRow(number: Int, title: String, body: String, done: Boolean, onAllow: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Allowed", tint = scheme.primary)
            } else {
                Text(number.toString(), color = scheme.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        if (!done) FilledTonalButton(onClick = onAllow) { Text("Allow") }
    }
}

/** Android 13+ greys out the accessibility switch for apps installed outside the Play Store. */
@Composable
private fun RestrictedSettingsHint(onOpenAppInfo: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = WarningColor.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Switch greyed out?", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Android protects apps installed outside the Play Store. Open App info, tap the three dots at the " +
                    "top right, choose Allow restricted settings, then try step 2 again.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onOpenAppInfo) { Text("Open App info") }
        }
    }
}
