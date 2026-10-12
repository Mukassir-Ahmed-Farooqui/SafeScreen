package com.example.safescreen.enforcement

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.example.safescreen.SafeScreenApp
import com.example.safescreen.data.AppRule
import com.example.safescreen.data.hasLimit
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.RestrictionPolicy
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
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
 * Extras over the basic version:
 *  - limits are re-checked every [RECHECK_MS] while a limited app stays open;
 *  - if Android refuses to start the block screen from the background, the app
 *    is pushed to the Home screen instead (checked [FALLBACK_CHECK_MS] later);
 *  - a notification fires when blocking is blocked/limit is nearly used up,
 *    and when this service itself is switched off.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container get() = (application as SafeScreenApp).container

    @Volatile private var rules: Map<String, AppRule> = emptyMap()
    private var ignoredPackages: Set<String> = setOf("com.android.systemui")
    private var watchJob: Job? = null
    private var lastBlock: Pair<String, Long>? = null
    private val warnedToday = ConcurrentHashMap<String, LocalDate>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        container.notifier.cancelProtectionOff()
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
        if (!decision.isRestricted) {
            warnIfAlmostOut(rule, decision)
            return false
        }
        withContext(Dispatchers.Main) {
            showBlockScreen(rule, decision)
            // Background activity starts can be silently refused; make sure the app is covered anyway.
            delay(FALLBACK_CHECK_MS)
            if (!BlockedActivity.isVisible) performGlobalAction(GLOBAL_ACTION_HOME)
        }
        return true
    }

    private fun warnIfAlmostOut(rule: AppRule, decision: RestrictionDecision) {
        val remaining = decision.remainingMillis ?: return
        if (remaining <= 0L || remaining > LOW_TIME_MS) return
        val today = LocalDate.now()
        if (warnedToday.put(rule.packageName, today) == today) return
        container.notifier.notifyLowTime(rule.appName, rule.packageName, remaining)
    }

    private fun showBlockScreen(rule: AppRule, decision: RestrictionDecision) {
        val now = System.currentTimeMillis()
        val previous = lastBlock
        if (previous != null && previous.first == rule.packageName && now - previous.second < DEBOUNCE_MS) return
        lastBlock = rule.packageName to now

        container.notifier.notifyBlocked(rule.appName, rule.packageName, decision.verdict)
        runCatching {
            startActivity(BlockedActivity.intent(this, rule.appName, decision))
        }.onFailure {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /** Called when the parent (or anyone) switches the service off: say so loudly. */
    override fun onUnbind(intent: Intent?): Boolean {
        container.notifier.notifyProtectionOff()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val RECHECK_MS = 15_000L
        const val DEBOUNCE_MS = 1_500L
        const val FALLBACK_CHECK_MS = 700L
        const val LOW_TIME_MS = 5 * 60_000L
    }
}
