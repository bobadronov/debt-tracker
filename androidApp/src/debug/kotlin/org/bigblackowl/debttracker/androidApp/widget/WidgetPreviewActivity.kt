package org.bigblackowl.debttracker.androidApp.widget

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import org.bigblackowl.debttracker.androidApp.R

/**
 * Debug-only host for [DebtSummaryWidgetReceiver]'s [RemoteViews]. Renders the real widget layout
 * (colours + localised labels come straight from the receiver's `buildViews`), then overrides the
 * two amount views with sample multi-currency totals so the layout can be screenshotted without
 * placing the widget on a launcher home screen.
 */
class WidgetPreviewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FF9AA0A6"))
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }

        try {
            val views = buildWidgetViews()
            views.setTextViewText(R.id.widget_amount_debtors, "1 200,00 ₴\n80,00 $\n450,00 €")
            views.setTextViewText(R.id.widget_amount_creditors, "620,00 ₴\n15,00 $")

            root.addView(label("170 x 120 dp (min, shrunk to 2 cells)"))
            root.addView(sizedHost(views, widthDp = 170, heightDp = 120))
            root.addView(spacer())
            root.addView(label("180 x 190 dp (2x3, default)"))
            root.addView(sizedHost(views, widthDp = 180, heightDp = 190))
            root.addView(spacer())
            root.addView(label("320 x 190 dp (4x3, default width max)"))
            root.addView(sizedHost(views, widthDp = 320, heightDp = 190))
        } catch (t: Throwable) {
            root.addView(TextView(this).apply { text = "preview failed: ${t.stackTraceToString()}" })
        }

        setContentView(root, ViewGroup.LayoutParams(MATCH, MATCH))
    }

    /** Reflectively calls the receiver companion's private `buildViews(Context, Amounts?)` with null. */
    private fun buildWidgetViews(): RemoteViews {
        val receiver = Class.forName(
            "org.bigblackowl.debttracker.androidApp.widget.DebtSummaryWidgetReceiver",
        )
        val companion = receiver.getDeclaredField("Companion").apply { isAccessible = true }.get(null)
        val buildViews = companion.javaClass.declaredMethods.first { it.name == "buildViews" }
            .apply { isAccessible = true }
        return buildViews.invoke(companion, applicationContext, null) as RemoteViews
    }

    private fun sizedHost(views: RemoteViews, widthDp: Int, heightDp: Int): FrameLayout {
        val host = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(widthDp), dp(heightDp))
        }
        host.addView(views.apply(applicationContext, host))
        return host
    }

    private fun spacer() = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(16))
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 11f
        setPadding(0, dp(4), 0, dp(2))
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics,
    ).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
