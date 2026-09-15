package org.bigblackowl.debttracker.androidApp.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bigblackowl.debttracker.androidApp.AppActivity
import org.bigblackowl.debttracker.androidApp.R
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.core.shortcuts.HomeTabRequest
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.domain.model.sumByCurrency
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.koin.core.context.GlobalContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Home-screen widget — plain [AppWidgetProvider] + [RemoteViews] (spec §6, §8).
 *
 * The previous Glance implementation kept rendering as an empty tinted box (v1.0.50/51, then again
 * on API 31+): Glance's `SizeMode.Single` composes at `AppWidgetProviderInfo.getMinSize()`, and any
 * mismatch in the size metadata collapsed the whole tree. RemoteViews has none of that — the
 * launcher inflates `R.layout.widget_debt_summary` itself and this class only pushes text, colours
 * and click intents into it. One fluid layout covers the whole resize range (2x2 .. 4x2); the
 * refresh control is a full-width bar along the bottom.
 *
 * Data is read straight from Koin (`GlobalContext`): the widget process starts
 * `DebtTrackerApplication` (so Koin is up) but never the Compose nav graph. Colours follow the
 * app's own light/dark preference (`AppSettings.theme`), re-pushed whenever it might have changed
 * (`AppActivity.onStop`, the ~30-min host tick, the in-widget refresh button).
 */
class DebtSummaryWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        render(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            render(context, manager, manager.getAppWidgetIds(ComponentName(context, javaClass)))
        }
    }

    /** Read balances off the main thread ([goAsync]) and push a fresh [RemoteViews] to every id. */
    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val appContext = context.applicationContext
        // Immediate feedback (spec: loading indicator on refresh) — reuses the last amounts shown
        // so numbers don't flash to "—" while the DB read is in flight, only the refresh icon does.
        val loadingViews = buildViews(appContext, lastAmounts, isLoading = true)
        ids.forEach { manager.updateAppWidget(it, loadingViews) }

        val pending = goAsync()
        scope.launch {
            try {
                val amounts = withTimeoutOrNull(BROADCAST_BUDGET_MS.milliseconds) { loadAmounts() }
                if (amounts != null) lastAmounts = amounts
                val views = buildViews(appContext, amounts ?: lastAmounts, isLoading = false)
                ids.forEach { manager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_REFRESH = "org.bigblackowl.debttracker.androidApp.widget.REFRESH"

        /** Extra on the [AppActivity] launch intent — which Home tab a tapped row opens. */
        const val EXTRA_HOME_TAB = "org.bigblackowl.debttracker.androidApp.widget.HOME_TAB"

        /** goAsync() grants ~10s; stay inside it so a slow DB never ANRs the broadcast. */
        private const val BROADCAST_BUDGET_MS = 8_000L

        /** Soft tonal fill for the icon circle — accent knocked back to ~19 % (0..255). */
        private const val BADGE_ALPHA = 48

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Last successfully loaded totals, kept so a fresh refresh can show a spinner without
         *  blanking out the numbers already on screen; null until the first successful load. */
        @Volatile
        private var lastAmounts: Amounts? = null

        // Mirrors sharedUI theme/DebtAccentColors.kt + the surface/label tokens from theme/Color.kt
        // (both internal, out of reach across the module boundary) — the same accents as KpiCard on
        // StatsScreen. "Repay" green = owed to me, "Debt" red = I owe.
        private const val REPAY_LIGHT = 0xFF2E7D32.toInt()
        private const val REPAY_DARK = 0xFF81C995.toInt()
        private const val DEBT_LIGHT = 0xFFBB152C.toInt()
        private const val DEBT_DARK = 0xFFFFB3B1.toInt()
        private const val LABEL_LIGHT = 0xFF404941.toInt() // OnSurfaceVariantLight
        private const val LABEL_DARK = 0xFFC0C9BE.toInt()  // OnSurfaceVariantDark

        /** Ask the host to re-render every placed widget (called from [AppActivity]). */
        fun refresh(context: Context) {
            val ids = AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, DebtSummaryWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, DebtSummaryWidgetReceiver::class.java).setAction(ACTION_REFRESH),
            )
        }

        /** The two per-currency totals, each already formatted (newline-joined when multi-currency). */
        private data class Amounts(val debtors: String, val creditors: String)

        private suspend fun loadAmounts(): Amounts = coroutineScope {
            val koin = GlobalContext.get()
            val debtors = async {
                koin.get<DebtorRepository>().observeDebtors().first()
                    .sumByCurrency({ it.debtor.currency }, { it.balance }).formatTotals()
            }
            val creditors = async {
                koin.get<CreditorRepository>().observeCreditors().first()
                    .sumByCurrency({ it.creditor.currency }, { it.balance }).formatTotals()
            }
            Amounts(debtors.await(), creditors.await())
        }

        /**
         * Mirrors sharedUI theme/Theme.kt `resolveIsDark` (internal): an explicit "dark"/"light"
         * preference wins over the OS setting, anything else follows it.
         */
        private fun resolveIsDark(themePreference: String, context: Context): Boolean =
            when (themePreference) {
                "dark" -> true
                "light" -> false
                else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            }

        /** [amounts] is null only when the DB read timed out — labels still render, amounts show "—". */
        private fun buildViews(context: Context, amounts: Amounts?, isLoading: Boolean): RemoteViews {
            val settings = GlobalContext.get().get<AppSettings>()
            val strings = resolveStrings(settings.locale)
            val isDark = resolveIsDark(settings.theme, context)
            val repay = if (isDark) REPAY_DARK else REPAY_LIGHT
            val debt = if (isDark) DEBT_DARK else DEBT_LIGHT
            val label = if (isDark) LABEL_DARK else LABEL_LIGHT
            val dash = "—"

            return RemoteViews(context.packageName, R.layout.widget_debt_summary).apply {
                setInt(
                    R.id.widget_root, "setBackgroundResource",
                    if (isDark) R.drawable.widget_card_dark else R.drawable.widget_card_light,
                )
                setInt(
                    R.id.widget_refresh, "setBackgroundResource",
                    if (isDark) R.drawable.widget_refresh_dark else R.drawable.widget_refresh_light,
                )

                bindRow(
                    labelId = R.id.widget_label_debtors,
                    amountId = R.id.widget_amount_debtors,
                    iconId = R.id.widget_icon_debtors,
                    badgeId = R.id.widget_badge_debtors,
                    rowId = R.id.widget_row_debtors,
                    accent = repay,
                    labelColor = label,
                    labelText = strings.home.tabDebtors,
                    amountText = amounts?.debtors ?: dash,
                    description = amounts?.let { strings.widgetDebtorsTotal(it.debtors) },
                    click = activityIntent(context, HomeTabRequest.TAB_DEBTORS),
                )
                bindRow(
                    labelId = R.id.widget_label_creditors,
                    amountId = R.id.widget_amount_creditors,
                    iconId = R.id.widget_icon_creditors,
                    badgeId = R.id.widget_badge_creditors,
                    rowId = R.id.widget_row_creditors,
                    accent = debt,
                    labelColor = label,
                    labelText = strings.home.tabCreditors,
                    amountText = amounts?.creditors ?: dash,
                    description = amounts?.let { strings.widgetCreditorsTotal(it.creditors) },
                    click = activityIntent(context, HomeTabRequest.TAB_CREDITORS),
                )

                setInt(R.id.widget_refresh_icon, "setColorFilter", label)
                setViewVisibility(R.id.widget_refresh_icon, if (isLoading) View.GONE else View.VISIBLE)
                setViewVisibility(R.id.widget_refresh_progress, if (isLoading) View.VISIBLE else View.GONE)
                setTextViewText(R.id.widget_refresh_label, strings.exchangeRates.refresh)
                setTextColor(R.id.widget_refresh_label, label)
                setContentDescription(R.id.widget_refresh, strings.exchangeRates.refresh)
                setOnClickPendingIntent(R.id.widget_refresh, refreshIntent(context))

                // Anywhere else on the card opens the app on the default (debtors) tab.
                setOnClickPendingIntent(
                    R.id.widget_root, activityIntent(context, HomeTabRequest.TAB_DEBTORS),
                )
            }
        }

        private fun RemoteViews.bindRow(
            labelId: Int, amountId: Int, iconId: Int, badgeId: Int, rowId: Int,
            accent: Int, labelColor: Int,
            labelText: String, amountText: String, description: String?,
            click: PendingIntent,
        ) {
            setTextViewText(labelId, labelText)
            setTextViewText(amountId, amountText)
            setTextColor(labelId, labelColor)
            setTextColor(amountId, accent)
            setInt(iconId, "setColorFilter", accent)
            setInt(badgeId, "setColorFilter", accent)
            setInt(badgeId, "setImageAlpha", BADGE_ALPHA)
            description?.let { setContentDescription(rowId, it) }
            setOnClickPendingIntent(rowId, click)
        }

        private fun activityIntent(context: Context, tab: Int): PendingIntent {
            val intent = Intent(context, AppActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_HOME_TAB, tab)
            return PendingIntent.getActivity(
                context, /* requestCode distinct per tab */ 100 + tab, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, DebtSummaryWidgetReceiver::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
