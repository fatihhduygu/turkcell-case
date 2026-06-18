package com.turkcell.bip.feature.client

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
class OutgoingCallViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val callStateFlow = MutableStateFlow<CallState>(CallState.Idle)
    private val callManager = mockk<CallManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { callManager.callState } returns callStateFlow
        every { callManager.endCall() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = OutgoingCallViewModel(callManager)

    @Test
    fun `outgoingCall state updates targetAddress`() = runTest(testDispatcher) {
        val vm = viewModel()
        callStateFlow.value = CallState.OutgoingCall("192.168.1.10:3015")
        advanceUntilIdle()
        assertEquals("192.168.1.10:3015", vm.state.value.targetAddress)
    }

    @Test
    fun `callActive after outgoingCall emits NavigateToActiveCall`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.OutgoingCall("192.168.1.10:3015")
            advanceUntilIdle()
            callStateFlow.value = CallState.CallActive
            advanceUntilIdle()
            assertEquals(OutgoingCallEffect.NavigateToActiveCall, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callActive without prior outgoingCall does not navigate`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.CallActive
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `idle after outgoingCall emits NavigateBack`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.OutgoingCall("192.168.1.10:3015")
            advanceUntilIdle()
            callStateFlow.value = CallState.Idle
            advanceUntilIdle()
            assertEquals(OutgoingCallEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel intent calls endCall and emits NavigateBack`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.OutgoingCall("192.168.1.10:3015")
            advanceUntilIdle()
            vm.onIntent(OutgoingCallIntent.Cancel)
            advanceUntilIdle()
            verify { callManager.endCall() }
            assertEquals(OutgoingCallEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callEnded after outgoingCall emits NavigateBack`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.OutgoingCall("192.168.1.10:3015")
            advanceUntilIdle()
            callStateFlow.value = CallState.CallEnded
            advanceUntilIdle()
            assertEquals(OutgoingCallEffect.NavigateBack, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
