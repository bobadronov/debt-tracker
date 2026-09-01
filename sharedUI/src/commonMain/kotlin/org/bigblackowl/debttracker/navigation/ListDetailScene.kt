package org.bigblackowl.debttracker.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Below the Material "expanded" width the app is single-pane, as on phones. */
private val TWO_PANE_MIN_WIDTH: Dp = 720.dp

/** Which slot of a [ListDetailScene] a screen is rendering in — [Full] when there's no split. */
enum class NavPane { Full, List, Detail }

/**
 * Provided by [ListDetailScene] so a screen can adapt to being one half of a split view: the list
 * pane keeps its own in-app top bar, only the detail pane routes into the desktop native title bar
 * (see `BackTopAppBar` / `HomeScreen`). [NavPane.Full] everywhere else (phone, web, narrow window).
 */
val LocalNavPane = staticCompositionLocalOf { NavPane.Full }

/**
 * Renders a list [NavEntry] and a detail [NavEntry] side by side (40 / 60). The list stays put
 * while the selected detail changes — the scene [key] is the *list's* content key, so switching
 * detail items is a plain recomposition rather than a whole-scene NavDisplay animation.
 */
private class ListDetailScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    private val listEntry: NavEntry<T>,
    private val detailEntry: NavEntry<T>,
) : Scene<T> {
    override val entries: List<NavEntry<T>> = listOf(listEntry, detailEntry)

    override val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxSize()) {
            Column(Modifier.weight(0.4f).fillMaxHeight()) {
                CompositionLocalProvider(LocalNavPane provides NavPane.List) { listEntry.Content() }
            }
            VerticalDivider()
            Column(Modifier.weight(0.6f).fillMaxHeight()) {
                CompositionLocalProvider(LocalNavPane provides NavPane.Detail) { detailEntry.Content() }
            }
        }
    }

    // Two ListDetailScenes are the "same scene" iff they show the same list — required so NavDisplay
    // reuses the scene (and only swaps the detail pane) when the selected item changes.
    override fun equals(other: Any?) = other is ListDetailScene<*> && other.key == key
    override fun hashCode() = key.hashCode()
}

/**
 * Returns a [ListDetailScene] when the window is wide enough, the top entry is a detail
 * ([detailPane] metadata) and some earlier entry is a list ([listPane] metadata); `null` otherwise
 * (NavDisplay then falls back to single-pane).
 */
private class ListDetailSceneStrategy<T : Any>(private val twoPane: Boolean) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (!twoPane) return null
        val (listIdx, detailIdx) = listDetailIndices(entries.map { it.metadata }) ?: return null
        return ListDetailScene(
            key = entries[listIdx].contentKey,
            previousEntries = entries.dropLast(1),
            listEntry = entries[listIdx],
            detailEntry = entries[detailIdx],
        )
    }

    companion object {
        const val LIST_KEY = "ListDetailScene-List"
        const val DETAIL_KEY = "ListDetailScene-Detail"
    }
}

/**
 * The list + detail entry positions in a back stack, or `null` if it isn't a "list … then detail"
 * shape: the top entry must carry [detailPane] metadata and some earlier entry [listPane] metadata.
 */
internal fun listDetailIndices(paneMetadata: List<Map<String, Any>>): Pair<Int, Int>? {
    val detailIdx = paneMetadata.lastIndex.takeIf {
        it >= 0 && paneMetadata[it].containsKey(ListDetailSceneStrategy.DETAIL_KEY)
    } ?: return null
    val listIdx = paneMetadata.subList(0, detailIdx).indexOfLast {
        it.containsKey(ListDetailSceneStrategy.LIST_KEY)
    }
    return if (listIdx >= 0) listIdx to detailIdx else null
}

/** Marks a [NavEntry] as the list half of a [ListDetailScene]. */
fun listPane(): Map<String, Any> = mapOf(ListDetailSceneStrategy.LIST_KEY to true)

/** Marks a [NavEntry] as the detail half of a [ListDetailScene]. */
fun detailPane(): Map<String, Any> = mapOf(ListDetailSceneStrategy.DETAIL_KEY to true)

@Composable
fun <T : Any> rememberListDetailSceneStrategy(): SceneStrategy<T> {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    // Reading containerSize here subscribes to window resizes — the strategy flips as the user drags.
    val twoPane = with(density) { windowInfo.containerSize.width.toDp() } >= TWO_PANE_MIN_WIDTH
    return remember(twoPane) { ListDetailSceneStrategy(twoPane) }
}
