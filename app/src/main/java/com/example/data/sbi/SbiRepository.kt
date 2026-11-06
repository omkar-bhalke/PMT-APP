package com.example.data.sbi

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.AutomationDto
import com.example.data.model.AutomationLogDto
import com.example.data.model.BalanceHistoryItem
import com.example.data.model.CategoryDto2
import com.example.data.model.ConfirmationDto
import com.example.data.model.OverviewResponse
import com.example.data.model.PendingSyncRequest
import com.example.data.model.PendingTransactionDto
import com.example.data.model.RealBalanceResponse
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.RwIntervalRequest
import com.example.data.model.RwPairRequest
import com.example.data.model.RwSettingsRequest
import com.example.data.model.RwStartRequest
import com.example.data.model.RwStatusResponse
import com.example.data.model.RwAutomationStatusResponse
import com.example.data.model.SnoozeRequest
import com.example.data.model.SyncStatus
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.remote.NetworkClient
import com.example.data.remote.RailwayApiService
import com.example.data.remote.RailwayClient
import com.example.data.security.DeviceSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class Outcome<out T> {
  data class Ok<T>(val data: T) : Outcome<T>()
  data class Offline(val message: String = "No internet connection") : Outcome<Nothing>()
  object AuthRequired : Outcome<Nothing>()
  data class Failed(val message: String, val code: String? = null, val httpStatus: Int = 0) : Outcome<Nothing>()
}

data class SyncSummary(
  val pending: List<PendingTransactionDto>,
  val submitted: Int,
  val balance: Double?,
  val balanceDifference: Double?
)

/**
 * Single place for all SBI-automation network calls.
 *  - cPanel (permanent data) via NetworkClient, Railway (WhatsApp / schedule) via RailwayClient
 *  - one automatic token refresh on 401
 *  - answers typed offline are queued locally and sent idempotently by syncPending()
 */
class SbiRepository(context: Context) {
  private val ctx = context.applicationContext
  private val prefs = SbiPrefs(ctx)
  private val pref = PreferenceManager(ctx)

  // ---------------------------------------------------------------- session
  suspend fun ensureSession(): Boolean {
    val s = pref.userSettingsFlow.first()
    if (s.authToken.isNullOrBlank() || s.userId.isBlank()) return false
    NetworkClient.setBaseUrl(s.backendUrl.ifBlank { com.example.BuildConfig.CPANEL_BASE_URL })
    NetworkClient.setAuthToken(s.authToken)
    NetworkClient.setRefreshToken(s.refreshToken)
    NetworkClient.setDeviceId(s.deviceId.ifBlank { DeviceSecurityManager.getOrCreateDeviceId(ctx) })
    return true
  }

  private suspend fun refreshToken(): Boolean {
    val rt = NetworkClient.currentRefreshToken() ?: return false
    return try {
      val r = NetworkClient.apiService.refreshToken(RefreshTokenRequest(rt))
      val newToken = r.body()?.token
      if (r.isSuccessful && !newToken.isNullOrBlank()) {
        val s = pref.userSettingsFlow.first()
        NetworkClient.setAuthToken(newToken)
        pref.saveAuthProfile(s.userId, s.userEmail, s.userName, s.userPhotoUrl, newToken, rt, s.deviceId.ifBlank { null })
        true
      } else false
    } catch (_: Exception) { false }
  }

  private fun errorMessage(r: Response<*>): Pair<String, String?> {
    return try {
      val raw = r.errorBody()?.string().orEmpty()
      val j = JSONObject(raw)
      j.optString("message", "Request failed (${r.code()})") to j.optString("code").ifBlank { null }
    } catch (_: Exception) { "Request failed (${r.code()})" to null }
  }

