package org.bigblackowl.debttracker.core.di

import com.russhwolf.settings.Settings
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import org.bigblackowl.debttracker.core.remote.createAppSupabaseClient
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.SearchFocusRequests
import org.bigblackowl.debttracker.BuildConfig
import org.bigblackowl.debttracker.core.sound.NoopSoundPlayer
import org.bigblackowl.debttracker.core.sound.SoundPlayer
import org.bigblackowl.debttracker.core.sound.createSoundPlayer
import org.bigblackowl.debttracker.data.remote.SupabaseAuthRepository
import org.bigblackowl.debttracker.data.remote.SupabaseProfileLookupRepository
import org.bigblackowl.debttracker.domain.repository.AuthRepository
import org.bigblackowl.debttracker.domain.repository.ProfileLookupRepository
import org.bigblackowl.debttracker.domain.usecase.DeleteAllDataUseCase
import org.bigblackowl.debttracker.domain.usecase.FindProfileByEmailUseCase
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
import org.bigblackowl.debttracker.ui.screens.creditors.AddEditCreditorViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorDetailViewModel
import org.bigblackowl.debttracker.ui.screens.creditors.CreditorListViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.AddEditDebtorViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorDetailViewModel
import org.bigblackowl.debttracker.ui.screens.debtors.DebtorListViewModel
import org.bigblackowl.debttracker.ui.screens.auth.AuthViewModel
import org.bigblackowl.debttracker.ui.screens.settings.EditAccountViewModel
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
    single<AuthRepository> { SupabaseAuthRepository(get(), get()) }
    single<ProfileLookupRepository> { SupabaseProfileLookupRepository(get(), get()) }
    factoryOf(::DeleteAllDataUseCase)
    factoryOf(::FindProfileByEmailUseCase)

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
    viewModelOf(::StatsViewModel)
}
