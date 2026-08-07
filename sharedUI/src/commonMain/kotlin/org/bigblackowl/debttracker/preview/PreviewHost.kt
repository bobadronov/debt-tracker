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
 * Ізольований DI+тема контекст для @Preview-функцій екранів: власний
 * [KoinApplicationPreview] з [previewModule] (фейковий бекенд — жодних Room/Supabase
 * викликів) замість глобального Koin-контексту застосунку, і власний
 * [ViewModelStoreOwner], бо Preview-рендерер не завжди надає його через
 * [LocalViewModelStoreOwner] (потрібен для koinViewModel()).
 */
@Composable
fun DebtTrackerPreview(content: @Composable () -> Unit) {
    val viewModelStoreOwner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    KoinApplicationPreview(application = { modules(previewModule()) }) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
            AppTheme(onThemeChanged = {}, content = content)
        }
    }
}
