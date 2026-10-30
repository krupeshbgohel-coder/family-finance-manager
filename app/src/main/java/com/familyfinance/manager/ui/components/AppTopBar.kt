package com.familyfinance.manager.ui.components
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.ExperimentalMaterial3Api
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AppTopBar(title: String) { CenterAlignedTopAppBar(title = { Text(title) }) }
