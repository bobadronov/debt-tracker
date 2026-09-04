package org.bigblackowl.debttracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.debttracker.theme.Dimens

/**
 * First-load spinner shared by every screen whose state carries an `isLoading` flag (spec §6) —
 * true only until that screen's `combine(...)` in its ViewModel emits for the first time (data
 * sources loading in parallel), then false for the life of the ViewModel. Centered over the whole
 * content area, same sizing as [PlaceholderScreen]'s default body, so a screen's brief first-load
 * flash and its Phase-1 placeholder look identical.
 */
@Composable
fun FullScreenLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularWavyProgressIndicator(modifier = Modifier.size(Dimens.space60))
    }
}
