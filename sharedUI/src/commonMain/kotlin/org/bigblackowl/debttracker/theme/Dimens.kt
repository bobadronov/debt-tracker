package org.bigblackowl.debttracker.theme

import androidx.compose.ui.unit.dp

/**
 * Єдиний каталог відступів і розмірів для всього UI. Значення згруповані за
 * числом (не за призначенням), бо той самий dp часто повторно використовується
 * для різних цілей (padding, spacing, розмір іконки, радіус кута тощо) —
 * зміна значення тут одразу застосовується до всіх місць використання.
 */
object Dimens {
    val space0 = 0.dp
    val space1 = 1.dp
    val space2 = 2.dp
    val space3 = 3.dp
    val space4 = 4.dp
    val space5 = 5.dp
    val space6 = 6.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space14 = 14.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space28 = 28.dp
    val space30 = 30.dp
    val space40 = 40.dp
    val space48 = 48.dp
    val space56 = 56.dp
    val space60 = 60.dp
    val space72 = 72.dp
    val space120 = 120.dp

    /** Максимальна ширина основного контенту на великих екранах (desktop/tablet). */
    val contentMaxWidth = 400.dp
}
