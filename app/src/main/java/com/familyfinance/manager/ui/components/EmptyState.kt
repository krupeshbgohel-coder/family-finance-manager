package com.familyfinance.manager.ui.components
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
@Composable fun EmptyState(title: String, message: String) { Column { Text(title); Text(message) } }
