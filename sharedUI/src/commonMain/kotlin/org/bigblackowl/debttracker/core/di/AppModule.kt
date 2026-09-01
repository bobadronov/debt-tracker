package org.bigblackowl.debttracker.core.di

import com.russhwolf.settings.Settings
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.bigblackowl.debttracker.core.notifications.DueReminderCoordinator
import org.bigblackowl.debttracker.core.notifications.NotificationsPoller
import org.bigblackowl.debttracker.core.remote.createAppSupabaseClient
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.sound.NoopSoundPlayer
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.core.sound.createSoundPlayer
import org.bigblackowl.debttracker.data.remote.RestoreCredentialCoordinator
import org.bigblackowl.debttracker.data.remote.HttpExchangeRatesRepository
import org.bigblackowl.debttracker.data.remote.SupabaseAuthRepository
import org.bigblackowl.debttracker.data.remote.SupabaseNotificationRepository
import org.bigblackowl.debttracker.data.remote.SupabaseProfileLookupRepository
import org.bigblackowl.debttracker.data.remote.SupabaseSessionRepository
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.ExchangeRatesRepository
import org.bigblackowl.debttracker.domain.repository.NotificationRepository
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository
import org.bigblackowl.debttracker.domain.repository.RestoreCredentialGateway
import org.bigblackowl.debttracker.domain.repository.SessionRepository
import org.bigblackowl.debttracker.domain.usecase.ClearLocalCacheUseCase
import org.bigblackowl.debttracker.domain.usecase.DeleteAllDataUseCase
import org.bigblackowl.debttracker.domain.usecase.FindProfileByEmailUseCase
import org.bigblackowl.debttracker.domain.usecase.ForceSignOutUseCase
import org.bigblackowl.debttracker.domain.usecase.ObserveContactSuggestionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddCreditorTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddOrUpdateCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.DeleteCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.LinkCreditorToRegisteredUserUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddDebtTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddOrUpdateDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.DeleteDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.LinkDebtorToRegisteredUserUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase
import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.bigblackowl.debttracker.ui.screens.contacts.AddEditContactViewModel
import org.bigblackowl.debttracker.ui.screens.contacts.ContactPickerViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorListViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorDetailViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorListViewModel
import org.bigblackowl.debttracker.ui.screens.accountonboarding.AccountOnboardingViewModel
import org.bigblackowl.debttracker.ui.screens.authgate.AuthGateViewModel
import org.bigblackowl.debttracker.ui.screens.home.HomeViewModel
import org.bigblackowl.debttracker.ui.screens.notifications.NotificationsViewModel
import org.bigblackowl.debttracker.ui.screens.protectiononboarding.ProtectionOnboardingViewModel
import org.bigblackowl.debttracker.ui.screens.qr.QrHubViewModel
import org.bigblackowl.debttracker.ui.screens.splash.SplashViewModel
import org.bigblackowl.debttracker.ui.screens.auth.AuthViewModel
import org.bigblackowl.debttracker.ui.screens.export.ExportViewModel
import org.bigblackowl.debttracker.ui.screens.settings.ActiveSessionsViewModel
import org.bigblackowl.debttracker.ui.screens.settings.EditAccountViewModel
import org.bigblackowl.debttracker.ui.screens.settings.SettingsViewModel
import org.bigblackowl.debttracker.ui.screens.exchange.ExchangeRatesViewModel
import org.bigblackowl.debttracker.ui.screens.stats.StatsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Спільні (platform-agnostic) біндинги: use cases поверх [org.bigblackowl.debttracker.domain.repository.DebtorRepository]/
 * [org.bigblackowl.debttracker.domain.repository.CreditorRepository] — самі репозиторії
 * прив'язуються в [platformDataModule] (різні реалізації на Android/iOS/Desktop vs Web).
 */
val appModule = module {
    single { AppSettings(Settings()) }
    single<SoundPlayer> { if (BuildConfig.SOUND_ENABLED) createSoundPlayer() else NoopSoundPlayer }
    single<CoroutineScope> { ApplicationScope() }
    single { SearchFocusRequests() }
    single<SupabaseClient> { createAppSupabaseClient() }
    // Shared Ktor client for the app's own small REST calls (exchange rates) — the engine is
    // whatever the platform source set pulls in (OkHttp / Darwin / JS).
    single {
        HttpClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
            install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 10_000 }
        }
    }
    single<AuthRepository> { SupabaseAuthRepository(get(), get()) }
    single<ExchangeRatesRepository> { HttpExchangeRatesRepository(get(), get()) }
    single<RestoreCredentialGateway> { RestoreCredentialCoordinator(get(), get(), get()) }
    single<ProfileLookupRepository> { SupabaseProfileLookupRepository(get(), get()) }
    single<SessionRepository> { SupabaseSessionRepository(get(), get()) }
    single<NotificationRepository> { SupabaseNotificationRepository(get(), get()) }
    single { NotificationsPoller(get(), get(), get(), get(), get()) }
    single { DueReminderCoordinator(get(), get(), get(), get(), get(), get()) }
    factoryOf(::DeleteAllDataUseCase)
    factoryOf(::ClearLocalCacheUseCase)
    factoryOf(::FindProfileByEmailUseCase)
    factoryOf(::ForceSignOutUseCase)

    factoryOf(::ObserveDebtorsUseCase)
    factoryOf(::ObserveDebtorUseCase)
    factoryOf(::ObserveDebtorTransactionsUseCase)
    factoryOf(::AddOrUpdateDebtorUseCase)
    factoryOf(::DeleteDebtorUseCase)
    factoryOf(::AddDebtTransactionUseCase)
    factoryOf(::LinkDebtorToRegisteredUserUseCase)

    factoryOf(::ObserveCreditorsUseCase)
    factoryOf(::ObserveCreditorUseCase)
    factoryOf(::ObserveCreditorTransactionsUseCase)
    factoryOf(::AddOrUpdateCreditorUseCase)
    factoryOf(::DeleteCreditorUseCase)
    factoryOf(::AddCreditorTransactionUseCase)
    factoryOf(::LinkCreditorToRegisteredUserUseCase)

    factoryOf(::ObserveContactSuggestionsUseCase)

    viewModelOf(::DebtorListViewModel)
    viewModelOf(::CreditorListViewModel)
    viewModelOf(::ContactPickerViewModel)
    viewModel { (direction: DebtDirection, prefill: ContactPrefill?, editId: String?) ->
        AddEditContactViewModel(
            direction, prefill, editId,
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (debtorId: String) -> DebtorDetailViewModel(debtorId, get(), get(), get(), get(), get()) }
    viewModel { (creditorId: String) -> CreditorDetailViewModel(creditorId, get(), get(), get(), get(), get()) }
    viewModelOf(::AuthViewModel)
    viewModelOf(::EditAccountViewModel)
    viewModelOf(::ActiveSessionsViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::ExchangeRatesViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::AccountOnboardingViewModel)
    viewModelOf(::ProtectionOnboardingViewModel)
    viewModelOf(::AuthGateViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::QrHubViewModel)
    viewModelOf(::NotificationsViewModel)
    viewModel { (debtorId: String?, creditorId: String?) ->
        ExportViewModel(debtorId, creditorId, get(), get(), get(), get(), get(), get(), get(), get())
    }
}
