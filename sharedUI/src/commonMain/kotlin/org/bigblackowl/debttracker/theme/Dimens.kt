package org.bigblackowl.debttracker.theme

import androidx.compose.ui.unit.dp

/**
 * The single catalog of spacings and sizes for the whole UI, organized by purpose (not by number):
 * [Spacing] — padding/gap between elements, [Border] — outline thickness, [Radius] — corner radius,
 * [IconSize] — icon/avatar sizes. Values within a category form a small fixed scale;
 * a change here applies immediately everywhere it's used.
 */
object Dimens {
    /** Padding and gap between elements (`Arrangement.spacedBy`, `Spacer`). */
    object Spacing {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 40.dp
    }

    /** Outline thickness (`BorderStroke`). */
    object Border {
        val thin = 1.dp
        val thick = 2.dp
    }

    /** Corner rounding radius (`RoundedCornerShape`). */
    object Radius {
        val none = 0.dp
        val sm = 16.dp
        val lg = 20.dp
    }

    /** Icon and avatar sizes (`Modifier.size`). */
    object IconSize {
        val sm = 20.dp
        val md = 40.dp
        val lg = 60.dp
        val xl = 72.dp
        val xxl = 120.dp
    }

    /** Maximum width of the main content on large screens (desktop/tablet). */
    val contentMaxWidth = 500.dp
}
