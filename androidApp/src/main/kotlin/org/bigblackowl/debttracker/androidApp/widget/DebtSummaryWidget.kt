package org.bigblackowl.debttracker.androidApp.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
        val appSettings = koin.get<AppSettings>()
        val strings = resolveStrings(appSettings.locale)
        // Render in whatever light/dark the user picked for the app, not just the system setting —
        // AppActivity nudges the widget via updateAll() whenever that preference changes.
        val isDark = resolveWidgetIsDark(appSettings.theme, context)

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
                isDark = isDark,
                debtorsAmount = debtorsAmount,
                creditorsAmount = creditorsAmount,
                debtorsLabel = strings.home.tabDebtors,
                creditorsLabel = strings.home.tabCreditors,
                debtorsDescription = strings.widgetDebtorsTotal(debtorsAmount),
                creditorsDescription = strings.widgetCreditorsTotal(creditorsAmount),
            )
        }
    }
}

/**
 * Mirrors sharedUI `theme/Theme.kt`'s `resolveIsDark` (internal, out of reach across the module
 * boundary — the same reason the accent colours below are duplicated): the app's `"dark"` /
 * `"light"` preference wins over the OS setting, anything else follows the OS. Keeps the widget on
 * the same palette the user chose in Settings instead of always tracking the system.
 */
private fun resolveWidgetIsDark(themePreference: String, context: Context): Boolean =
    when (themePreference) {
        "dark" -> true
        "light" -> false
        else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

// Дублює sharedUI theme/DebtAccentColors.kt + потрібні surface-токени з theme/Color.kt (обидва
// internal, недоступні з androidApp через межу модуля) — той самий "тривожний"/"позитивний"
// акцент, що й у KpiCard (StatsScreen.kt), і ті самі фон/підпис, що в res/values*/colors.xml
// (набір widget_preview_*), тож рендер збігається з превʼю та layout-заглушкою.
private val DebtLight = Color(0xFFBB152C)
private val DebtDark = Color(0xFFFFB3B1)
private val RepayLight = Color(0xFF2E7D32)
private val RepayDark = Color(0xFF81C995)

private val WidgetBackgroundLight = Color(0xFFF3F3F4)
private val WidgetBackgroundDark = Color(0xFF1A1C1C)
private val LabelLight = Color(0xFF404941) // OnSurfaceVariantLight
private val LabelDark = Color(0xFFC0C9BE)  // OnSurfaceVariantDark

// Фіксовані розміри розкладки — віджет має один відомий розмір (див. DebtSummaryWidget.sizeMode).
private val OuterPadding = 10.dp
private val PillGap = 20.dp
private val PillCorner = 22.dp
private val BadgeSize = 34.dp
private val BadgeIconSize = 18.dp
private val LabelFontSize = 13.sp
private val AmountFontSize = 18.sp

/**
 * Статична палітра пігулки — синглтони enum, тож [WidgetUi] не алокує нічого зайвого. [icon] —
 * гліф тренду; [accent]/[pill]/[badge] дають акцент і дві тоновані підкладки для обраної теми
 * (дві м'які підкладки на кожен акцент: світліша — для «пігулки» рядка, трохи насиченіша — для
 * кола з іконкою, щоб коло читалося на тлі пігулки).
 */
private enum class Accent(
    val icon: Int,
    private val light: Color,
    private val dark: Color,
) {
    Positive(R.drawable.ic_widget_trend_down, RepayLight, RepayDark),
    Negative(R.drawable.ic_widget_trend_up, DebtLight, DebtDark);

    fun accent(isDark: Boolean): Color = if (isDark) dark else light
    fun pill(isDark: Boolean): Color = accent(isDark).copy(alpha = if (isDark) 0.13f else 0.10f)
    fun badge(isDark: Boolean): Color = accent(isDark).copy(alpha = if (isDark) 0.26f else 0.22f)
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
    isDark: Boolean = false,
    debtorsAmount: String = "1 250,00 ₴",
    creditorsAmount: String = "430,00 ₴",
    debtorsLabel: String = "Мені винні",
    creditorsLabel: String = "Я винен",
    debtorsDescription: String = "$debtorsLabel: $debtorsAmount",
    creditorsDescription: String = "$creditorsLabel: $creditorsAmount",
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(if (isDark) WidgetBackgroundDark else WidgetBackgroundLight)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(AppActivity::class.java))
            .padding(OuterPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        KpiPill(Accent.Positive, isDark, debtorsLabel, debtorsAmount, debtorsDescription)
        Spacer(GlanceModifier.height(PillGap))
        KpiPill(Accent.Negative, isDark, creditorsLabel, creditorsAmount, creditorsDescription)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalGlancePreviewApi::class)
@SuppressLint("RestrictedApi")
@Preview(250, 130)
@Composable
@GlanceComposable
private fun WidgetUiDarkPreview() = WidgetUi(isDark = true)

/** Кольорове коло з іконкою (як у KpiCard) + підпис і сума на м'якій акцентній підкладці. */
@SuppressLint("RestrictedApi")
@Composable
private fun KpiPill(accent: Accent, isDark: Boolean, label: String, amount: String, description: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(accent.pill(isDark))
            .cornerRadius(PillCorner)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Badge(accent, isDark, description)
        Spacer(GlanceModifier.width(10.dp))
        Column {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(if (isDark) LabelDark else LabelLight),
                    fontWeight = FontWeight.Medium,
                    fontSize = LabelFontSize,
                ),
            )
            Text(
                text = amount,
                maxLines = 1,
                style = TextStyle(color = ColorProvider(accent.accent(isDark)), fontWeight = FontWeight.Bold, fontSize = AmountFontSize),
            )
        }
    }
}

/** Коло акцентного кольору з тонованим гліфом тренду. */
@SuppressLint("RestrictedApi")
@Composable
private fun Badge(accent: Accent, isDark: Boolean, description: String) {
    Box(
        modifier = GlanceModifier
            .size(BadgeSize)
            .cornerRadius(BadgeSize / 2)
            .background(accent.badge(isDark)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(accent.icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(ColorProvider(accent.accent(isDark))),
            modifier = GlanceModifier.size(BadgeIconSize),
        )
    }
}
