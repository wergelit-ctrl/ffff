package com.finfocus.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finfocus.app.data.settings.ISettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsStore: ISettingsStore,
) : ViewModel() {
    val completed = settingsStore.onboardingCompleted.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun finishOnboarding(uri: String) {
        viewModelScope.launch {
            settingsStore.completeOnboarding(uri)
        }
    }
}
