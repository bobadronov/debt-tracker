package org.bigblackowl.debttracker.ui.screens.auth

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bigblackowl.debttracker.core.auth.GoogleSignInLauncher
import org.bigblackowl.debttracker.core.auth.GoogleSignInOutcome
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway
import org.bigblackowl.debttracker.domain.repository.RestoreSessionResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Covers the [AuthIntent.GoogleSignIn] reducer paths that don't need a live session or i18n
 * (`resolveStrings` can't run in a headless jvmTest without the Compose runtime). The Success and
 * Failure paths are exercised by the manual per-platform smoke test in the plan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class FakeGoogleLauncher(var outcome: GoogleSignInOutcome) : GoogleSignInLauncher {
        var calls = 0
        override suspend fun signIn(): GoogleSignInOutcome { calls++; return outcome }
    }

    private class FakeRestoreGateway : RestoreCredentialGateway {
        override val isActive = false
        override suspend fun registerForCurrentSession() {}
        override suspend fun tryRestoreSession() = RestoreSessionResult.UNSUPPORTED
        override suspend fun clear() {}
    }

    private class FakeAuthRepository : AuthRepository {
        override val isAuthenticated: StateFlow<Boolean> = MutableStateFlow(false)
        override val currentUserId: String? = null
        override val email = MutableStateFlow<String?>(null)
        override val displayName = MutableStateFlow<String?>(null)
        override val phone = MutableStateFlow<String?>(null)
        override val avatarUrl = MutableStateFlow<String?>(null)
        override suspend fun signUp(email: String, password: String) = Result.success(Unit)
        override suspend fun signIn(email: String, password: String) = Result.success(Unit)
        override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String?) = Result.success(Unit)
        override suspend fun signOut() {}
        override suspend fun updateAvatar(bytes: ByteArray, fileExtension: String) = Result.success("")
        override suspend fun updateProfile(displayName: String, phone: String?) = Result.success(Unit)
    }

    private fun viewModel(launcher: FakeGoogleLauncher) =
        AuthViewModel(FakeAuthRepository(), AppSettings(MapSettings()), FakeRestoreGateway(), launcher)

    @Test
    fun `google cancelled just clears the spinner`() = runTest(dispatcher) {
        val launcher = FakeGoogleLauncher(GoogleSignInOutcome.Cancelled)
        val vm = viewModel(launcher)

        vm.onIntent(AuthIntent.GoogleSignIn)
        testScheduler.advanceUntilIdle()

        assertFalse(vm.state.value.isGoogleLoading)
        assertNull(vm.state.value.error)
        assertEquals(1, launcher.calls)
    }

    @Test
    fun `the spinner is shown synchronously when the flow starts`() = runTest(dispatcher) {
        val launcher = FakeGoogleLauncher(GoogleSignInOutcome.Cancelled)
        val vm = viewModel(launcher)

        vm.onIntent(AuthIntent.GoogleSignIn) // no scheduler advance — must already be reflected
        assertEquals(true, vm.state.value.isGoogleLoading)

        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `google sign-in ignores a second tap while already running`() = runTest(dispatcher) {
        val launcher = FakeGoogleLauncher(GoogleSignInOutcome.Cancelled)
        val vm = viewModel(launcher)

        vm.onIntent(AuthIntent.GoogleSignIn)
        vm.onIntent(AuthIntent.GoogleSignIn) // still in-flight — must be a no-op
        testScheduler.advanceUntilIdle()

        assertEquals(1, launcher.calls)
    }
}
