package com.familyfinance.manager.utils
fun isValidAmount(amount: String): Boolean = amount.toDoubleOrNull()?.let { it > 0.0 } ?: false
