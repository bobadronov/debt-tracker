package org.bigblackowl.debttracker.androidApp.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import org.bigblackowl.debttracker.androidApp.AppActivity
import org.bigblackowl.debttracker.androidApp.R
import org.bigblackowl.debttracker.core.i18n.resolveStrings
import org.bigblackowl.debttracker.core.settings.AppSettings
import org.bigblackowl.debttracker.domain.model.formatTotals
import org.bigblackowl.debttracker.domain.model.sumByCurrency
import org.bigblackowl.debttracker.domain.repository.CreditorRepository
import org.bigblackowl.debttracker.domain.repository.DebtorRepository
import org.koin.core.context.GlobalContext

/**
 * Android Glance widget (спек §6, §8, Фаза 7): сумарні "Мені винні"/"Я винен".
 * Дані читаються напряму з Koin (GlobalContext), бо widget-процес не проходить
 * через звичайний Compose-навігаційний граф застосунку.
 */
class DebtSummaryWidget : GlanceAppWidget() {

    /** Реальний поточний розмір (а не лише minWidth/minHeight) — потрібен для [WidgetUi] масштабування. */
    override val sizeMode: SizeMode = SizeMode.Exact

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val debtorRepository = koin.get<DebtorRepository>()
        val creditorRepository = koin.get<CreditorRepository>()
        val strings = resolveStrings(koin.get<AppSettings>().locale)

        val debtorsTotal = debtorRepository.observeDebtors().first()
            .sumByCurrency({ it.debtor.currency }, { it.balance })
        val creditorsTotal = creditorRepository.observeCreditors().first()
            .sumByCurrency({ it.creditor.currency }, { it.balance })

        provideContent {
            WidgetUi(
                debtorsAmount = debtorsTotal.formatTotals(),
                creditorsAmount = creditorsTotal.formatTotals(),
                debtorsLabel = strings.homeTabDebtors,
                creditorsLabel = strings.homeTabCreditors,
                debtorsDescription = strings.widgetDebtorsTotal(debtorsTotal.formatTotals()),
                creditorsDescription = strings.widgetCreditorsTotal(creditorsTotal.formatTotals()),
            )
        }
    }
}

// Дублює sharedUI theme/DebtAccentColors.kt (internal, недоступний з androidApp через
// межу модуля) — той самий "тривожний"/"позитивний" акцент, що й у KpiCard (StatsScreen.kt).
private val DebtLight = Color(0xFFBB152C)
private val DebtDark = Color(0xFFFFB3B1)
private val RepayLight = Color(0xFF2E7D32)
private val RepayDark = Color(0xFF81C995)

private val DebtColor = ColorProvider(day = DebtLight, night = DebtDark)
private val RepayColor = ColorProvider(day = RepayLight, night = RepayDark)

// Дві м'які підкладки на кожен акцент: світліша — для «пігулки» рядка, трохи
// насиченіша — для кола з іконкою, щоб коло читалося на тлі пігулки.
private val DebtPillColor = ColorProvider(day = DebtLight.copy(alpha = 0.10f), night = DebtDark.copy(alpha = 0.13f))
private val RepayPillColor = ColorProvider(day = RepayLight.copy(alpha = 0.10f), night = RepayDark.copy(alpha = 0.13f))
private val DebtBadgeColor = ColorProvider(day = DebtLight.copy(alpha = 0.22f), night = DebtDark.copy(alpha = 0.26f))
private val RepayBadgeColor = ColorProvider(day = RepayLight.copy(alpha = 0.22f), night = RepayDark.copy(alpha = 0.26f))

private class Metric(
    val icon: Int,
    val label: String,
    val amount: String,
    val description: String,
    val accent: ColorProvider,
    val pill: ColorProvider,
    val badge: ColorProvider,
)

