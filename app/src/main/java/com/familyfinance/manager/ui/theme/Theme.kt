package com.familyfinance.manager.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
@Composable fun FamilyFinanceTheme(content: @Composable () -> Unit) {
 MaterialTheme(colorScheme = lightColorScheme(primary = FinanceBlue), content = content)
}
