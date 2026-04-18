package com.finfocus.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "finfocus_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : ISettingsStore {
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val contractsTreeUriKey = stringPreferencesKey("contracts_tree_uri")

    override val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[onboardingCompletedKey] ?: false }
    override val contractsTreeUri: Flow<String?> = context.dataStore.data.map { it[contractsTreeUriKey] }

    override suspend fun completeOnboarding(uri: String) {
        context.dataStore.edit {
            it[onboardingCompletedKey] = true
            it[contractsTreeUriKey] = uri
        }
    }

    override suspend fun resetOnboarding() {
        context.dataStore.edit {
            it.remove(onboardingCompletedKey)
            it.remove(contractsTreeUriKey)
        }
    }
}
