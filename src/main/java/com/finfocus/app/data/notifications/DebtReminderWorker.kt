package com.finfocus.app.data.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.finfocus.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DebtReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val person = inputData.getString("person").orEmpty()
        val amount = inputData.getDouble("amount", 0.0)
        val notification = NotificationCompat.Builder(appContext, DebtNotificationService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_empty_state)
            .setContentTitle(appContext.getString(R.string.debt_notification_title))
            .setContentText(appContext.getString(R.string.debt_notification_message, person, amount.toString()))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        appContext.getSystemService(NotificationManager::class.java).notify(person.hashCode(), notification)
        return Result.success()
    }
}
