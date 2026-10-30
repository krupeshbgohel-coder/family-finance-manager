package com.familyfinance.manager.utils
import java.text.NumberFormat
import java.util.Locale
fun formatCurrency(amount: Double, locale: Locale = Locale("en", "IN")): String = NumberFormat.getCurrencyInstance(locale).format(amount)
