import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import org.bigblackowl.debttracker.App
import org.bigblackowl.debttracker.core.di.initKoin

/** Web (JS/Wasm) entry point — mounts [App] into the page's canvas via [ComposeViewport]. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initKoin()
    ComposeViewport { App() }
}
