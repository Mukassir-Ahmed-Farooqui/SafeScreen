package com.example.safescreen.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safescreen.data.MAX_DAILY_LIMIT_MINUTES
import com.example.safescreen.ui.components.AppIcon
import com.example.safescreen.ui.components.EmptyState
import com.example.safescreen.ui.components.SoftCard
import com.example.safescreen.ui.components.formatDuration
import com.example.safescreen.ui.components.formatLimit
import com.example.safescreen.ui.containerFactory

@Composable
fun AppsScreen(
    onPreview: (packageName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = viewModel(factory = containerFactory { AppsViewModel(it) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var limitTarget by remember { mutableStateOf<AppItem?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "Apps",
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            placeholder = { Text("Search apps") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        )
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter == AppFilter.ALL,
                onClick = { viewModel.onFilterChange(AppFilter.ALL) },
                label = { Text("All apps") },
            )
            FilterChip(
                selected = state.filter == AppFilter.WITH_RULES,
                onClick = { viewModel.onFilterChange(AppFilter.WITH_RULES) },
                label = { Text("With rules") },
            )
        }

        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.items.isEmpty() -> EmptyState(
                title = if (state.filter == AppFilter.WITH_RULES && state.query.isBlank()) "No rules yet" else "No apps found",
                body = if (state.filter == AppFilter.WITH_RULES && state.query.isBlank()) {
                    "Switch to All apps and turn on Block, or set a daily limit."
                } else {
                    "Try a different name."
                },
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.app.packageName }) { item ->
                    AppRuleCard(
                        item = item,
                        onBlockedChange = { viewModel.setBlocked(item, it) },
                        onEditLimit = { limitTarget = item },
                        onPreview = { onPreview(item.app.packageName) },
                    )
                }
            }
        }
    }

    limitTarget?.let { target ->
        LimitDialog(
            appName = target.app.label,
            currentMinutes = target.limitMinutes,
            onDismiss = { limitTarget = null },
            onSave = { minutes ->
                viewModel.setLimit(target, minutes)
                limitTarget = null
            },
        )
    }
}

@Composable
private fun AppRuleCard(
    item: AppItem,
    onBlockedChange: (Boolean) -> Unit,
    onEditLimit: () -> Unit,
    onPreview: () -> Unit,
) {
    val used = item.usedMillis
    val subtitle = when {
        item.isBlocked -> "Blocked all day"
        item.limitMinutes > 0 && used != null ->
            "Limit ${formatLimit(item.limitMinutes)} a day, used ${formatDuration(used)} today"
        item.limitMinutes > 0 -> "Limit ${formatLimit(item.limitMinutes)} a day"
        else -> "No rules"
    }
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(item.app.packageName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.app.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = item.isBlocked, onCheckedChange = onBlockedChange)
                Text(
                    "Block all day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = onEditLimit,
                label = { Text(if (item.limitMinutes > 0) "Limit: ${formatLimit(item.limitMinutes)}" else "Set daily limit") },
            )
            if (item.hasRule) TextButton(onClick = onPreview) { Text("Preview screen") }
        }
    }
}

@Composable
private fun LimitDialog(
    appName: String,
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(if (currentMinutes > 0) currentMinutes.toString() else "") }
    val minutes = text.toIntOrNull()
    val isValid = text.isEmpty() || (minutes != null && minutes in 0..MAX_DAILY_LIMIT_MINUTES)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily limit for $appName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 60, 120).forEach { preset ->
                        FilterChip(
                            selected = minutes == preset,
                            onClick = { text = preset.toString() },
                            label = { Text(formatLimit(preset)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> if (value.length <= 4 && value.all(Char::isDigit)) text = value },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Minutes per day") },
                    isError = !isValid,
                    supportingText = {
                        Text(
                            if (isValid) "Leave empty for no limit." else "Enter 0 to $MAX_DAILY_LIMIT_MINUTES minutes.",
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = isValid, onClick = { onSave(minutes ?: 0) }) { Text("Save limit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
