package com.example.safescreen.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safescreen.domain.Verdict
import com.example.safescreen.monitoring.PermissionChecker
import com.example.safescreen.monitoring.PermissionState
import com.example.safescreen.ui.components.AppIcon
import com.example.safescreen.ui.components.EmptyState
import com.example.safescreen.ui.components.SectionTitle
import com.example.safescreen.ui.components.SoftCard
import com.example.safescreen.ui.components.StatusChip
import com.example.safescreen.ui.components.UsageBar
import com.example.safescreen.ui.components.WarningColor
import com.example.safescreen.ui.components.formatDuration
import com.example.safescreen.ui.components.formatLimit
import com.example.safescreen.ui.containerFactory

@Composable
fun HomeScreen(
    onOpenApps: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = containerFactory { HomeViewModel(it) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("SafeScreen", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Today's overview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { StatusHero(state) }

        if (!state.loading && !state.setupComplete) {
            item {
                SetupCard(
                    permissions = state.permissions,
                    onGrantUsage = { PermissionChecker.openUsageAccessSettings(context) },
                    onEnableService = { PermissionChecker.openAccessibilitySettings(context) },
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Apps managed", state.rules.size.toString(), Modifier.weight(1f))
                StatCard("Restricted now", state.restrictedCount.toString(), Modifier.weight(1f))
            }
        }

        item { SectionTitle("Today") }

        if (!state.loading && state.rules.isEmpty()) {
            item {
                EmptyState(
                    title = "No rules yet",
                    body = "Choose apps to block or give them a daily time limit.",
                    actionLabel = "Choose apps",
                    onAction = onOpenApps,
                )
            }
        } else {
            items(state.rules, key = { it.rule.packageName }) { item ->
                RuleUsageCard(item, usageAvailable = state.permissions.usageAccess)
            }
        }
    }
}

private data class Hero(val icon: ImageVector, val color: Color, val title: String, val body: String)

@Composable
private fun StatusHero(state: HomeUiState) {
    val scheme = MaterialTheme.colorScheme
    val hero = when {
        state.loading -> Hero(Icons.Filled.Lock, scheme.primary, "Checking protection", "One moment.")
        !state.permissions.accessibility -> Hero(
            Icons.Filled.Warning, WarningColor, "Blocking is off",
            "Turn on the SafeScreen service so rules can be enforced.",
        )
        state.rules.isEmpty() -> Hero(
            Icons.Filled.Lock, scheme.primary, "Ready for your first rule",
            "Blocking is on. Add an app to start.",
        )
        else -> Hero(
            Icons.Filled.CheckCircle, scheme.primary, "Protection is on",
            "${state.rules.size} apps managed, ${state.restrictedCount} restricted right now.",
        )
    }
    Surface(shape = RoundedCornerShape(28.dp), color = hero.color.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(hero.color.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) { Icon(hero.icon, contentDescription = null, tint = hero.color) }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(hero.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(hero.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SetupCard(
    permissions: PermissionState,
    onGrantUsage: () -> Unit,
    onEnableService: () -> Unit,
) {
    SoftCard {
        Text("Finish setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SetupRow(
                title = "Usage access",
                body = "Lets SafeScreen measure how long each app is used today.",
                done = permissions.usageAccess,
                actionLabel = "Grant",
                onAction = onGrantUsage,
            )
            SetupRow(
                title = "Blocking service",
                body = "Lets SafeScreen notice which app is open and cover restricted ones.",
                done = permissions.accessibility,
                actionLabel = "Turn on",
                onAction = onEnableService,
            )
        }
    }
}

@Composable
private fun SetupRow(title: String, body: String, done: Boolean, actionLabel: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (done) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
        } else {
            FilledTonalButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RuleUsageCard(item: RuleUsage, usageAvailable: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val decision = item.decision
    val fraction = decision.usageFraction
    val (label, color) = when {
        decision.verdict == Verdict.BLOCKED_BY_RULE -> "Blocked" to scheme.error
        decision.verdict == Verdict.DAILY_LIMIT_REACHED -> "Limit reached" to scheme.error
        fraction != null && fraction >= 0.75f -> "Almost out" to WarningColor
        else -> "On track" to scheme.primary
    }
    val showBar = fraction != null && usageAvailable && !item.rule.isBlocked
    val detail = when {
        item.rule.isBlocked -> "Blocked all day"
        !usageAvailable -> "Grant usage access to see time used"
        else -> "${formatDuration(item.usedMillis)} of ${formatLimit(decision.limitMinutes)}"
    }

    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(item.rule.packageName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.rule.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            StatusChip(label, color)
        }
        if (showBar && fraction != null) {
            UsageBar(fraction = fraction, color = color, modifier = Modifier.padding(top = 12.dp))
            decision.remainingMillis?.let { remaining ->
                Text(
                    text = if (remaining > 0) "${formatDuration(remaining)} left today" else "No time left today",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}
