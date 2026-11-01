package com.familyfinance.manager.ui.components
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
@Composable fun SummaryCard(title: String, value: String) { Card { Column { Text(title); Text(value) } } }
