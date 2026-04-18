package com.finfocus.app.presentation.settings

import com.finfocus.app.data.settings.ISettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // T1: was using real Android main dispatcher — unit tests need test dispatcher
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `resetOnboarding delegates to settings store`() = runTest {
        val fakeStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(fakeStore)

        viewModel.resetOnboarding()
        advanceUntilIdle()

        assertTrue("resetOnboarding() should have been called on the store", fakeStore.resetCalled)
    }

    @Test
    fun `resetOnboarding can be called multiple times`() = runTest {
        val fakeStore = FakeSettingsStore()
        val viewModel = SettingsViewModel(fakeStore)

        viewModel.resetOnboarding()
        viewModel.resetOnboarding()
        advanceUntilIdle()

        assertTrue(fakeStore.resetCalled)
    }
}

/** T1: In-memory fake — replaces real DataStore in unit tests */
private class FakeSettingsStore : ISettingsStore {
    var resetCalled = false
    var completeCalled = false
    var lastCompletedUri: String? = null

    private val _onboardingCompleted = MutableStateFlow(false)
    private val _treeUri = MutableStateFlow<String?>(null)

    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted
    override val contractsTreeUri: Flow<String?> = _treeUri

    override suspend fun completeOnboarding(uri: String) {
        completeCalled = true
        lastCompletedUri = uri
        _onboardingCompleted.value = true
        _treeUri.value = uri
    }

    override suspend fun resetOnboarding() {
        resetCalled = true
        _onboardingCompleted.value = false
        _treeUri.value = null
    }
}
