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
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinPulseWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    for (appWidgetId in appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId)
    }
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
    if (intent.action == ACTION_REFRESH_WIDGET) {
      val appWidgetManager = AppWidgetManager.getInstance(context)
      val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, FinPulseWidgetProvider::class.java))
      for (id in ids) {
        updateAppWidget(context, appWidgetManager, id)
      }
    }
  }

  companion object {
    const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

    fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
      val views = RemoteViews(context.packageName, R.layout.widget_finpulse_balance)

      // Intent to open App & Add Transaction
      val addIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("navigate_to", "add_transaction")
      }
      val addPendingIntent = PendingIntent.getActivity(
        context,
        1001,
        addIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

      // Intent to open Main Dashboard
      val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      val openAppPendingIntent = PendingIntent.getActivity(
        context,
        1002,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

      // Intent to refresh widget
      val refreshIntent = Intent(context, FinPulseWidgetProvider::class.java).apply {
        action = ACTION_REFRESH_WIDGET
      }
      val refreshPendingIntent = PendingIntent.getBroadcast(
        context,
        1003,
        refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
      views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

      // Fetch financial values asynchronously
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val prefs = PreferenceManager(context)
          val settings = prefs.userSettingsFlow.first()
          val db = AppDatabase.getDatabase(context)

          val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
          val todayExpenses = db.transactionDao().getTodayExpensesFlow(todayDate).first()
          val totalIncome = db.transactionDao().getTotalIncomeFlow().first()
          val totalExpenses = db.transactionDao().getTotalExpensesFlow().first()

          val calculatedBalance = settings.initialBalance + totalIncome - totalExpenses
          val dailyLimit = settings.dailySpendingLimit

          views.setTextViewText(R.id.widget_balance_text, "₹${String.format("%.2f", calculatedBalance)}")
          views.setTextViewText(
            R.id.widget_daily_spending_text,
            "Today: ₹${String.format("%.2f", todayExpenses)} • Limit: ₹${dailyLimit.toInt()}"
          )

          appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (_: Exception) {
          appWidgetManager.updateAppWidget(appWidgetId, views)
        }
      }
    }
  }
}
