package com.familyfinance.manager.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyfinance.manager.data.FinanceDatabase
import java.util.Calendar

/** Daily local EMI reminder. WorkManager timing is inexact; it does not need network access. */
class EmiReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "emi_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "EMI reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val now = Calendar.getInstance()
        val today = now.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val loans = FinanceDatabase.get(applicationContext).dao().getAllLoans()
        val currentMonth = "%04d-%02d".format(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
        loans.filter { it.lastPaidYearMonth != currentMonth }.forEach { loan ->
            val dueDay = loan.emiDueDay.coerceIn(1, daysInMonth)
            val dueInTwoDays = (today + 2 == dueDay) || (today == daysInMonth - 1 && dueDay == 1)
            val dueToday = today == dueDay
            val overdue = today > dueDay
            if (dueInTwoDays || dueToday || overdue) {
                val heading = when { overdue -> "EMI overdue: ${loan.title}"; dueToday -> "EMI due today: ${loan.title}"; else -> "EMI due in 2 days: ${loan.title}" }
                val body = when { overdue -> "Your EMI due on day $dueDay is still marked unpaid. Open Family Finance to review it."; dueToday -> "Your monthly EMI date is today (day $dueDay). Open Family Finance to review it."; else -> "Your monthly EMI date is day $dueDay. Plan your payment." }
                val notification = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(heading).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT).setAutoCancel(true).build()
                manager.notify((loan.id % Int.MAX_VALUE).toInt(), notification)
            }
        }
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
