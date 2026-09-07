package org.bigblackowl.debttracker.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** [listDetailIndices] decides when a back stack renders as a two-pane [ListDetailScene]. */
class ListDetailSceneTest {

    private val L = listPane()
    private val D = detailPane()
    private val other = emptyMap<String, Any>()

    @Test
    fun listMetadataAndDetailMetadataAreDisjoint() {
        assertTrue((L.keys intersect D.keys).isEmpty())
    }

    @Test
    fun listThenDetailPairsUp() {
        assertEquals(0 to 1, listDetailIndices(listOf(L, D)))
        // Home, Settings(list), DebtorDetail → the nearest earlier list wins
        assertEquals(1 to 2, listDetailIndices(listOf(L, L, D)))
        // an unrelated screen between them is fine
        assertEquals(0 to 2, listDetailIndices(listOf(L, other, D)))
    }

    @Test
    fun noDetailOnTopMeansSinglePane() {
        assertNull(listDetailIndices(listOf(L)))
        assertNull(listDetailIndices(listOf(L, other)))
        assertNull(listDetailIndices(listOf(L, D, other))) // detail is no longer the top entry
        assertNull(listDetailIndices(emptyList()))
    }

    @Test
    fun detailWithoutAnyPrecedingListIsSinglePane() {
        assertNull(listDetailIndices(listOf(D)))
        assertNull(listDetailIndices(listOf(other, D)))
    }

    /**
     * The list pane shows a back arrow (`ListDetailScene.listCanGoBack`) exactly when its entry
     * isn't the back-stack root — i.e. the returned list index is > 0.
     */
    @Test
    fun listPaneCanGoBackOnlyWhenNotRoot() {
        // Home(list), DebtorDetail → list is the root, no back arrow
        assertEquals(0, listDetailIndices(listOf(L, D))?.first)
        // Home, Settings(list), SettingsProtection → list opened from the menu, keeps a back arrow
        assertTrue((listDetailIndices(listOf(L, L, D))?.first ?: 0) > 0)
        assertTrue((listDetailIndices(listOf(L, other, D))?.first ?: 0) == 0)
    }
}
