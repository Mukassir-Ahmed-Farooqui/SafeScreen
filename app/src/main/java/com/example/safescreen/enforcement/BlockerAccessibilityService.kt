package com.example.safescreen.enforcement

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.safescreen.SafeScreenApp
import com.example.safescreen.data.AppRule
import com.example.safescreen.data.hasLimit
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.RestrictionPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Notices which app just came to the foreground and, if the policy says it is
 * restricted, covers it with [BlockedActivity].
 *
 * Privacy: the service config sets canRetrieveWindowContent=false, so it can
 * see *which* app is open but never what is on screen or what is typed.
 *
 * Limits are re-checked every [RECHECK_MS] while a limited app stays open,
 * because no window event fires when the allowance simply runs out.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container get() = (application as SafeScreenApp).container

    @Volatile private var rules: Map<String, AppRule> = emptyMap()
    private var ignoredPackages: Set<String> = setOf("com.android.systemui")
    private var watchJob: Job? = null
    private var lastBlock: Pair<String, Long>? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        ignoredPackages = buildSet {
            add("com.android.systemui")
            // Keyboards open their own window while the real app is still in front.
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.enabledInputMethodList.forEach { add(it.packageName) }
        }
        scope.launch {
            container.ruleRepository.rules.collect { list ->
                rules = list.associateBy { it.packageName }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in ignoredPackages) return

        watchJob?.cancel()
        if (!rules.containsKey(pkg)) return

        watchJob = scope.launch {
            while (isActive) {
                if (enforce(pkg)) break
                delay(RECHECK_MS)
            }
        }
    }

    /** Returns true when watching this app can stop (it was blocked, or its rule is gone). */
    private suspend fun enforce(pkg: String): Boolean {
        val rule = rules[pkg] ?: return true
        val usedMillis = if (rule.hasLimit && !rule.isBlocked && container.permissions.hasUsageAccess()) {
            container.usageMonitor.todayUsageMillis()[pkg] ?: 0L
        } else {
            0L
        }
        val decision = RestrictionPolicy.evaluate(rule, usedMillis)
        if (!decision.isRestricted) return false
        withContext(Dispatchers.Main) { showBlockScreen(rule, decision) }
        return true
    }

    private fun showBlockScreen(rule: AppRule, decision: RestrictionDecision) {
        val now = System.currentTimeMillis()
        val previous = lastBlock
        if (previous != null && previous.first == rule.packageName && now - previous.second < DEBOUNCE_MS) return
        lastBlock = rule.packageName to now

        runCatching {
            startActivity(BlockedActivity.intent(this, rule.appName, decision))
        }.onFailure {
            // If Android refuses the background launch, at least push the app away.
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val RECHECK_MS = 15_000L
        const val DEBOUNCE_MS = 1_500L
    }
}
