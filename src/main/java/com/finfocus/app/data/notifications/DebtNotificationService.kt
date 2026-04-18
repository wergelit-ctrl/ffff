package com.finfocus.app.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class DebtNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "debt_reminders"
    }

    fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Напоминания о долгах", NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun scheduleDebtReminder(debtId: String, personName: String, amount: Double, dueDate: Long) {
        val now = System.currentTimeMillis()
        val threeDays = dueDate - TimeUnit.DAYS.toMillis(3)
        val payload = Data.Builder().putString("person", personName).putDouble("amount", amount).build()
        if (threeDays > now) {
            val remindBefore = OneTimeWorkRequestBuilder<DebtReminderWorker>()
                .setInputData(payload)
                .setInitialDelay(threeDays - now, TimeUnit.MILLISECONDS)
                .addTag("debt_$debtId")
                .build()
            WorkManager.getInstance(context).enqueue(remindBefore)
        }
        if (dueDate > now) {
            val remindOnDay = OneTimeWorkRequestBuilder<DebtReminderWorker>()
                .setInputData(payload)
                .setInitialDelay(dueDate - now, TimeUnit.MILLISECONDS)
                .addTag("debt_day_$debtId")
                .build()
            WorkManager.getInstance(context).enqueue(remindOnDay)
        }
    }
}
