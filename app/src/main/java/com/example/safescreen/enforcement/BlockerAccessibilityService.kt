package com.example.safescreen.enforcement

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.example.safescreen.SafeScreenApp
import com.example.safescreen.data.AppRule
import com.example.safescreen.data.hasLimit
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.RestrictionPolicy
import com.example.safescreen.domain.UsageWarnings
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
 * Timing: while a limited app stays open it is re-checked every 15s, and every
 * 5s during the last 90s, so the block lands within seconds of the limit.
 * Warnings (10 / 5 / 1 minutes left) are a notification plus a toast, once each
 * per app per day.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val container get() = (application as SafeScreenApp).container

    @Volatile private var rules: Map<String, AppRule> = emptyMap()
    private var ignoredPackages: Set<String> = setOf("com.android.systemui")
    private var watchJob: Job? = null
    private var lastBlock: Pair<String, Long>? = null
    private val announced = ConcurrentHashMap<String, Pair<LocalDate, Set<Int>>>()

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
                val wait = enforce(pkg) ?: break
                delay(wait)
            }
        }
    }

    /** Returns how long to wait before checking again, or null when watching this app can stop. */
    private suspend fun enforce(pkg: String): Long? {
        val rule = rules[pkg] ?: return null
        val usedMillis = if (rule.hasLimit && !rule.isBlocked && container.permissions.hasUsageAccess()) {
            container.usageMonitor.todayUsageMillis()[pkg] ?: 0L
        } else {
            0L
        }
        val decision = RestrictionPolicy.evaluate(rule, usedMillis)
        if (!decision.isRestricted) {
            warnIfNeeded(rule, decision)
            return nextCheckDelay(decision.remainingMillis)
        }
        withContext(Dispatchers.Main) {
            showBlockScreen(rule, decision)
            // Background activity starts can be silently refused; make sure the app is covered anyway.
            delay(FALLBACK_CHECK_MS)
            if (!BlockedActivity.isVisible) performGlobalAction(GLOBAL_ACTION_HOME)
        }
        return null
    }

    private fun nextCheckDelay(remainingMillis: Long?): Long = when {
        remainingMillis == null -> RECHECK_MS
        remainingMillis <= FINAL_STRETCH_MS -> minOf(FINAL_RECHECK_MS, remainingMillis).coerceAtLeast(1_000L)
        else -> RECHECK_MS
    }

    private suspend fun warnIfNeeded(rule: AppRule, decision: RestrictionDecision) {
        val remaining = decision.remainingMillis ?: return
        val today = LocalDate.now()
        val before = announced[rule.packageName]?.takeIf { it.first == today }?.second.orEmpty()
        val step = UsageWarnings.next(remaining, decision.limitMinutes, before)
        announced[rule.packageName] = today to step.announced
        val minutes = step.threshold ?: return

        container.notifier.notifyTimeLeft(rule.appName, rule.packageName, minutes)
        withContext(Dispatchers.Main) {
            Toast.makeText(
                applicationContext,
                "${rule.appName}: ${UsageWarnings.describe(minutes)} left today",
                Toast.LENGTH_LONG,
            ).show()
        }
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

    /** Called when the service is switched off (by anyone): say so loudly. */
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
        const val FINAL_STRETCH_MS = 90_000L
        const val FINAL_RECHECK_MS = 5_000L
        const val DEBOUNCE_MS = 1_500L
        const val FALLBACK_CHECK_MS = 700L
    }
}
