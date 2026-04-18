package com.finfocus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.data.contract.ContractRepository
import com.finfocus.app.presentation.error.DataErrorScreen
import com.finfocus.app.presentation.navigation.FinFocusNavGraph
import com.finfocus.app.presentation.onboarding.OnboardingScreen
import com.finfocus.app.presentation.onboarding.OnboardingViewModel
import com.finfocus.app.ui.theme.FinFocusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Задача 4.5: получаем репозиторий для чтения initError. */
    @Inject lateinit var contractRepository: ContractRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val onboardingVm: OnboardingViewModel = hiltViewModel()
            val completed  by onboardingVm.completed.collectAsStateWithLifecycle(initialValue = false)
            val initError  by contractRepository.initError.collectAsStateWithLifecycle()
            val isLoading  by contractRepository.isLoading.collectAsStateWithLifecycle()
            val scope       = rememberCoroutineScope()

            FinFocusTheme {
                if (isLoading) {
                    com.finfocus.app.presentation.components.LoadingOverlay()
                    return@FinFocusTheme
                }
                when {
                    // Задача 4.5: показываем экран ошибки если инициализация упала
                    initError != null -> DataErrorScreen(
                        errorMessage = initError!!,
                        onRetry = {
                            scope.launch {
                                runCatching { contractRepository.initialize() }
                                    .onFailure { e ->
                                        contractRepository.reportInitError(
                                            e.localizedMessage ?: "Unknown error"
                                        )
                                    }
                            }
                        },
                    )
                    completed -> FinFocusNavGraph()
                    else      -> OnboardingScreen(onboardingVm)
                }
            }
        }
    }
}
