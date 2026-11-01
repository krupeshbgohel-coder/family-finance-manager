package com.familyfinance.manager.utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
fun formatDate(millis: Long, pattern: String = "dd MMM yyyy"): String = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
