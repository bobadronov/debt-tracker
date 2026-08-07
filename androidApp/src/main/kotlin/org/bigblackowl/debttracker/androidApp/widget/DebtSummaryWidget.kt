package org.bigblackowl.debttracker.androidApp.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.flow.first
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
 *
 * Оформлення: cyberpunk neon-on-black. Glance не має border-модифікатора, тому
 * рамка симулюється вкладеним Box — зовнішній пофарбований у неон, з відступом
 * у 2dp просвічує тонким кільцем навколо темнішого внутрішнього контейнера.
 */
private val CyberPanel = Color(0xFF0F0B1E)
private val CyberCyan = Color(0xFF00F0FF)
private val CyberMagenta = Color(0xFFFF2AD4)
private val CyberGreen = Color(0xFF39FF14)
private val CyberPink = Color(0xFFFF2079)
private val CyberDim = Color(0xFF7C7CA8)

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

        val debtorsLine = strings.widgetDebtorsTotal(debtorsTotal.formatTotals())
        val creditorsLine = strings.widgetCreditorsTotal(creditorsTotal.formatTotals())

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(CyberMagenta)
                    .cornerRadius(18.dp)
                    .padding(2.dp),
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(CyberPanel)
                        .cornerRadius(16.dp)
                        .padding(12.dp),
                ) {
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                            Text(
                                text = "▮",
                                style = TextStyle(
                                    color = ColorProvider(CyberCyan),
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Spacer(modifier = GlanceModifier.width(6.dp))
                            Text(
                                text = strings.appName.uppercase(),
                                style = TextStyle(
                                    color = ColorProvider(CyberCyan),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                ),
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(CyberMagenta),
                        ) {}
                        Spacer(modifier = GlanceModifier.height(10.dp))

                        StatLine(line = debtorsLine, valueColor = CyberGreen)
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        StatLine(line = creditorsLine, valueColor = CyberPink)
                    }
                }
            }
        }
    }
}

/** Розбиває рядок виду "Мітка: значення" на дві частини, аби виділити суму неоновим кольором. */
@SuppressLint("RestrictedApi")
@androidx.compose.runtime.Composable
private fun StatLine(line: String, valueColor: Color) {
    val separatorIndex = line.indexOf(':')
    if (separatorIndex == -1) {
        Text(
            text = line,
            style = TextStyle(
                color = ColorProvider(valueColor),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
        )
        return
    }
    val label = line.substring(0, separatorIndex + 1)
    val value = line.substring(separatorIndex + 1)
    Row {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(CyberDim),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            ),
        )
        Text(
            text = value,
            style = TextStyle(
                color = ColorProvider(valueColor),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
        )
    }
}
