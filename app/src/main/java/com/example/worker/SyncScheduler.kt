package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.sbi.Outcome
import com.example.data.sbi.SbiRepository
import com.example.util.NotificationHelper
import java.util.concurrent.TimeUnit

/**
 * Background synchronisation.
 *  - Periodic (every 15 min, the Android minimum) whenever internet is available. WorkManager keeps it
 *    across app restarts and phone reboots, so a phone that was OFF for hours catches up as soon as it is
 *    ON and online again.
 *  - syncSoon(): one extra run as soon as a connection exists (used after an offline answer).
 * Note: this is polling. Instant delivery would need Firebase Cloud Messaging (not included).
 */
object SyncScheduler {
  private const val PERIODIC = "sbi_pending_periodic"
  private const val ONCE = "sbi_pending_once"
  private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

  fun schedule(context: Context) {
    val req = PeriodicWorkRequestBuilder<PendingSyncWorker>(15, TimeUnit.MINUTES)
      .setConstraints(online)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
      .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, req)
    syncSoon(context)
  }

  fun syncSoon(context: Context) {
    val req = OneTimeWorkRequestBuilder<PendingSyncWorker>()
      .setConstraints(online)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
      .build()
    WorkManager.getInstance(context).enqueueUniqueWork(ONCE, ExistingWorkPolicy.REPLACE, req)
  }

  fun cancelAll(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(PERIODIC)
    WorkManager.getInstance(context).cancelUniqueWork(ONCE)
  }
}

/** Fetches ALL pending transactions (oldest first, none skipped), uploads queued answers, raises notifications. */
class PendingSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  override suspend fun doWork(): Result {
    val repo = SbiRepository(applicationContext)
    return when (val r = repo.syncPending()) {
      is Outcome.Ok -> {
        PendingNotifier.process(applicationContext, repo, r.data.pending)
        PendingNotifier.checkConnection(applicationContext, repo)
        Result.success()
      }
      is Outcome.Offline -> Result.retry()
      is Outcome.AuthRequired -> Result.success()     // user must log in again; nothing to retry
      is Outcome.Failed -> if (r.httpStatus in 500..599 || r.httpStatus == 0) Result.retry() else Result.success()
    }
  }
}

/** Raises one heads-up notification per NEW pending transaction (never twice, never for snoozed ones). */
object PendingNotifier {
  /** Tells the user (once per incident) when their WhatsApp link was lost while automatic tracking is on; keeps the widget fresh. */
  suspend fun checkConnection(context: Context, repo: SbiRepository) {
    val prefs = com.example.data.sbi.SbiPrefs(context)
    when (val o = repo.overview()) {
      is Outcome.Ok -> {
        val ov = o.data
        val broken = ov.automation.isEnabled && ov.whatsapp.status == "DISCONNECTED"
        if (broken) {
          if (!prefs.whatsappAlerted && NotificationHelper.showWhatsappDisconnected(context)) prefs.whatsappAlerted = true
        } else if (ov.whatsapp.status == "CONNECTED" || !ov.automation.isEnabled) {
          if (prefs.whatsappAlerted) { prefs.whatsappAlerted = false; NotificationHelper.cancelWhatsappDisconnected(context) }
        }
        prefs.widgetBalance = ov.balance.currentBalance?.let { com.example.ui.screens.sbi.SbiFmt.rupees(it) }
        prefs.widgetChange = ov.balance.balanceDifference?.let { com.example.ui.screens.sbi.SbiFmt.signedRupees(it) }
        prefs.widgetPending = ov.pendingCount
      }
      else -> {}
    }
    com.example.widget.BalanceWidgetProvider.refresh(context)
  }

  fun process(context: Context, repo: SbiRepository, items: List<com.example.data.model.PendingTransactionDto>) {
    repo.retainNotified(items.map { it.id }.toSet())
    val fresh = repo.itemsToNotify(items)
    val shown = fresh.filter { NotificationHelper.show(context, it) }
    if (shown.isNotEmpty()) repo.markNotified(shown.map { it.id })
    if (items.size >= 2) NotificationHelper.showSummary(context, items.size) else NotificationHelper.cancelSummary(context)
  }
}
