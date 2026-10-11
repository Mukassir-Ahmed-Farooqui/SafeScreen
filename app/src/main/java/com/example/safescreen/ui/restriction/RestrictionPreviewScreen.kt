package com.example.safescreen.ui.restriction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.safescreen.AppContainer
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.RestrictionPolicy
import com.example.safescreen.ui.containerFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PreviewState(val appName: String, val decision: RestrictionDecision)

class RestrictionPreviewViewModel(
    container: AppContainer,
    private val packageName: String,
) : ViewModel() {
    private val _state = MutableStateFlow<PreviewState?>(null)
    val state: StateFlow<PreviewState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val rule = container.ruleRepository.find(packageName)
            val usage = if (container.permissions.hasUsageAccess()) {
                container.usageMonitor.todayUsageMillis()[packageName] ?: 0L
            } else {
                0L
            }
            _state.value = PreviewState(
                appName = rule?.appName ?: packageName,
                decision = RestrictionPolicy.evaluate(rule, usage),
            )
        }
    }
}

/** Shows exactly what the child would see for this app's current rule and today's usage. */
@Composable
fun RestrictionPreviewScreen(
    packageName: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: RestrictionPreviewViewModel = viewModel(
        key = "preview-$packageName",
        factory = containerFactory { RestrictionPreviewViewModel(it, packageName) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Preview of what your child sees",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            RestrictionContent(
                appName = current.appName,
                decision = current.decision,
                actionLabel = "Close preview",
                onAction = onClose,
            )
        }
    }
}
