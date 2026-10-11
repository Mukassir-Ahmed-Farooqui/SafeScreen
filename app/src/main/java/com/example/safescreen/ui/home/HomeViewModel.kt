package com.example.safescreen.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safescreen.AppContainer
import com.example.safescreen.data.AppRule
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.RestrictionPolicy
import com.example.safescreen.monitoring.PermissionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class RuleUsage(
    val rule: AppRule,
    val usedMillis: Long,
    val decision: RestrictionDecision,
)

data class HomeUiState(
    val loading: Boolean = true,
    val permissions: PermissionState = PermissionState(usageAccess = false, accessibility = false),
    val rules: List<RuleUsage> = emptyList(),
) {
    val restrictedCount: Int get() = rules.count { it.decision.isRestricted }
    val setupComplete: Boolean get() = permissions.usageAccess && permissions.accessibility
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private data class Snapshot(val permissions: PermissionState, val usage: Map<String, Long>)

    private val refreshKey = MutableStateFlow(0)

    /** Re-reads permissions and usage every 30s while the screen is visible, and on demand via [refresh]. */
    private val snapshots: Flow<Snapshot> = refreshKey.flatMapLatest {
        flow {
            while (true) {
                val permissions = container.permissions.read()
                val usage = if (permissions.usageAccess) container.usageMonitor.todayUsageMillis() else emptyMap()
                emit(Snapshot(permissions, usage))
                delay(POLL_MS)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(container.ruleRepository.rules, snapshots) { rules, snapshot ->
            val items = rules
                .map { rule ->
                    val used = snapshot.usage[rule.packageName] ?: 0L
                    RuleUsage(rule, used, RestrictionPolicy.evaluate(rule, used))
                }
                .sortedWith(
                    compareByDescending<RuleUsage> { it.decision.isRestricted }
                        .thenByDescending { it.usedMillis },
                )
            HomeUiState(loading = false, permissions = snapshot.permissions, rules = items)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refresh() {
        refreshKey.value += 1
    }

    private companion object {
        const val POLL_MS = 30_000L
    }
}
