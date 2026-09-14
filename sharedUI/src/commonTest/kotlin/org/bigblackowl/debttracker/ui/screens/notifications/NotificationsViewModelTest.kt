package org.bigblackowl.debttracker.ui.screens.notifications

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.AppNotification
import org.bigblackowl.debttracker.domain.model.CorrectionReason
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository
import org.bigblackowl.debttracker.preview.NoOpLocalNotifier
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Covers the 0014 correction reducer paths — propose / approve / reject each RPC + delete the row. */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

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

    private class FakeNotificationRepository : NotificationRepository {
        var rows = mutableListOf<AppNotification>()
        val deleted = mutableListOf<String>()
        var proposeArgs: Triple<String, CorrectionReason, BigDecimal?>? = null
        var approvedId: String? = null
        var rejectedId: String? = null

        override suspend fun fetchSince(after: kotlin.time.Instant?) = rows.toList()
        override suspend fun fetchAll() = rows.toList()
        override suspend fun unreadCount() = rows.count { !it.isRead }
        override suspend fun markRead(id: String) {}
        override suspend fun markAllRead() {}
        override suspend fun delete(id: String) { deleted += id; rows.removeAll { it.id == id } }
        var succeed = true
        override suspend fun approveLinkRequest(requestId: String) = succeed
        override suspend fun rejectLinkRequest(requestId: String) = succeed
        override suspend fun proposeTransactionCorrection(
            notificationId: String,
            reason: CorrectionReason,
            amount: BigDecimal?,
        ): Boolean { proposeArgs = Triple(notificationId, reason, amount); return succeed }
        override suspend fun approveTransactionCorrection(correctionId: String): Boolean { approvedId = correctionId; return succeed }
        override suspend fun rejectTransactionCorrection(correctionId: String): Boolean { rejectedId = correctionId; return succeed }
    }

    private fun viewModel(repo: NotificationRepository): NotificationsViewModel {
        val poller = NotificationsPoller(
            scope = CoroutineScope(dispatcher),
            authRepository = FakeAuthRepository(),
            notificationRepository = repo,
            localNotifier = NoOpLocalNotifier(),
            appSettings = AppSettings(MapSettings()),
        )
        return NotificationsViewModel(repo, poller, AppSettings(MapSettings()))
    }

    @Test
    fun `submit correction calls RPC then deletes the source notification`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = viewModel(repo)

        vm.onIntent(
            NotificationsIntent.SubmitCorrection("n1", CorrectionReason.WRONG_AMOUNT, BigDecimal.parseString("42")),
        )
        advanceUntilIdle()

        assertEquals("n1", repo.proposeArgs?.first)
        assertEquals(CorrectionReason.WRONG_AMOUNT, repo.proposeArgs?.second)
        assertEquals(BigDecimal.parseString("42"), repo.proposeArgs?.third)
        assertTrue("n1" in repo.deleted)
        assertNull(vm.state.value.correctionDialogFor)
    }

    @Test
    fun `approve correction calls RPC then deletes the row`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = viewModel(repo)

        vm.onIntent(NotificationsIntent.ApproveCorrection("n2", "c2"))
        advanceUntilIdle()

        assertEquals("c2", repo.approvedId)
        assertTrue("n2" in repo.deleted)
    }

    @Test
    fun `reject correction calls RPC then deletes the row`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = viewModel(repo)

        vm.onIntent(NotificationsIntent.RejectCorrection("n3", "c3"))
        advanceUntilIdle()

        assertEquals("c3", repo.rejectedId)
        assertTrue("n3" in repo.deleted)
    }

    @Test
    fun `failed approve correction sends an error effect and keeps the notification`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository().apply { succeed = false }
        val vm = viewModel(repo)

        val effects = mutableListOf<NotificationsEffect>()
        val job = launch { vm.effects.collect { effects += it } }

        vm.onIntent(NotificationsIntent.ApproveCorrection("n2", "c2"))
        advanceUntilIdle()

        assertTrue(effects.any { it is NotificationsEffect.Error })
        assertTrue("n2" !in repo.deleted)
        job.cancel()
    }

    @Test
    fun `open and dismiss correction dialog toggles state`() = runTest(dispatcher) {
        val repo = FakeNotificationRepository()
        val vm = viewModel(repo)
        val n = AppNotification(
            id = "n4",
            type = org.bigblackowl.debttracker.domain.model.NotificationType.DEBT_TRANSACTION_ADDED,
            actorDisplayName = "Bob",
            relatedDebtorId = null,
            relatedCreditorId = "cr1",
            relatedLinkRequestId = null,
            relatedTransactionId = "tx1",
            relatedCorrectionId = null,
            amount = BigDecimal.parseString("-100"),
            currency = null,
            isRead = false,
            createdAt = kotlin.time.Clock.System.now(),
        )

        vm.onIntent(NotificationsIntent.OpenCorrectionDialog(n))
        assertEquals("n4", vm.state.value.correctionDialogFor?.id)

        vm.onIntent(NotificationsIntent.DismissCorrectionDialog)
        assertNull(vm.state.value.correctionDialogFor)
    }
}
