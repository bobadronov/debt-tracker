package org.bigblackowl.debttracker.preview

import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository
import org.bigblackowl.debttracker.domain.repository.SessionRepository
import org.bigblackowl.debttracker.domain.sync.SyncStatusProvider
import org.bigblackowl.debttracker.domain.usecase.ClearLocalCacheUseCase
import org.bigblackowl.debttracker.domain.usecase.DeleteAllDataUseCase
import org.bigblackowl.debttracker.domain.usecase.FindProfileByEmailUseCase
import org.bigblackowl.debttracker.domain.usecase.ForceSignOutUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddCreditorTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.AddOrUpdateCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.DeleteCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorUseCase
import org.bigblackowl.debttracker.domain.usecase.creditor.ObserveCreditorsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddDebtTransactionUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.AddOrUpdateDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.DeleteDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorTransactionsUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorUseCase
import org.bigblackowl.debttracker.domain.usecase.debtor.ObserveDebtorsUseCase
import org.bigblackowl.debttracker.ui.screens.accountonboarding.AccountOnboardingViewModel
import org.bigblackowl.debttracker.ui.screens.auth.AuthViewModel
import org.bigblackowl.debttracker.ui.screens.authgate.AuthGateViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.AddEditCreditorViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorListViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.AddEditDebtorViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorDetailViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorListViewModel
import org.bigblackowl.debttracker.ui.screens.export.ExportViewModel
import org.bigblackowl.debttracker.ui.screens.home.HomeViewModel
import org.bigblackowl.debttracker.ui.screens.protectiononboarding.ProtectionOnboardingViewModel
import org.bigblackowl.debttracker.ui.screens.settings.ActiveSessionsViewModel
import org.bigblackowl.debttracker.ui.screens.settings.EditAccountViewModel
import org.bigblackowl.debttracker.ui.screens.settings.SettingsViewModel
import org.bigblackowl.debttracker.ui.screens.splash.SplashViewModel
import org.bigblackowl.debttracker.ui.screens.stats.StatsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Дзеркало [org.bigblackowl.debttracker.core.di.appModule] для @Preview: ті самі use case/
 * ViewModel біндинги, але репозиторії/AuthRepository/AppSettings — фейкові (in-memory,
 * без Room/Supabase), щоб превʼю рендерились ізольовано й миттєво.
 */
fun previewModule(darkTheme: Boolean? = null): Module = module {
    single {
        AppSettings(InMemorySettings()).apply {
            if (darkTheme != null) theme = if (darkTheme) "dark" else "light"
            locale = "uk"
        }
    }
    single<SoundPlayer> { NoOpSoundPlayer() }
    single { SearchFocusRequests() }
    single<DebtorRepository> { FakeDebtorRepository() }
    single<CreditorRepository> { FakeCreditorRepository() }
    single<AuthRepository> { FakeAuthRepository() }
    single<SyncStatusProvider> { FakeSyncStatusProvider() }
    single<ProfileLookupRepository> { FakeProfileLookupRepository() }
    single<SessionRepository> { FakeSessionRepository() }
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

    factoryOf(::ObserveCreditorsUseCase)
    factoryOf(::ObserveCreditorUseCase)
    factoryOf(::ObserveCreditorTransactionsUseCase)
    factoryOf(::AddOrUpdateCreditorUseCase)
    factoryOf(::DeleteCreditorUseCase)
    factoryOf(::AddCreditorTransactionUseCase)

    viewModelOf(::DebtorListViewModel)
    viewModelOf(::CreditorListViewModel)
    viewModel { (debtorId: String?) -> AddEditDebtorViewModel(debtorId, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (creditorId: String?) -> AddEditCreditorViewModel(creditorId, get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (debtorId: String) -> DebtorDetailViewModel(debtorId, get(), get(), get(), get(), get()) }
    viewModel { (creditorId: String) -> CreditorDetailViewModel(creditorId, get(), get(), get(), get(), get()) }
    viewModelOf(::AuthViewModel)
    viewModelOf(::EditAccountViewModel)
    viewModelOf(::ActiveSessionsViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::AccountOnboardingViewModel)
    viewModelOf(::ProtectionOnboardingViewModel)
    viewModelOf(::AuthGateViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { (debtorId: String?, creditorId: String?) ->
        ExportViewModel(debtorId, creditorId, get(), get(), get(), get(), get(), get(), get(), get())
    }
}