  private suspend fun <T> call(block: suspend () -> Response<T>): Outcome<T> = withContext(Dispatchers.IO) {
    if (!ensureSession()) return@withContext Outcome.AuthRequired
    try {
      var r = block()
      if (r.code() == 401 && refreshToken()) r = block()
      when {
        r.isSuccessful && r.body() != null -> Outcome.Ok(r.body()!!)
        r.code() == 401 -> Outcome.AuthRequired
        else -> { val (m, c) = errorMessage(r); Outcome.Failed(m, c, r.code()) }
      }
    } catch (e: IOException) {
      Outcome.Offline(e.localizedMessage ?: "No internet connection")
    } catch (e: Exception) {
      Outcome.Failed(e.localizedMessage ?: "Unexpected error")
    }
  }

  // ---------------------------------------------------------------- railway discovery
  private suspend fun railway(): Outcome<RailwayApiService> {
    if (!RailwayClient.isConfigured()) RailwayClient.configure(prefs.railwayBaseUrl)
    if (!RailwayClient.isConfigured()) {
      when (val cfg = call { NetworkClient.apiService.getAppConfig() }) {
        is Outcome.Ok -> {
          val u = cfg.data.railwayBaseUrl
          if (!u.isNullOrBlank() && !u.contains("CHANGE_ME")) { prefs.railwayBaseUrl = u; RailwayClient.configure(u) }
        }
        is Outcome.Offline -> return cfg
        is Outcome.AuthRequired -> return cfg
        is Outcome.Failed -> {}
      }
    }
    val api = RailwayClient.api()
      ?: return Outcome.Failed("Railway URL is not configured. Ask the admin to set RAILWAY_PUBLIC_URL on cPanel, or enter it under Advanced.", "RAILWAY_NOT_CONFIGURED")
    return Outcome.Ok(api)
  }

  fun setRailwayUrlManually(url: String) {
    val u = url.trim()
    prefs.railwayBaseUrl = u
    RailwayClient.configure(u)
  }
  fun currentRailwayUrl(): String? = prefs.railwayBaseUrl

  private suspend fun <T> rw(block: suspend (RailwayApiService) -> Response<T>): Outcome<T> {
    val api = when (val a = railway()) {
      is Outcome.Ok -> a.data
      is Outcome.Offline -> return a
      is Outcome.AuthRequired -> return a
      is Outcome.Failed -> return a
    }
    return call { block(api) }
  }

  // ---------------------------------------------------------------- Railway: WhatsApp + automation
  suspend fun whatsappStatus(): Outcome<RwStatusResponse> = rw { it.whatsappStatus() }
  suspend fun startQrLinking(): Outcome<RwStatusResponse> = rw { it.startQrLinking() }
  suspend fun requestPairingCode(phone: String) = rw { it.requestPairingCode(RwPairRequest(phone.trim())) }
  suspend fun disconnectWhatsapp() = rw { it.disconnect() }
  suspend fun automationStatus(): Outcome<RwAutomationStatusResponse> = rw { it.automationStatus() }
  suspend fun startAutomation(minutes: Int?, importInitial: Boolean?) = rw { it.startAutomation(RwStartRequest(minutes, importInitial)) }
  suspend fun stopAutomation() = rw { it.stopAutomation() }
  suspend fun updateInterval(minutes: Int) = rw { it.updateInterval(RwIntervalRequest(minutes)) }
  suspend fun setImportInitial(v: Boolean) = rw { it.updateSettings(RwSettingsRequest(v)) }
  suspend fun runNow() = rw { it.runNow() }

  // ---------------------------------------------------------------- cPanel: read model
  suspend fun overview(): Outcome<OverviewResponse> = call { NetworkClient.apiService.getAutomationOverview() }
  suspend fun balance(): Outcome<RealBalanceResponse> = call { NetworkClient.apiService.getRealBalance() }

  suspend fun balanceHistory(limit: Int = 30): Outcome<List<BalanceHistoryItem>> =
    when (val r = call { NetworkClient.apiService.getBalanceHistory(limit) }) {
      is Outcome.Ok -> Outcome.Ok(r.data.history)
      is Outcome.Offline -> r
      is Outcome.AuthRequired -> r
      is Outcome.Failed -> r
    }

  suspend fun logs(limit: Int = 50): Outcome<List<AutomationLogDto>> =
    when (val r = call { NetworkClient.apiService.getAutomationLogs(limit) }) {
      is Outcome.Ok -> Outcome.Ok(r.data.logs)
      is Outcome.Offline -> r
      is Outcome.AuthRequired -> r
      is Outcome.Failed -> r
    }

