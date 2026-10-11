package com.example.safescreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.safescreen.ui.SafeScreenRoot
import com.example.safescreen.ui.theme.PtyxisPalette
import com.example.safescreen.ui.theme.SafeScreenTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as SafeScreenApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val palette by container.settingsRepository.palette
                .collectAsStateWithLifecycle(initialValue = PtyxisPalette.COBALT_NEON)
            SafeScreenTheme(palette = palette) {
                SafeScreenRoot()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.sessionManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        container.sessionManager.onAppBackgrounded()
    }
}
