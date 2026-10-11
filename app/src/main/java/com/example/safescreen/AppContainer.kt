package com.example.safescreen

import android.content.Context
import com.example.safescreen.data.AppRuleRepository
import com.example.safescreen.data.SafeScreenDatabase
import com.example.safescreen.data.SettingsRepository
import com.example.safescreen.data.safeScreenDataStore
import com.example.safescreen.monitoring.InstalledAppsProvider
import com.example.safescreen.monitoring.PermissionChecker
import com.example.safescreen.monitoring.UsageMonitor
import com.example.safescreen.security.PinManager
import com.example.safescreen.security.SessionManager

/**
 * Hand-rolled dependency container. Small enough that Hilt would add more
 * ceremony than value; every dependency is created once and shared.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database by lazy { SafeScreenDatabase.build(appContext) }

    val ruleRepository by lazy { AppRuleRepository(database.appRuleDao()) }
    val settingsRepository = SettingsRepository(appContext.safeScreenDataStore)
    val pinManager = PinManager(appContext.safeScreenDataStore)
    val sessionManager = SessionManager()
    val usageMonitor = UsageMonitor(appContext)
    val installedApps = InstalledAppsProvider(appContext)
    val permissions = PermissionChecker(appContext)
}
