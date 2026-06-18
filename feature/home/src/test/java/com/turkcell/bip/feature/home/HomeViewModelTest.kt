package com.turkcell.bip.feature.home

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val callStateFlow = MutableStateFlow<CallState>(CallState.Idle)
    private val serverAddressFlow = MutableStateFlow("")
    private val callManager = mockk<CallManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { callManager.callState } returns callStateFlow
        every { callManager.serverAddress } returns serverAddressFlow
        every { callManager.startServer() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HomeViewModel(callManager)

    @Test
    fun `init starts server`() = runTest(testDispatcher) {
        viewModel()
        verify { callManager.startServer() }
    }

    @Test
    fun `server address update reflects in state`() = runTest(testDispatcher) {
        val vm = viewModel()
        serverAddressFlow.value = "192.168.1.5:3015"
        advanceUntilIdle()
        assertEquals("192.168.1.5:3015", vm.state.value.localAddress)
    }

    @Test
    fun `onTargetAddressChanged updates targetAddress`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onIntent(HomeIntent.OnTargetAddressChanged("192.168.1.10:3015"))
        advanceUntilIdle()
        assertEquals("192.168.1.10:3015", vm.state.value.targetAddress)
    }

    @Test
    fun `onCallClick with self address sets selfCallError`() = runTest(testDispatcher) {
        serverAddressFlow.value = "192.168.1.5:3015"
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(HomeIntent.OnTargetAddressChanged("192.168.1.5:3015"))
        vm.onIntent(HomeIntent.OnCallClick)
        advanceUntilIdle()
        assertTrue(vm.state.value.selfCallError)
    }

    @Test
    fun `onCallClick with self address does not call makeCall`() = runTest(testDispatcher) {
        serverAddressFlow.value = "192.168.1.5:3015"
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(HomeIntent.OnTargetAddressChanged("192.168.1.5:3015"))
        vm.onIntent(HomeIntent.OnCallClick)
        advanceUntilIdle()
        verify(exactly = 0) { callManager.makeCall(any()) }
    }

    @Test
    fun `onCallClick with valid address calls makeCall and emits NavigateToOutgoingCall`() = runTest(testDispatcher) {
        serverAddressFlow.value = "192.168.1.5:3015"
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(HomeIntent.OnTargetAddressChanged("192.168.1.10:3015"))
        advanceUntilIdle()

        vm.effect.receiveAsFlow().test {
            vm.onIntent(HomeIntent.OnCallClick)
            advanceUntilIdle()
            assertEquals(HomeEffect.NavigateToOutgoingCall, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        verify { callManager.makeCall("192.168.1.10:3015") }
    }

    @Test
    fun `onTargetAddressChanged clears selfCallError`() = runTest(testDispatcher) {
        serverAddressFlow.value = "192.168.1.5:3015"
        val vm = viewModel()
        advanceUntilIdle()
        vm.onIntent(HomeIntent.OnTargetAddressChanged("192.168.1.5:3015"))
        vm.onIntent(HomeIntent.OnCallClick)
        advanceUntilIdle()
        assertTrue(vm.state.value.selfCallError)

        vm.onIntent(HomeIntent.OnTargetAddressChanged("192.168.1.10:3015"))
        advanceUntilIdle()
        assertTrue(!vm.state.value.selfCallError)
    }

    @Test
    fun `incoming call emits NavigateToIncomingCall`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.IncomingCall("")
            advanceUntilIdle()
            assertEquals(HomeEffect.NavigateToIncomingCall, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `incoming call does not navigate twice for same state`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.IncomingCall("")
            advanceUntilIdle()
            assertEquals(HomeEffect.NavigateToIncomingCall, awaitItem())
            // StateFlow deduplicates equal values — a second identical emit won't fire
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `callEnded resets flag so next IncomingCall navigates again`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.IncomingCall("")
            advanceUntilIdle()
            assertEquals(HomeEffect.NavigateToIncomingCall, awaitItem())

            callStateFlow.value = CallState.CallEnded
            advanceUntilIdle()

            callStateFlow.value = CallState.IncomingCall("second")
            advanceUntilIdle()
            assertEquals(HomeEffect.NavigateToIncomingCall, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
