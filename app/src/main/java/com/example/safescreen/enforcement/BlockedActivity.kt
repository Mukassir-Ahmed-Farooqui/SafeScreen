package com.example.safescreen.enforcement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safescreen.SafeScreenApp
import com.example.safescreen.domain.RestrictionDecision
import com.example.safescreen.domain.Verdict
import com.example.safescreen.ui.restriction.RestrictionContent
import com.example.safescreen.ui.theme.PtyxisPalette
import com.example.safescreen.ui.theme.SafeScreenTheme

/** Full-screen "this app is restricted" page shown over the blocked app. Back and the button both go Home. */
class BlockedActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appName = intent.getStringExtra(EXTRA_APP_NAME).orEmpty()
        val verdict = runCatching { Verdict.valueOf(intent.getStringExtra(EXTRA_VERDICT).orEmpty()) }
            .getOrDefault(Verdict.BLOCKED_BY_RULE)
        val decision = RestrictionDecision(
            verdict = verdict,
            usedMillis = intent.getLongExtra(EXTRA_USED_MS, 0L),
            limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MIN, 0),
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        val container = (application as SafeScreenApp).container
        setContent {
            val palette by container.settingsRepository.palette
                .collectAsStateWithLifecycle(initialValue = PtyxisPalette.COBALT_NEON)
            SafeScreenTheme(palette = palette) {
                RestrictionContent(
                    appName = appName,
                    decision = decision,
                    actionLabel = "Go to home screen",
                    onAction = ::goHome,
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    companion object {
        private const val EXTRA_APP_NAME = "app_name"
        private const val EXTRA_VERDICT = "verdict"
        private const val EXTRA_USED_MS = "used_ms"
        private const val EXTRA_LIMIT_MIN = "limit_min"

        fun intent(context: Context, appName: String, decision: RestrictionDecision): Intent =
            Intent(context, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_APP_NAME, appName)
                .putExtra(EXTRA_VERDICT, decision.verdict.name)
                .putExtra(EXTRA_USED_MS, decision.usedMillis)
                .putExtra(EXTRA_LIMIT_MIN, decision.limitMinutes)
    }
}
