package com.example.safescreen.ui.restriction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.Verdict
import com.example.safescreen.ui.components.WarningColor
import com.example.safescreen.ui.components.formatDuration
import com.example.safescreen.ui.components.formatLimit

/** The screen a child sees. Shared by the real [com.example.safescreen.enforcement.BlockedActivity] and the in-app preview. */
@Composable
fun RestrictionContent(
    appName: String,
    decision: RestrictionDecision,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val icon = when (decision.verdict) {
        Verdict.BLOCKED_BY_RULE -> Icons.Filled.Lock
        Verdict.DAILY_LIMIT_REACHED -> Icons.Filled.Warning
        Verdict.ALLOW -> Icons.Filled.CheckCircle
    }
    val color = when (decision.verdict) {
        Verdict.BLOCKED_BY_RULE -> scheme.error
        Verdict.DAILY_LIMIT_REACHED -> WarningColor
        Verdict.ALLOW -> scheme.primary
    }
    val title = when (decision.verdict) {
        Verdict.BLOCKED_BY_RULE -> "$appName is blocked"
        Verdict.DAILY_LIMIT_REACHED -> "Daily limit reached"
        Verdict.ALLOW -> "$appName is allowed right now"
    }
    val body = when (decision.verdict) {
        Verdict.BLOCKED_BY_RULE ->
            "A parent turned this app off. Ask them to change the rule in SafeScreen."
        Verdict.DAILY_LIMIT_REACHED ->
            "You've used ${formatDuration(decision.usedMillis)} of ${formatLimit(decision.limitMinutes)} today. " +
                "$appName unlocks again at midnight."
        Verdict.ALLOW -> {
            val remaining = decision.remainingMillis
            if (remaining != null) "${formatDuration(remaining)} left of today's ${formatLimit(decision.limitMinutes)}."
            else "No rule is stopping this app at the moment."
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(44.dp)) }

        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
        Spacer(Modifier.height(20.dp))
        Text(
            "SafeScreen applies rules through an accessibility service while it is switched on.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