/**
 * Дані приходять готовими з [DebtSummaryWidget.provideGlance] (suspend-виклики Koin/Flow
 * не можна робити в @Composable). Значення за замовчуванням — для @Preview.
 *
 * Кожен показник — «пігулка» з тонованою підкладкою: коло з гліфом (той самий мотив, що й
 * `KpiCard` на [org.bigblackowl.debttracker.ui.screens.stats.StatsScreen]) і сума жирним
 * акцентним кольором. Дві розкладки, вибір — від [LocalSize.current], щоб контент ніколи не
 * обрізався: досить високий віджет → пігулки стовпчиком, з підписами "Мені винні"/"Я винен";
 * нижчий (у т.ч. широкий короткий) → дві компактні пігулки поряд, лише коло + сума.
 */
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalGlancePreviewApi::class)
@SuppressLint("RestrictedApi")
@Preview(250, 120)
@Composable
@GlanceComposable
fun WidgetUi(
    debtorsAmount: String = "1 250,00 ₴",
    creditorsAmount: String = "430,00 ₴",
    debtorsLabel: String = "Мені винні",
    creditorsLabel: String = "Я винен",
    debtorsDescription: String = "$debtorsLabel: $debtorsAmount",
    creditorsDescription: String = "$creditorsLabel: $creditorsAmount",
) {
    val size = LocalSize.current
    val w = size.width
    val h = size.height
    val outer = 10.dp
    val gap = 6.dp

    // Стовпчик двох пігулок вимагає висоти; інакше (і на широкому короткому) — пігулки поряд.
    val stacked = h >= 116.dp

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(AppActivity::class.java))
                .padding(outer),
            contentAlignment = Alignment.Center,
        ) {
            val debtors = Metric(
                R.drawable.ic_widget_trend_down, debtorsLabel, debtorsAmount, debtorsDescription,
                RepayColor, RepayPillColor, RepayBadgeColor,
            )
            val creditors = Metric(
                R.drawable.ic_widget_trend_up, creditorsLabel, creditorsAmount, creditorsDescription,
                DebtColor, DebtPillColor, DebtBadgeColor,
            )

            if (stacked) {
                val pillH = ((h - outer * 2 - gap) / 2).coerceIn(44.dp, 104.dp)
                val pillW = w - outer * 2
                val spec = pillSpec(pillH, pillW)
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    KpiPill(GlanceModifier.fillMaxWidth(), debtors, spec)
                    Spacer(GlanceModifier.height(gap))
                    KpiPill(GlanceModifier.fillMaxWidth(), creditors, spec)
                }
            } else {
                val cellH = (h - outer * 2).coerceIn(28.dp, 84.dp)
                val cellW = (w - outer * 2 - gap) / 2
                val badge = minOf(cellH * 0.62f, cellW * 0.34f).coerceIn(20.dp, 44.dp)
                val amountFont = (badge.value * 0.5f).coerceIn(12f, 19f).sp
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    MiniMetric(GlanceModifier.defaultWeight(), debtors, cellH, badge, amountFont)
                    Spacer(GlanceModifier.width(gap))
                    MiniMetric(GlanceModifier.defaultWeight(), creditors, cellH, badge, amountFont)
                }
            }
        }
    }
}

private class PillSpec(
    val badgeSize: Dp,
    val iconSize: Dp,
    val innerPaddingH: Dp,
    val innerPaddingV: Dp,
    val corner: Dp,
    val amountFontSize: TextUnit,
    val labelFontSize: TextUnit,
    val showLabel: Boolean,
)

private fun pillSpec(pillHeight: Dp, pillWidth: Dp): PillSpec {
    val showLabel = pillHeight >= 54.dp && pillWidth >= 150.dp
    val innerV = (pillHeight * 0.16f).coerceIn(6.dp, 14.dp)
    val contentH = pillHeight - innerV * 2
    val badge = minOf(contentH, pillWidth * 0.30f).coerceIn(18.dp, 44.dp)
    val amountFactor = if (showLabel) 0.40f else 0.52f
    return PillSpec(
        badgeSize = badge,
        iconSize = badge * 0.52f,
        innerPaddingH = (pillHeight * 0.20f).coerceIn(8.dp, 16.dp),
        innerPaddingV = innerV,
        corner = (pillHeight * 0.34f).coerceIn(12.dp, 26.dp),
        amountFontSize = (contentH.value * amountFactor).coerceIn(12f, 21f).sp,
        labelFontSize = (contentH.value * 0.26f).coerceIn(9f, 12f).sp,
        showLabel = showLabel,
    )
}

/** Кольорове коло з іконкою (як у KpiCard) + сума (та опційно підпис) на м'якій акцентній підкладці. */
@SuppressLint("RestrictedApi")
@Composable
private fun KpiPill(modifier: GlanceModifier, metric: Metric, spec: PillSpec) {
    Row(
        modifier = modifier
            .background(metric.pill)
            .cornerRadius(spec.corner)
            .padding(horizontal = spec.innerPaddingH, vertical = spec.innerPaddingV),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Badge(metric, spec.badgeSize, spec.iconSize)
        Spacer(GlanceModifier.width(10.dp))
        if (spec.showLabel) {
            Column {
                Text(
                    text = metric.label,
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = spec.labelFontSize,
                    ),
                )
                AmountText(metric.amount, metric.accent, spec.amountFontSize, maxLines = 2)
            }
        } else {
            AmountText(metric.amount, metric.accent, spec.amountFontSize, maxLines = 1)
        }
    }
}

/** Найкомпактніший варіант: коло + сума в один рядок по центру пігулки, без підпису. */
@SuppressLint("RestrictedApi")
@Composable
private fun MiniMetric(
    modifier: GlanceModifier,
    metric: Metric,
    cellHeight: Dp,
    badgeSize: Dp,
    amountFontSize: TextUnit,
) {
    Row(
        modifier = modifier
            .background(metric.pill)
            .cornerRadius((cellHeight / 2).coerceAtMost(22.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Badge(metric, badgeSize, badgeSize * 0.52f)
        Spacer(GlanceModifier.width(7.dp))
        AmountText(metric.amount, metric.accent, amountFontSize, maxLines = 1)
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun Badge(metric: Metric, badgeSize: Dp, iconSize: Dp) {
    Box(
        modifier = GlanceModifier
            .size(badgeSize)
            .cornerRadius(badgeSize / 2)
            .background(metric.badge),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(metric.icon),
            contentDescription = metric.description,
            colorFilter = ColorFilter.tint(metric.accent),
            modifier = GlanceModifier.size(iconSize),
        )
    }
}

@Composable
private fun AmountText(amount: String, accent: ColorProvider, fontSize: TextUnit, maxLines: Int) {
    Text(
        text = amount,
        maxLines = maxLines,
        style = TextStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = fontSize),
    )
}
