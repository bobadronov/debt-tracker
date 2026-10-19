package org.bigblackowl.debttracker

import androidx.compose.ui.Modifier

/** Android-only: exposes `testTag`s as UiAutomator resource-ids. No-op elsewhere. */
expect fun Modifier.exposeTestTagsAsResourceId(): Modifier
