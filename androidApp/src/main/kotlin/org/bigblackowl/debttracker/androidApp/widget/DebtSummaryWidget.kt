package org.bigblackowl.debttracker.androidApp.widget

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

    /**
     * Віджет фіксованого розміру (resizeMode="none", один targetCell у
     * debt_summary_widget_info.xml). [SizeMode.Single] → [WidgetUi] завжди рендериться під
     * єдиний, наперед відомий розмір, тож жодної адаптивної розкладки не потрібно.
     */
    override val sizeMode: SizeMode = SizeMode.Single

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koin = GlobalContext.get()
        val strings = resolveStrings(koin.get<AppSettings>().locale)

        // Дві незалежні вибірки з БД — читаємо паралельно, форматуємо суму один раз.
        val (debtorsAmount, creditorsAmount) = coroutineScope {
            val debtors = async {
                koin.get<DebtorRepository>().observeDebtors().first()
                    .sumByCurrency({ it.debtor.currency }, { it.balance }).formatTotals()
            }
            val creditors = async {
                koin.get<CreditorRepository>().observeCreditors().first()
                    .sumByCurrency({ it.creditor.currency }, { it.balance }).formatTotals()
            }
            debtors.await() to creditors.await()
        }

        provideContent {
            WidgetUi(
                debtorsAmount = debtorsAmount,
                creditorsAmount = creditorsAmount,
                debtorsLabel = strings.homeTabDebtors,
                creditorsLabel = strings.homeTabCreditors,
                debtorsDescription = strings.widgetDebtorsTotal(debtorsAmount),
                creditorsDescription = strings.widgetCreditorsTotal(creditorsAmount),
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

// Фіксовані розміри розкладки — віджет має один відомий розмір (див. DebtSummaryWidget.sizeMode).
private val OuterPadding = 10.dp
private val PillGap = 20.dp
private val PillCorner = 22.dp
private val BadgeSize = 34.dp
private val BadgeIconSize = 18.dp
private val LabelFontSize = 13.sp
private val AmountFontSize = 18.sp

/**
 * Статична палітра пігулки — синглтони enum, тож [WidgetUi] не алокує нічого на кожну
 * рекомпозицію. [icon] — гліф тренду, решта — акцент і дві тоновані підкладки.
 */
private enum class Accent(
    val icon: Int,
    val color: ColorProvider,
    val pill: ColorProvider,
    val badge: ColorProvider,
) {
    Positive(R.drawable.ic_widget_trend_down, RepayColor, RepayPillColor, RepayBadgeColor),
    Negative(R.drawable.ic_widget_trend_up, DebtColor, DebtPillColor, DebtBadgeColor),
}

/**
 * Дані приходять готовими з [DebtSummaryWidget.provideGlance] (suspend-виклики Koin/Flow
 * не можна робити в @Composable). Значення за замовчуванням — для @Preview.
 *
 * Розкладка одна: дві «пігулки» стовпчиком, кожна — коло з гліфом (той самий мотив, що й
 * `KpiCard` на [org.bigblackowl.debttracker.ui.screens.stats.StatsScreen]), підпис
 * "Мені винні"/"Я винен" і сума жирним акцентним кольором.
 */
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalGlancePreviewApi::class)
@SuppressLint("RestrictedApi")
@Preview(250, 130)
@Composable
@GlanceComposable
fun WidgetUi(
    debtorsAmount: String = "1 250,00 ₴",
    creditorsAmount: String = "430,00 ₴",
    debtorsLabel: String = "Мені винні",
    creditorsLabel: String = "Я винен",
    debtorsDescription: String = "$debtorsLabel: $debtorsAmount",
    creditorsDescription: String = "$creditorsLabel: $creditorsAmount",
) = GlanceTheme {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(AppActivity::class.java))
            .padding(OuterPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        KpiPill(Accent.Positive, debtorsLabel, debtorsAmount, debtorsDescription)
        Spacer(GlanceModifier.height(PillGap))
        KpiPill(Accent.Negative, creditorsLabel, creditorsAmount, creditorsDescription)
    }
}

/** Кольорове коло з іконкою (як у KpiCard) + підпис і сума на м'якій акцентній підкладці. */
@SuppressLint("RestrictedApi")
@Composable
private fun KpiPill(accent: Accent, label: String, amount: String, description: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(accent.pill)
            .cornerRadius(PillCorner)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Badge(accent, description)
        Spacer(GlanceModifier.width(10.dp))
        Column {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    fontSize = LabelFontSize,
                ),
            )
            Text(
                text = amount,
                maxLines = 1,
                style = TextStyle(color = accent.color, fontWeight = FontWeight.Bold, fontSize = AmountFontSize),
            )
        }
    }
}

/** Коло акцентного кольору з тонованим гліфом тренду. */
@SuppressLint("RestrictedApi")
@Composable
private fun Badge(accent: Accent, description: String) {
    Box(
        modifier = GlanceModifier
            .size(BadgeSize)
            .cornerRadius(BadgeSize / 2)
            .background(accent.badge),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(accent.icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(accent.color),
            modifier = GlanceModifier.size(BadgeIconSize),
        )
    }
}