  // ---------------------------------------------------------------- categories
  suspend fun categories(): List<CategoryDto2> {
    when (val r = call { NetworkClient.apiService.getCategories() }) {
      is Outcome.Ok -> if (r.data.categories.isNotEmpty()) { prefs.saveCategories(r.data.categories); return r.data.categories }
      else -> {}
    }
    return prefs.cachedCategories().ifEmpty { BUILTIN_CATEGORIES }
  }

  // ---------------------------------------------------------------- pending queue
  /** What the phone currently shows as pending (server copy minus answers still waiting to be uploaded). */
  fun cachedPending(): List<PendingTransactionDto> {
    val answered = prefs.answerQueue().map { it.pendingTransactionId }.toSet()
    return prefs.cachedPending().filter { it.id !in answered }
  }

  fun findCachedPending(id: String): PendingTransactionDto? = prefs.cachedPending().firstOrNull { it.id == id }

  suspend fun fetchPendingOne(id: String): PendingTransactionDto? {
    findCachedPending(id)?.let { return it }
    return when (val r = call { NetworkClient.apiService.getPendingTransaction(id) }) {
      is Outcome.Ok -> r.data.item
      else -> null
    }
  }

  fun isLocallySnoozed(id: String): Boolean = (prefs.localSnoozes()[id] ?: 0L) > System.currentTimeMillis()

  /**
   * Push queued answers + snoozes, then pull the full pending list. Safe to call repeatedly (idempotent on the server).
   * Never drops an unanswered transaction.
   */
  suspend fun syncPending(): Outcome<SyncSummary> {
    // 1) deliver snoozes (best effort)
    val snoozes = prefs.snoozeQueue()
    if (snoozes.isNotEmpty()) {
      val left = mutableListOf<SnoozeRequest>()
      for (s in snoozes) {
        when (call { NetworkClient.apiService.snoozePendingTransaction(s) }) {
          is Outcome.Ok -> {}
          is Outcome.Failed -> {}              // e.g. already answered: drop
          else -> left.add(s)
        }
      }
      prefs.saveSnoozeQueue(left)
    }

    // 2) answers + list
    val queue = prefs.answerQueue()
    val before = prefs.cachedPending()
    val res = call { NetworkClient.apiService.syncPendingTransactions(PendingSyncRequest(queue)) }
    return when (res) {
      is Outcome.Ok -> {
        val body = res.data
        val done = body.results.filter { it.status == "CONFIRMED" || it.status == "ALREADY_CONFIRMED" || it.status == "NOT_FOUND" || it.status == "INVALID" }
          .map { it.pendingTransactionId }.toSet()
        // mirror confirmed answers into the local ledger (marked SYNCED so they are not uploaded twice)
        for (r in body.results) {
          if ((r.status == "CONFIRMED" || r.status == "ALREADY_CONFIRMED") && r.transactionId != null) {
            val answer = queue.firstOrNull { it.pendingTransactionId == r.pendingTransactionId }
            val item = before.firstOrNull { it.id == r.pendingTransactionId }
            if (answer != null && item != null) mirrorToLocalLedger(item, answer, r.transactionId)
          }
        }
        prefs.saveAnswerQueue(queue.filter { it.pendingTransactionId !in done })
        prefs.saveCachedPending(body.items)
        prefs.lastSyncAt = System.currentTimeMillis()
        Outcome.Ok(SyncSummary(cachedPending(), body.results.count { it.status == "CONFIRMED" }, body.balance, body.balanceDifference))
      }
      is Outcome.Offline -> res
      is Outcome.AuthRequired -> res
      is Outcome.Failed -> res
    }
  }

