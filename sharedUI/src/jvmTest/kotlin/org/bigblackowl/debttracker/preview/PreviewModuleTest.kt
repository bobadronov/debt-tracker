package org.bigblackowl.debttracker.preview

import com.russhwolf.settings.Settings
import org.bigblackowl.debttracker.domain.model.ContactPrefill
import org.bigblackowl.debttracker.domain.model.DebtDirection
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify
import kotlin.test.Test

/**
 * [previewModule] mirrors `appModule` + `platformDataModule()` for @Preview: every use case and
 * ViewModel binding, backed by in-memory fakes. It is trivially easy to add a ViewModel (or a new
 * constructor dependency on one) and forget the preview mirror — the failure only surfaces as a
 * Koin `NoDefinitionFoundException` the first time someone opens that @Preview.
 *
 * [Module.verify] reflects over every definition's primary-type constructors and asserts each
 * parameter resolves to another definition (or a whitelisted type), catching the gap at build time.
 *
 * `extraTypes` — types that are legitimately supplied from outside the container:
 *  - [Settings]: hand-constructed as `InMemorySettings()` inside the `AppSettings` lambda.
 *  - [DebtDirection] / [ContactPrefill]: `parametersOf` args for `AddEditContactViewModel`
 *    (`String` params like ids are already whitelisted by Koin).
 */
class PreviewModuleTest {

    @Test
    @OptIn(KoinExperimentalAPI::class)
    fun previewModuleGraphIsComplete() {
        previewModule().verify(
            extraTypes = listOf(
                Settings::class,
                DebtDirection::class,
                ContactPrefill::class,
            ),
        )
    }
}
