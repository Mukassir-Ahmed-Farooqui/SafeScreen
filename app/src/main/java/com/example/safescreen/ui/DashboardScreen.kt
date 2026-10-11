package com.example.safescreen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.safescreen.data.AppRule
import com.example.safescreen.ui.theme.SafeScreenTheme

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    rules: List<AppRule> = listOf(
        AppRule("com.instagram.android", "Instagram", dailyLimitMinutes = 60),
        AppRule("com.zhiliaoapp.musically", "TikTok", isBlocked = true),
        AppRule("com.google.android.youtube", "YouTube", dailyLimitMinutes = 120)
    )
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "SafeScreen",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (rules.isEmpty()) {
            Text(
                text = "No app rules yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            rules.forEach { rule ->
                RuleCard(rule = rule)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    SafeScreenTheme {
        DashboardScreen()
    }
}
