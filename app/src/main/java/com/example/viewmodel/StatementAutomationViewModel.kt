package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AutomationDto
import com.example.data.model.AutomationLogDto
import com.example.data.model.BalanceHistoryItem
import com.example.data.model.CategoryDto2
import com.example.data.model.PendingTransactionDto
import com.example.data.model.RealBalanceResponse
import com.example.data.model.RwWhatsAppDto
import com.example.data.sbi.Outcome
import com.example.data.sbi.SbiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State holder for WhatsApp linking, automation settings, balance and the pending-transaction queue. */
class StatementAutomationViewModel(app: Application) : AndroidViewModel(app) {
  private val repo = SbiRepository(app)

  private val _pending = MutableStateFlow<List<PendingTransactionDto>>(repo.cachedPending())
  val pendingTransactions: StateFlow<List<PendingTransactionDto>> = _pending.asStateFlow()

  private val _balance = MutableStateFlow<RealBalanceResponse?>(null)
  val realBalance: StateFlow<RealBalanceResponse?> = _balance.asStateFlow()

  private val _whatsapp = MutableStateFlow(RwWhatsAppDto())
  val whatsapp: StateFlow<RwWhatsAppDto> = _whatsapp.asStateFlow()

  private val _automation = MutableStateFlow<AutomationDto?>(null)
  val automation: StateFlow<AutomationDto?> = _automation.asStateFlow()

  private val _logs = MutableStateFlow<List<AutomationLogDto>>(emptyList())
  val logs: StateFlow<List<AutomationLogDto>> = _logs.asStateFlow()

  private val _history = MutableStateFlow<List<BalanceHistoryItem>>(emptyList())
  val history: StateFlow<List<BalanceHistoryItem>> = _history.asStateFlow()

  private val _categories = MutableStateFlow<List<CategoryDto2>>(SbiRepository.BUILTIN_CATEGORIES)
  val categories: StateFlow<List<CategoryDto2>> = _categories.asStateFlow()

  private val _storagePersistent = MutableStateFlow(true)
  val storagePersistent: StateFlow<Boolean> = _storagePersistent.asStateFlow()

  private val _pairingCode = MutableStateFlow<String?>(null)
  val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

  private val _busy = MutableStateFlow(false)
  val busy: StateFlow<Boolean> = _busy.asStateFlow()

  /** Latest user-visible problem (cleared with clearError). Never silent: offline / server errors show here. */
  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  private val _info = MutableStateFlow<String?>(null)
  val info: StateFlow<String?> = _info.asStateFlow()

  private val _authRequired = MutableStateFlow(false)
  val authRequired: StateFlow<Boolean> = _authRequired.asStateFlow()

  fun clearError() { _error.value = null }
  fun clearInfo() { _info.value = null }
  fun currentRailwayUrl(): String? = repo.currentRailwayUrl()
  fun setRailwayUrl(url: String) { repo.setRailwayUrlManually(url) }

  private fun <T> handleFailure(o: Outcome<T>): Boolean {
    when (o) {
      is Outcome.Offline -> _error.value = "You are offline. ${o.message}"
      is Outcome.AuthRequired -> _authRequired.value = true
      is Outcome.Failed -> _error.value = o.message
      is Outcome.Ok -> return false
    }
    return true
  }

  // ------------------------------------------------------------ overview / balance
  fun refreshOverview() {
    viewModelScope.launch {
      val o = repo.overview()
      if (o is Outcome.Ok) {
        _automation.value = o.data.automation
        _balance.value = o.data.balance
        _whatsapp.value = _whatsapp.value.copy(
          state = if (_whatsapp.value.state in listOf("QR_READY", "PAIRING_CODE_READY", "CONNECTING")) _whatsapp.value.state else o.data.whatsapp.status,
          connected = o.data.whatsapp.status == "CONNECTED",
          phoneMasked = o.data.whatsapp.phoneMasked,
          lastError = o.data.whatsapp.lastError
        )
      } else handleFailure(o)
    }
  }

  fun refreshBalanceAndHistory() {
    viewModelScope.launch {
      (repo.balance() as? Outcome.Ok)?.let { _balance.value = it.data }
      val h = repo.balanceHistory(50)
      if (h is Outcome.Ok) _history.value = h.data else handleFailure(h)
    }
  }

  fun refreshLogs() {
    viewModelScope.launch {
      val l = repo.logs(60)
      if (l is Outcome.Ok) _logs.value = l.data else handleFailure(l)
    }
  }

  // ------------------------------------------------------------ WhatsApp (Railway)
  /** Live status from the Railway worker (QR code / pairing code / Connected / Reconnecting). */
  fun refreshWhatsapp(quiet: Boolean = true) {
    viewModelScope.launch {
      val s = repo.whatsappStatus()
      if (s is Outcome.Ok) {
        _whatsapp.value = s.data.whatsapp
        _storagePersistent.value = s.data.storagePersistent
        if (s.data.whatsapp.state != "PAIRING_CODE_READY" && s.data.whatsapp.state != "QR_READY") _pairingCode.value = null
      } else if (!quiet) handleFailure(s)
    }
  }

