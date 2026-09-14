package org.bigblackowl.debttracker.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Lives as long as the app itself — for AuthRepository.isAuthenticated and SyncCoordinator. */
class ApplicationScope : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.Default)
