package com.example.safescreen.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safescreen.AppContainer
import com.example.safescreen.data.AppRule
import com.example.safescreen.data.effectiveLimitMinutes
import com.example.safescreen.monitoring.InstalledApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppItem(val app: InstalledApp, val rule: AppRule?) {
    val isBlocked: Boolean get() = rule?.isBlocked == true
    val limitMinutes: Int get() = rule?.effectiveLimitMinutes ?: 0
    val hasRule: Boolean get() = rule != null
}

enum class AppFilter { ALL, WITH_RULES }

data class AppsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val items: List<AppItem> = emptyList(),
)

class AppsViewModel(private val container: AppContainer) : ViewModel() {

    private val installed = MutableStateFlow<List<InstalledApp>?>(null)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(AppFilter.ALL)

    init {
        viewModelScope.launch { installed.value = container.installedApps.load() }
    }

    val uiState: StateFlow<AppsUiState> =
        combine(installed, container.ruleRepository.rules, query, filter) { apps, rules, q, f ->
            if (apps == null) {
                AppsUiState(loading = true, query = q, filter = f)
            } else {
                val byPackage = rules.associateBy { it.packageName }
                val items = apps.asSequence()
                    .map { AppItem(it, byPackage[it.packageName]) }
                    .filter { f == AppFilter.ALL || it.hasRule }
                    .filter { q.isBlank() || it.app.label.contains(q.trim(), ignoreCase = true) }
                    .toList()
                AppsUiState(loading = false, query = q, filter = f, items = items)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun onQueryChange(value: String) { query.value = value }
    fun onFilterChange(value: AppFilter) { filter.value = value }

    fun setBlocked(item: AppItem, blocked: Boolean) = save(item) { it.copy(isBlocked = blocked) }

    /** [minutes] is validated by the dialog (0..1440); the repository normalises again as a safety net. */
    fun setLimit(item: AppItem, minutes: Int) = save(item) { it.copy(dailyLimitMinutes = minutes) }

    private fun save(item: AppItem, change: (AppRule) -> AppRule) {
        viewModelScope.launch {
            val base = item.rule ?: AppRule(packageName = item.app.packageName, appName = item.app.label)
            container.ruleRepository.save(change(base))
        }
    }
}
