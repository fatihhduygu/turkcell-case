package com.turkcell.bip.feature.host

import app.cash.turbine.test
import com.turkcell.bip.core.webrtc.CallManager
import com.turkcell.bip.core.webrtc.CallState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingCallViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val callStateFlow = MutableStateFlow<CallState>(CallState.IncomingCall(""))
    private val callManager = mockk<CallManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { callManager.callState } returns callStateFlow
        every { callManager.acceptCall() } just runs
        every { callManager.rejectCall() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = IncomingCallViewModel(callManager)

    @Test
    fun `accept intent calls acceptCall`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onIntent(IncomingCallIntent.Accept)
        advanceUntilIdle()
        verify { callManager.acceptCall() }
    }

    @Test
    fun `reject intent calls rejectCall and emits NavigateBack`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            vm.onIntent(IncomingCallIntent.Reject)
            advanceUntilIdle()
            verify { callManager.rejectCall() }
            assertEquals(IncomingCallEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callActive after accept emits NavigateToActiveCall`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            vm.onIntent(IncomingCallIntent.Accept)
            advanceUntilIdle()
            callStateFlow.value = CallState.CallActive
            advanceUntilIdle()
            assertEquals(IncomingCallEffect.NavigateToActiveCall, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callActive without accept does not navigate`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.CallActive
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callEnded before accept emits NavigateBack`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.CallEnded
            advanceUntilIdle()
            assertEquals(IncomingCallEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `idle before accept emits NavigateBack`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.Idle
            advanceUntilIdle()
            assertEquals(IncomingCallEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
