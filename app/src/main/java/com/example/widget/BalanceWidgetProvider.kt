package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.sbi.SbiPrefs
import com.example.ui.screens.restriction.AppRestrictionManager
import com.example.ui.screens.sbi.SbiFmt
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Home-screen widget: balance, today's spending, entries waiting, and a one-tap "Add transaction". */
class BalanceWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
    refresh(context)
  }

  companion object {
    fun refresh(context: Context) {
      val app = context.applicationContext
      val manager = AppWidgetManager.getInstance(app)
      val ids = manager.getAppWidgetIds(ComponentName(app, BalanceWidgetProvider::class.java))
      if (ids.isEmpty()) return
      CoroutineScope(Dispatchers.IO).launch {
        val prefs = SbiPrefs(app)
        val (_, spent, limit) = AppRestrictionManager.checkSpendingLimitExceeded(app)
        val pending = prefs.cachedPending().size.takeIf { it > 0 } ?: prefs.widgetPending
        val views = RemoteViews(app.packageName, R.layout.widget_balance)
        views.setTextViewText(R.id.widget_balance, prefs.widgetBalance ?: ("₹" + String.format(java.util.Locale.US, "%,.0f", spent)))
        views.setTextViewText(
          R.id.widget_sub,
          (if (prefs.widgetBalance != null) "Bank balance" + (prefs.widgetChange?.let { "  ($it)" } ?: "") + "\n" else "Spent today\n") +
            (if (limit > 0) "Today ${SbiFmt.rupees(spent)} of ${SbiFmt.rupees(limit)}" else "Today ${SbiFmt.rupees(spent)}")
        )
        views.setTextViewText(R.id.widget_pending, if (pending > 0) (if (pending == 1) "1 entry needs your input" else "$pending entries need your input") else "")

        fun open(extra: String?, code: Int): PendingIntent = PendingIntent.getActivity(
          app, code,
          Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (extra != null) putExtra(NotificationHelper.EXTRA_PENDING_ID, extra)
          },
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, open(if (pending > 0) "list" else null, 71))
        views.setOnClickPendingIntent(R.id.widget_add, open("add", 72))
        manager.updateAppWidget(ids, views)
      }
    }
  }
}
