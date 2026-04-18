package com.finfocus.app.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Interface extracted from SettingsStore so ViewModels can be unit-tested
 * without Android DataStore (which requires Robolectric or instrumented tests).
 */
interface ISettingsStore {
    val onboardingCompleted: Flow<Boolean>
    val contractsTreeUri: Flow<String?>
    suspend fun completeOnboarding(uri: String)
    suspend fun resetOnboarding()
}
