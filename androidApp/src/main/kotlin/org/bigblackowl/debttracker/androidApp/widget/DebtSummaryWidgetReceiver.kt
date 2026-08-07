package org.bigblackowl.debttracker.androidApp.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** `AppWidgetProvider` entry point Android's widget host binds to; delegates all rendering to [DebtSummaryWidget]. */
class DebtSummaryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DebtSummaryWidget()
}
