package org.bigblackowl.debttracker.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.bigblackowl.debttracker.theme.AppTheme
import org.koin.compose.KoinApplicationPreview

/**
 * Isolated DI+theme context for screens' @Preview functions: its own
 * [KoinApplicationPreview] with [previewModule] (a fake backend — no Room/Supabase
 * calls) instead of the app's global Koin context, and its own
 * [ViewModelStoreOwner], because the Preview renderer doesn't always provide one via
 * [LocalViewModelStoreOwner] (needed for koinViewModel()).
 *
 * @param darkTheme forces [AppSettings.theme] to "dark"/"light" so Light/Dark @Preview variants
 *   render deterministically instead of following the IDE's own dark-mode setting. Null keeps the
 *   default ("system").
 */
@Composable
fun DebtTrackerPreview(darkTheme: Boolean? = null, content: @Composable () -> Unit) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    KoinApplicationPreview(application = { modules(previewModule(darkTheme)) }) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
            AppTheme(onThemeChanged = {}, content = content)
        }
    }
}
