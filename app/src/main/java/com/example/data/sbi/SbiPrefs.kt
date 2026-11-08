package com.example.data.sbi

import android.content.Context
import com.example.data.model.CategoryDto2
import com.example.data.model.ConfirmationDto
import com.example.data.model.PendingTransactionDto
import com.example.data.model.SnoozeRequest
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Small local store for things that must survive the app being closed or the phone being offline:
 *  - answers the user typed while offline (sent later, idempotently)
 *  - snooze requests not yet delivered
 *  - which pending transactions already produced a notification
 *  - a copy of the pending list / categories so the form still opens without internet
 * It holds no passwords or tokens.
 */
class SbiPrefs(context: Context) {
  private val sp = context.applicationContext.getSharedPreferences("sbi_prefs", Context.MODE_PRIVATE)
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private fun <T> listAdapter(c: Class<T>): JsonAdapter<List<T>> =
    moshi.adapter(Types.newParameterizedType(List::class.java, c))

  private val mapAdapter: JsonAdapter<Map<String, Long>> =
    moshi.adapter(Types.newParameterizedType(Map::class.java, String::class.java, java.lang.Long::class.java))

  var railwayBaseUrl: String?
    get() = sp.getString("railway_url", null)
    set(v) { sp.edit().putString("railway_url", v).apply() }

  /** true while a "WhatsApp disconnected" alert is showing, so it is sent once per incident. */
  var whatsappAlerted: Boolean
    get() = sp.getBoolean("wa_alerted", false)
    set(v) { sp.edit().putBoolean("wa_alerted", v).apply() }

  // Home-screen widget snapshot (written after every sync, read by the widget)
  var widgetBalance: String?
    get() = sp.getString("w_balance", null)
    set(v) { sp.edit().putString("w_balance", v).apply() }
  var widgetChange: String?
    get() = sp.getString("w_change", null)
    set(v) { sp.edit().putString("w_change", v).apply() }
  var widgetPending: Int
    get() = sp.getInt("w_pending", 0)
    set(v) { sp.edit().putInt("w_pending", v).apply() }

  var lastSyncAt: Long
    get() = sp.getLong("last_sync_at", 0L)
    set(v) { sp.edit().putLong("last_sync_at", v).apply() }

  // ---- offline answers ----
  @Synchronized fun answerQueue(): List<ConfirmationDto> =
    sp.getString("answers", null)?.let { runCatching { listAdapter(ConfirmationDto::class.java).fromJson(it) }.getOrNull() } ?: emptyList()

  @Synchronized fun saveAnswerQueue(list: List<ConfirmationDto>) {
    sp.edit().putString("answers", listAdapter(ConfirmationDto::class.java).toJson(list)).apply()
  }

  @Synchronized fun enqueueAnswer(c: ConfirmationDto) {
    saveAnswerQueue(answerQueue().filter { it.pendingTransactionId != c.pendingTransactionId } + c)
  }

  // ---- snoozes not delivered yet ----
  @Synchronized fun snoozeQueue(): List<SnoozeRequest> =
    sp.getString("snoozes", null)?.let { runCatching { listAdapter(SnoozeRequest::class.java).fromJson(it) }.getOrNull() } ?: emptyList()

  @Synchronized fun saveSnoozeQueue(list: List<SnoozeRequest>) {
    sp.edit().putString("snoozes", listAdapter(SnoozeRequest::class.java).toJson(list)).apply()
  }

  // ---- cached pending list (for offline form) ----
  @Synchronized fun cachedPending(): List<PendingTransactionDto> =
    sp.getString("pending_cache", null)?.let { runCatching { listAdapter(PendingTransactionDto::class.java).fromJson(it) }.getOrNull() } ?: emptyList()

  @Synchronized fun saveCachedPending(list: List<PendingTransactionDto>) {
    sp.edit().putString("pending_cache", listAdapter(PendingTransactionDto::class.java).toJson(list)).apply()
  }

  // ---- categories cache ----
  fun cachedCategories(): List<CategoryDto2> =
    sp.getString("categories", null)?.let { runCatching { listAdapter(CategoryDto2::class.java).fromJson(it) }.getOrNull() } ?: emptyList()

  fun saveCategories(list: List<CategoryDto2>) {
    sp.edit().putString("categories", listAdapter(CategoryDto2::class.java).toJson(list)).apply()
  }

  // ---- notification bookkeeping: pendingId -> epoch millis when we last notified ----
  @Synchronized fun notified(): Map<String, Long> =
    sp.getString("notified", null)?.let { runCatching { mapAdapter.fromJson(it) }.getOrNull() } ?: emptyMap()

  @Synchronized fun saveNotified(m: Map<String, Long>) {
    sp.edit().putString("notified", mapAdapter.toJson(m)).apply()
  }

  // ---- local snooze (hides the notification even while offline): pendingId -> until epoch millis ----
  @Synchronized fun localSnoozes(): Map<String, Long> =
    sp.getString("local_snooze", null)?.let { runCatching { mapAdapter.fromJson(it) }.getOrNull() } ?: emptyMap()

  @Synchronized fun saveLocalSnoozes(m: Map<String, Long>) {
    sp.edit().putString("local_snooze", mapAdapter.toJson(m)).apply()
  }

  fun clearAll() { sp.edit().clear().apply() }
}
