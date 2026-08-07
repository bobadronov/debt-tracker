package org.bigblackowl.debttracker.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Живе стільки ж, скільки застосунок — для AuthRepository.isAuthenticated і SyncCoordinator. */
class ApplicationScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
