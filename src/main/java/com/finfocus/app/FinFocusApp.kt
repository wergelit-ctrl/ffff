package com.finfocus.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.finfocus.app.BuildConfig
import com.finfocus.app.data.contract.ContractRepository
import com.finfocus.app.data.notifications.DebtNotificationService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class FinFocusApp : Application(), Configuration.Provider {

    @Inject lateinit var contractRepository: ContractRepository
    @Inject lateinit var debtNotificationService: DebtNotificationService
    @Inject lateinit var workerFactory: HiltWorkerFactory

    /**
     * Application-scoped coroutine scope tied to the process lifetime.
     * SupervisorJob — failure in one child does not cancel others.
     * Dispatchers.IO — off the main thread by default.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Timber только в debug-сборке — в release логи не пишем
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        debtNotificationService.createChannel()

        // Инициализация контрактов в фоне через applicationScope (не CoroutineScope(IO))
        // SupervisorJob гарантирует что ошибка при инициализации не убьёт другие корутины
        applicationScope.launch {
            try {
                contractRepository.initialize()
            } catch (e: Exception) {
                Timber.e(e, "ContractRepository initialization failed")
                // Задача 4.5: передаём ошибку в репозиторий для показа пользователю
                contractRepository.reportInitError(e.localizedMessage ?: e::class.simpleName ?: "Unknown error")
            }
        }
    }
}
