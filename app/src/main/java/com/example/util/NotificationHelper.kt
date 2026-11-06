package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PendingTransactionDto
import com.example.data.sbi.SbiRepository
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * Heads-up notifications for pending bank transactions.
 * Notifications NEVER interrupt the current app and are not auto-removed by us except when the user answers.
 * Swiping one away only clears the notification: the transaction stays PENDING and is still available in the app.
 */
object NotificationHelper {
  const val CHANNEL_PENDING = "pending_transactions"
  const val EXTRA_PENDING_ID = "pending_id"
  const val ACTION_SNOOZE = "com.example.action.SNOOZE_PENDING"
  private const val GROUP = "sbi_pending_group"
  private const val SUMMARY_ID = 9_000_001

  fun createChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val nm = context.getSystemService(NotificationManager::class.java)
      nm.createNotificationChannel(
        NotificationChannel(CHANNEL_PENDING, "New bank transactions", NotificationManager.IMPORTANCE_HIGH).apply {
          description = "Asks what a new credit or debit from your SBI statement was for"
          enableVibration(true)
        }
      )
    }
  }

  fun canNotify(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

  private fun notifId(pendingId: String) = pendingId.hashCode()

  fun money(amount: Double): String =
    "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2; maximumFractionDigits = 2 }.format(amount)

  fun question(item: PendingTransactionDto): String =
    if (item.isCredit) "${money(item.amount)} received. What was this money for?"
    else "New transaction detected. What was this ${money(item.amount)} spent on?"

  private fun openIntent(context: Context, pendingId: String): PendingIntent {
    val i = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_PENDING_ID, pendingId)
    }
    return PendingIntent.getActivity(context, notifId(pendingId), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  }

  private fun snoozeIntent(context: Context, pendingId: String): PendingIntent {
    val i = Intent(context, NotificationActionReceiver::class.java).apply {
      action = ACTION_SNOOZE
      putExtra(EXTRA_PENDING_ID, pendingId)
    }
    return PendingIntent.getBroadcast(context, notifId(pendingId) + 1, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  }

  fun show(context: Context, item: PendingTransactionDto) {
    if (!canNotify(context)) return
    createChannels(context)
    val title = if (item.isCredit) "Money received" else "New transaction detected"
    val text = question(item)
    val big = text + "\n" + (if (item.isCredit) "Credit" else "Debit") + " • " + item.transactionDate + (if (item.description.isNotBlank()) " • " + item.description else "")
    val n = NotificationCompat.Builder(context, CHANNEL_PENDING)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(big))
      .setPriority(NotificationCompat.PRIORITY_HIGH)          // heads-up on Android 7 and below
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)    // amounts stay hidden on the lock screen
      .setGroup(GROUP)
      .setAutoCancel(true)
      .setContentIntent(openIntent(context, item.id))
      .addAction(0, "Add details", openIntent(context, item.id))
      .addAction(0, "Snooze 1h", snoozeIntent(context, item.id))
      .build()
    try { NotificationManagerCompat.from(context).notify(notifId(item.id), n) } catch (_: SecurityException) {}
  }

  fun showSummary(context: Context, count: Int) {
    if (!canNotify(context) || count < 2) return
    val n = NotificationCompat.Builder(context, CHANNEL_PENDING)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("$count transactions need your input")
      .setContentText("Tell FinPulse what each one was for")
      .setGroup(GROUP).setGroupSummary(true).setAutoCancel(true)
      .setContentIntent(PendingIntent.getActivity(context, SUMMARY_ID, Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_PENDING_ID, "list")
      }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
      .build()
    try { NotificationManagerCompat.from(context).notify(SUMMARY_ID, n) } catch (_: SecurityException) {}
  }

  fun cancel(context: Context, pendingId: String) {
    NotificationManagerCompat.from(context).cancel(notifId(pendingId))
  }

  fun cancelSummary(context: Context) = NotificationManagerCompat.from(context).cancel(SUMMARY_ID)
}

/** Handles the "Snooze 1h" button. The transaction stays pending; only the reminder is hidden for an hour. */
class NotificationActionReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != NotificationHelper.ACTION_SNOOZE) return
    val id = intent.getStringExtra(NotificationHelper.EXTRA_PENDING_ID) ?: return
    val pending = goAsync()
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
      try {
        val repo = SbiRepository(context)
        repo.fetchPendingOne(id)?.let { repo.snooze(it, 60) }
        NotificationHelper.cancel(context, id)
      } finally { pending.finish() }
    }
  }
}
