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
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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
private val DebtBadgeColor = ColorProvider(day = DebtLight.copy(alpha = 0.12f), night = DebtDark.copy(alpha = 0.12f))
private val RepayBadgeColor = ColorProvider(day = RepayLight.copy(alpha = 0.12f), night = RepayDark.copy(alpha = 0.12f))

/** Базові розміри при "еталонній" висоті 110dp — масштабуються від [LocalSize] в [WidgetUi]. */
private const val ReferenceHeight = 110
private const val BaseBadgeSize = 36
private const val BaseFontSize = 17f
private const val BaseSpacing = 10
private const val BasePadding = 12

/** Від цієї ширини рядки KPI переходять з одної колонки (стовпчиком) у дві (пліч-о-пліч). */
private const val WideWidthThreshold = 260

/**
 * Чиста, без сайд-ефектів composable-функція — дані приходять готовими
 * з [DebtSummaryWidget.provideGlance], бо suspend-виклики (Koin, Flow.first)
 * не можна робити напряму всередині @Composable. Значення параметрів за
 * замовчуванням дають змогу рендерити @Preview в Android Studio.
 *
 * Іконка + сума в один рядок — той самий мотив (гліф і кольорове коло), що й
 * у [org.bigblackowl.debttracker.ui.screens.stats.StatsScreen] `KpiCard`.
 * Всі розміри порахувані від [LocalSize.current], тому вигляд плавно
 * підлаштовується під зміну розміру віджета на робочому столі.
 *
 * Рядки KPI лежать у [LazyVerticalGrid]: на вузькому віджеті — одна колонка
 * (рядки стовпчиком, як раніше), на широкому — дві колонки (рядки пліч-о-пліч).
 * [GridCells.Fixed] обрано замість [GridCells.Adaptive] свідомо — Adaptive
 * потребує API 31+, а minSdk застосунку — 26.
 */
@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalGlancePreviewApi::class)
@SuppressLint("RestrictedApi")
@Preview(300,150)
@Composable
@GlanceComposable
fun WidgetUi(
    debtorsAmount: String = "1 250,00 ₴",
    creditorsAmount: String = "430,00 ₴",
    debtorsDescription: String = "Мені винні: $debtorsAmount",
    creditorsDescription: String = "Мій борг: $creditorsAmount",
) {
    val size = LocalSize.current
    val scale = (size.height / ReferenceHeight.dp).coerceIn(0.55f, 1.6f)

    val badgeSize = (BaseBadgeSize.dp * scale).coerceIn(22.dp, 44.dp)
    val iconSize = badgeSize * 0.55f
    val amountFontSize = (BaseFontSize * scale).coerceIn(13f, 22f).sp
    val outerPadding = (BasePadding.dp * scale).coerceIn(8.dp, 16.dp)

    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(AppActivity::class.java))
                .padding(outerPadding),
            contentAlignment = Alignment.Center
        ) {
            LazyVerticalGrid(
                gridCells = GridCells.Adaptive(200.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                item {
                    KpiRow(
                        icon = R.drawable.ic_widget_trend_down,
                        amount = debtorsAmount,
                        contentDescription = debtorsDescription,
                        accentColor = RepayColor,
                        badgeColor = RepayBadgeColor,
                        badgeSize = badgeSize,
                        iconSize = iconSize,
                        fontSize = amountFontSize,
                    )
                }
                item {
                    KpiRow(
                        icon = R.drawable.ic_widget_trend_up,
                        amount = creditorsAmount,
                        contentDescription = creditorsDescription,
                        accentColor = DebtColor,
                        badgeColor = DebtBadgeColor,
                        badgeSize = badgeSize,
                        iconSize = iconSize,
                        fontSize = amountFontSize,
                    )
                }
            }
        }
    }
}

/** Кольорове коло з іконкою (як у KpiCard) + сума поряд у тому ж рядку. */
@SuppressLint("RestrictedApi")
@Composable
private fun KpiRow(
    icon: Int,
    amount: String,
    contentDescription: String,
    accentColor: ColorProvider,
    badgeColor: ColorProvider,
    badgeSize: Dp,
    iconSize: Dp,
    fontSize: TextUnit,
    modifier: GlanceModifier = GlanceModifier.padding(vertical = 5.dp),
) {
    Row(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
        Box(
            modifier = GlanceModifier
                .size(badgeSize)
                .cornerRadius(badgeSize / 2)
                .background(badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = contentDescription,
                colorFilter = ColorFilter.tint(accentColor),
                modifier = GlanceModifier.size(iconSize),
            )
        }
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = amount,
            style = TextStyle(
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
            ),
        )
    }
}
