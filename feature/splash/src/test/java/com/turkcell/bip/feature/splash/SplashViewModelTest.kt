package com.turkcell.bip.feature.splash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `after 5 seconds delay emits NavigateToHome`() = runTest(testDispatcher) {
        val viewModel = SplashViewModel()
        advanceTimeBy(5_001)
        val effect = viewModel.effect.receive()
        assertEquals(SplashEffect.NavigateToHome, effect)
    }

    @Test
    fun `before 5 seconds delay no effect emitted`() = runTest(testDispatcher) {
        val viewModel = SplashViewModel()
        advanceTimeBy(4_999)
        assertEquals(true, viewModel.effect.isEmpty)
    }
}
