package com.example.ui.screens.restriction

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceManager
import com.example.data.model.AppRestrictionItem
import com.example.data.model.LogRestrictionEventRequest
import com.example.data.remote.NetworkClient
import com.example.data.sbi.SbiPrefs
import com.example.service.FinPulseAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Payment-app restriction rules.
 * IMPORTANT: only apps on this list (and switched ON) are ever touched. Every other app, the launcher,
 * the browser, calls etc. are never interrupted.
 */
object AppRestrictionManager {
  private const val PREFS_NAME = "finpulse_app_restrictions"
  private const val KEY_RESTRICTION_ENABLED = "restriction_feature_enabled"

  /** (package, name, blocked by default). Anything not listed here is never blocked. */
  val KNOWN_PAYMENT_PACKAGES = listOf(
    AppRestrictionItem("com.google.android.apps.nbu.paisa.user", "Google Pay", true),
    AppRestrictionItem("com.phonepe.app", "PhonePe", true),
    AppRestrictionItem("net.one97.paytm", "Paytm", true),
    AppRestrictionItem("in.org.npci.upiapp", "BHIM UPI", true),
    AppRestrictionItem("com.dreamplug.androidapp", "CRED", true),
    AppRestrictionItem("com.sbi.upi", "BHIM SBI Pay", true),
    AppRestrictionItem("com.sbi.lotusintouch", "YONO SBI", true),
    AppRestrictionItem("com.snapwork.hdfc", "HDFC Bank", true),
    AppRestrictionItem("com.csam.icici.bank.imobile", "iMobile Pay", true),
    AppRestrictionItem("com.axis.mobile", "Axis Mobile", true),
    AppRestrictionItem("com.freecharge.android", "Freecharge", true),
    AppRestrictionItem("com.mobikwik_new", "MobiKwik", true),
    AppRestrictionItem("com.bharatpe.app", "BharatPe", true),
    AppRestrictionItem("in.amazon.mShop.android.shopping", "Amazon (Pay)", false),
    AppRestrictionItem("com.whatsapp", "WhatsApp Payments", false)
  )

  private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isFeatureEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_RESTRICTION_ENABLED, true)
  fun setFeatureEnabled(context: Context, enabled: Boolean) { prefs(context).edit().putBoolean(KEY_RESTRICTION_ENABLED, enabled).apply() }

  /** True ONLY for a listed payment app that the user has switched on. */
  fun isPackageRestricted(context: Context, packageName: String): Boolean {
    if (!isFeatureEnabled(context)) return false
    val item = KNOWN_PAYMENT_PACKAGES.firstOrNull { it.packageName == packageName } ?: return false
    return prefs(context).getBoolean("pkg_$packageName", item.isEnabled)
  }

  fun setPackageRestricted(context: Context, packageName: String, restricted: Boolean) {
    prefs(context).edit().putBoolean("pkg_$packageName", restricted).apply()
  }

  fun isInstalled(context: Context, packageName: String): Boolean =
    try { context.packageManager.getPackageInfo(packageName, 0); true } catch (_: Exception) { false }

  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val expected = "${context.packageName}/${FinPulseAccessibilityService::class.java.canonicalName}"
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
      it.id.equals(expected, ignoreCase = true) ||
        it.resolveInfo?.serviceInfo?.name.equals(FinPulseAccessibilityService::class.java.canonicalName, ignoreCase = true)
    }
  }

  fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  }

  /** Android 13+ blocks accessibility for apps installed from outside the Play Store until this is allowed. */
  fun openAppInfo(context: Context) {
    context.startActivity(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  }

  /**
   * Today's spending = confirmed ledger expenses + bank debits of today that are still waiting for an answer
   * (a payment you just made counts immediately, even before you describe it).
   * @return Triple(limit reached, spent today, daily limit)
   */
  suspend fun checkSpendingLimitExceeded(context: Context): Triple<Boolean, Double, Double> = withContext(Dispatchers.IO) {
    try {
      val prefs = PreferenceManager(context)
      val settings = prefs.userSettingsFlow.first()
      val ist = TimeZone.getTimeZone("Asia/Kolkata")
      val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = ist }.format(java.util.Date())
      val localToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

      val db = AppDatabase.getDatabase(context)
      val ledger = maxOf(db.transactionDao().getTodayExpensesFlow(today).first(), db.transactionDao().getTodayExpensesFlow(localToday).first())
      val waiting = SbiPrefs(context).cachedPending()
        .filter { !it.isCredit && (it.transactionDate == today || it.transactionDate == localToday) }
        .sumOf { it.amount }
      val spent = ledger + waiting
      val limit = settings.dailySpendingLimit
      Triple(spent > limit, spent, limit)
    } catch (e: Exception) {
      Triple(false, 0.0, 0.0)
    }
  }

  suspend fun logEvent(
    context: Context, packageName: String, appName: String, dailyLimit: Double, amountSpent: Double, actionTaken: String = "BLOCKED"
  ) = withContext(Dispatchers.IO) {
    try {
      val s = PreferenceManager(context).userSettingsFlow.first()
      if (s.authToken.isNullOrBlank()) return@withContext
      NetworkClient.setAuthToken(s.authToken)
      NetworkClient.setDeviceId(s.deviceId.ifBlank { null })
      NetworkClient.apiService.logRestrictionEvent(
        LogRestrictionEventRequest(packageName, appName, dailyLimit, amountSpent, (amountSpent - dailyLimit).coerceAtLeast(0.0), actionTaken)
      )
    } catch (_: Exception) {}
  }
}