  fun startQrLinking() {
    viewModelScope.launch {
      _busy.value = true
      val s = repo.startQrLinking()
      if (s is Outcome.Ok) _whatsapp.value = s.data.whatsapp else handleFailure(s)
      _busy.value = false
    }
  }

  fun requestPairingCode(phone: String) {
    viewModelScope.launch {
      _busy.value = true
      val s = repo.requestPairingCode(phone)
      if (s is Outcome.Ok) {
        _pairingCode.value = s.data.pairingCode
        _whatsapp.value = _whatsapp.value.copy(state = "PAIRING_CODE_READY", pairingCode = s.data.pairingCode)
      } else handleFailure(s)
      _busy.value = false
    }
  }

  fun disconnectWhatsapp() {
    viewModelScope.launch {
      _busy.value = true
      val s = repo.disconnectWhatsapp()
      if (s is Outcome.Ok) {
        _whatsapp.value = RwWhatsAppDto()
        _info.value = "WhatsApp disconnected. You can link it again any time."
      } else handleFailure(s)
      _busy.value = false
      refreshOverview()
    }
  }

  // ------------------------------------------------------------ automation (Railway owns the schedule)
  fun setEnabled(enabled: Boolean, intervalMinutes: Int?) {
    viewModelScope.launch {
      _busy.value = true
      val r = if (enabled) repo.startAutomation(intervalMinutes, null) else repo.stopAutomation()
      if (r is Outcome.Ok) { r.data.automation?.let { _automation.value = it }; _info.value = r.data.message } else handleFailure(r)
      _busy.value = false
      refreshOverview(); refreshLogs()
    }
  }

  fun setInterval(minutes: Int) {
    viewModelScope.launch {
      _busy.value = true
      val r = repo.updateInterval(minutes)
      if (r is Outcome.Ok) { r.data.automation?.let { _automation.value = it }; _info.value = "Interval saved: every ${formatInterval(minutes)}" } else handleFailure(r)
      _busy.value = false
    }
  }

  fun setImportInitial(v: Boolean) {
    viewModelScope.launch {
      val r = repo.setImportInitial(v)
      if (r is Outcome.Ok) r.data.automation?.let { _automation.value = it } else handleFailure(r)
    }
  }

  fun runNow() {
    viewModelScope.launch {
      _busy.value = true
      val r = repo.runNow()
      if (r is Outcome.Ok) _info.value = r.data.message ?: "Fetch started" else handleFailure(r)
      _busy.value = false
      kotlinx.coroutines.delay(4000); refreshOverview(); refreshLogs()
    }
  }

  // ------------------------------------------------------------ pending queue
  fun syncPending() {
    viewModelScope.launch {
      val r = repo.syncPending()
      if (r is Outcome.Ok) {
        _pending.value = r.data.pending
        if (r.data.balance != null) _balance.value = (_balance.value ?: RealBalanceResponse(true, true)).copy(currentBalance = r.data.balance, balanceDifference = r.data.balanceDifference)
      } else if (r is Outcome.AuthRequired) _authRequired.value = true
      // Offline is not an error here: the cached list stays visible.
    }
  }

  /** Used while the app is open: sync, refresh the list and raise notifications for new items. */
  fun syncAndNotify() {
    viewModelScope.launch {
      val r = repo.syncPending()
      if (r is Outcome.Ok) {
        _pending.value = r.data.pending
        com.example.worker.PendingNotifier.process(getApplication(), repo, r.data.pending)
        (repo.balance() as? Outcome.Ok)?.let { _balance.value = it.data }
      } else if (r is Outcome.AuthRequired) _authRequired.value = true
    }
  }

  fun loadCategories() {
    viewModelScope.launch { _categories.value = repo.categories() }
  }

  fun isSnoozedLocally(id: String): Boolean = repo.isLocallySnoozed(id)

  suspend fun loadPendingItem(id: String): PendingTransactionDto? = repo.fetchPendingOne(id)

  /** Saves the answer (queued on the phone first), then returns the next pending item id or null. */
  fun submitAnswer(item: PendingTransactionDto, category: String, purpose: String, onSaved: (savedOffline: Boolean, nextId: String?) -> Unit) {
    viewModelScope.launch {
      _busy.value = true
      val r = repo.submitAnswer(item, category, purpose)
      _busy.value = false
      _pending.value = repo.cachedPending()
      onSaved(r !is Outcome.Ok, _pending.value.firstOrNull { it.id != item.id }?.id)
    }
  }

  fun snooze(item: PendingTransactionDto, minutes: Int) {
    viewModelScope.launch {
      repo.snooze(item, minutes)
      _pending.value = repo.cachedPending()
      _info.value = "Reminder snoozed for ${formatInterval(minutes)}. The transaction stays pending."
    }
  }

  companion object {
    fun formatInterval(min: Int): String = when {
      min % 1440 == 0 -> "${min / 1440} day" + if (min / 1440 > 1) "s" else ""
      min % 60 == 0 -> "${min / 60} hour" + if (min / 60 > 1) "s" else ""
      else -> "$min minutes"
    }
  }
}
