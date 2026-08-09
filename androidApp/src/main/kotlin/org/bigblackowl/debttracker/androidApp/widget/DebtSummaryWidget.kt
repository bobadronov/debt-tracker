package org.bigblackowl.debttracker.androidApp.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
import org.bigblackowl.debttracker.androidApp.AppActivity
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

        val appName = strings.appName
        val debtorsLine = strings.widgetDebtorsTotal(debtorsTotal.formatTotals())
        val creditorsLine = strings.widgetCreditorsTotal(creditorsTotal.formatTotals())

        provideContent {
            WidgetUi(
                appName = appName,
                debtorsLine = debtorsLine,
                creditorsLine = creditorsLine,
            )
        }
    }
}

/**
 * Чиста, без сайд-ефектів composable-функція — дані приходять готовими
 * з [DebtSummaryWidget.provideGlance], бо suspend-виклики (Koin, Flow.first)
 * не можна робити напряму всередині @Composable. Значення параметрів за
 * замовчуванням дають змогу рендерити @Preview в Android Studio.
 *
 * Стиль: Material You / [GlanceTheme] — той самий "google-style" адаптивний
 * підхід, що й у системних віджетах (кольори підлаштовуються під шпалери на
 * Android 12+, інакше під світлу/темну тему), а не власна фіксована палітра.
 */
@SuppressLint("RestrictedApi")
@Preview
@Composable
@GlanceComposable
fun WidgetUi(
    appName: String = "DebtTracker",
    debtorsLine: String = "Мені винні: 1 250,00 ₴",
    creditorsLine: String = "Я винен: 430,00 ₴",
) {
    GlanceTheme {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(24.dp)
                .clickable(actionStartActivity(AppActivity::class.java))
                .padding(16.dp),
        ) {
            Column(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = appName,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    ),
                )

                Spacer(modifier = GlanceModifier.height(12.dp))

                StatLine(line = debtorsLine, valueColor = GlanceTheme.colors.primary)
                Spacer(modifier = GlanceModifier.height(8.dp))
                StatLine(line = creditorsLine, valueColor = GlanceTheme.colors.error)
            }
        }
    }
}

/** Розбиває рядок виду "Мітка: значення" на дві частини, аби виділити суму акцентним кольором. */
@SuppressLint("RestrictedApi")
@Composable
private fun StatLine(line: String, valueColor: ColorProvider) {
    val separatorIndex = line.indexOf(':')
    if (separatorIndex == -1) {
        Text(
            text = line,
            style = TextStyle(
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
        )
        return
    }
    val label = line.substring(0, separatorIndex + 1)
    val value = line.substring(separatorIndex + 1)
    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 13.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Text(
            text = value,
            style = TextStyle(
                color = valueColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            ),
        )
    }
}