  /** The user's answer. Saved on the phone first, so nothing is lost if the network drops. */
  suspend fun submitAnswer(item: PendingTransactionDto, category: String, purpose: String): Outcome<SyncSummary> {
    prefs.enqueueAnswer(ConfirmationDto(item.id, category.trim(), purpose.trim()))
    // keep a copy of the item so the ledger can be updated even if the list is refreshed meanwhile
    if (prefs.cachedPending().none { it.id == item.id }) prefs.saveCachedPending(prefs.cachedPending() + item)
    com.example.util.NotificationHelper.cancel(ctx, item.id)
    val r = syncPending()
    if (r !is Outcome.Ok) com.example.worker.SyncScheduler.syncSoon(ctx)     // retry when internet is back
    return r
  }

  suspend fun snooze(item: PendingTransactionDto, minutes: Int) {
    prefs.saveLocalSnoozes(prefs.localSnoozes() + (item.id to System.currentTimeMillis() + minutes * 60_000L))
    prefs.saveSnoozeQueue(prefs.snoozeQueue() + SnoozeRequest(item.id, minutes))
    com.example.util.NotificationHelper.cancel(ctx, item.id)
    val r = syncPending()
    if (r !is Outcome.Ok) com.example.worker.SyncScheduler.syncSoon(ctx)
  }

  // ---------------------------------------------------------------- notification bookkeeping
  /** Items that should produce a (new) notification now: never notified before, or a snooze just expired. */
  fun itemsToNotify(items: List<PendingTransactionDto>): List<PendingTransactionDto> {
    val seen = prefs.notified()
    val snoozes = prefs.localSnoozes()
    val now = System.currentTimeMillis()
    return items.filter { item ->
      val localUntil = snoozes[item.id] ?: 0L
      when {
        item.isSnoozed || localUntil > now -> false
        item.id !in seen -> true
        localUntil in 1..now && (seen[item.id] ?: 0L) < localUntil -> true
        else -> false
      }
    }
  }

  fun markNotified(ids: Collection<String>) {
    val m = prefs.notified().toMutableMap()
    val now = System.currentTimeMillis()
    ids.forEach { m[it] = now }
    prefs.saveNotified(m)
  }

  fun retainNotified(stillPending: Set<String>) {
    prefs.saveNotified(prefs.notified().filterKeys { it in stillPending })
    prefs.saveLocalSnoozes(prefs.localSnoozes().filterKeys { it in stillPending })
  }

  // ---------------------------------------------------------------- local ledger
  private suspend fun mirrorToLocalLedger(item: PendingTransactionDto, answer: ConfirmationDto, serverTxId: String) {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    val millis = runCatching { fmt.parse(item.transactionDate + " 12:00")?.time }.getOrNull() ?: System.currentTimeMillis()
    val entity = TransactionEntity(
      id = serverTxId,
      serverId = serverTxId,
      type = if (item.isCredit) TransactionType.INCOME else TransactionType.EXPENSE,
      amount = item.amount,
      description = answer.purpose,
      categoryName = answer.categoryName,
      dateMillis = millis,
      dateString = item.transactionDate,
      timeString = "12:00",
      notes = "Bank entry: ${item.description}",
      syncStatus = SyncStatus.SYNCED,
      idempotencyKey = "sbi_${item.id}"
    )
    AppDatabase.getDatabase(ctx).transactionDao().insertTransaction(entity)
  }

  companion object {
    val BUILTIN_CATEGORIES = listOf(
      CategoryDto2("b1", "Food & Dining", "EXPENSE"), CategoryDto2("b2", "Groceries", "EXPENSE"),
      CategoryDto2("b3", "Transportation", "EXPENSE"), CategoryDto2("b4", "Utilities & Bills", "EXPENSE"),
      CategoryDto2("b5", "Entertainment", "EXPENSE"), CategoryDto2("b6", "Healthcare", "EXPENSE"),
      CategoryDto2("b7", "Education & Books", "EXPENSE"), CategoryDto2("b8", "Other", "EXPENSE"),
      CategoryDto2("b9", "Salary & Allowance", "INCOME"), CategoryDto2("b10", "Investments & Gifts", "INCOME"),
      CategoryDto2("b11", "Refunds", "INCOME"), CategoryDto2("b12", "Other Income", "INCOME")
    )
  }
}
