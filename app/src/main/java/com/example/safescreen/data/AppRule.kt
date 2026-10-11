package com.example.safescreen.data

data class AppRule(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean = false,
    val dailyLimitMinutes: Int = 0
)
