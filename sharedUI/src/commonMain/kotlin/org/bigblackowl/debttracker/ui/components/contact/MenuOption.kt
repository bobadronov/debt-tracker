package org.bigblackowl.debttracker.ui.components.contact

import androidx.compose.ui.graphics.vector.ImageVector

/** One entry in a [ListSearchBar] sort/status dropdown. */
data class MenuOption<T>(val value: T, val label: String, val icon: ImageVector)
