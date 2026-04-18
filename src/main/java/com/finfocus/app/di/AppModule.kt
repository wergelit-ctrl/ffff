package com.finfocus.app.di

import android.content.Context
import com.finfocus.app.data.contract.ContractMerger
import com.finfocus.app.data.contract.ContractRepository
import com.finfocus.app.data.json.JsonDataSource
import com.finfocus.app.data.settings.ISettingsStore
import com.finfocus.app.data.settings.SettingsStore
import com.finfocus.app.domain.repository.FinFocusRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext context: Context): SettingsStore =
        SettingsStore(context)

    @Provides
    @Singleton
    fun provideISettingsStore(store: SettingsStore): ISettingsStore = store

    /**
     * JsonDataSource now receives ISettingsStore so it can read the persisted
     * SAF tree URI on every read/write operation. This makes the data directory
     * actually respect the folder chosen during onboarding.
     */
    @Provides
    @Singleton
    fun provideJsonDataSource(
        @ApplicationContext context: Context,
        settingsStore: ISettingsStore,
    ): JsonDataSource = JsonDataSource(context, settingsStore)

    @Provides
    @Singleton
    fun provideContractRepository(
        dataSource: JsonDataSource,
        merger: ContractMerger,
    ): ContractRepository = ContractRepository(dataSource, merger)

    @Provides
    @Singleton
    fun provideFinFocusRepository(repository: ContractRepository): FinFocusRepository = repository
}
