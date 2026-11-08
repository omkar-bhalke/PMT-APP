package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.ui.screens.restriction.AppRestrictionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/**
 * Spending Guardian.
 *
 * Only looks at WHICH app came to the front (no screen text is read: canRetrieveWindowContent=false).
 * If that app is one of the payment apps the user switched on AND today's limit is reached, a glass-style
 * reminder is drawn OVER that app only. It is a window of this service, not an activity, so:
 *   - FinPulse itself is never blocked or pushed into a strange task,
 *   - every other app (launcher, browser, calls, messages) stays completely untouched,
 *   - the reminder disappears by itself as soon as the user is on any other app or the home screen.
 */
class FinPulseAccessibilityService : AccessibilityService() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val handler = Handler(Looper.getMainLooper())
  private var wm: WindowManager? = null
  private var overlay: View? = null
  private var overlayPkg: String? = null
  private var shownAt = 0L
  private var autoHome: Runnable? = null
  private var lastLogged = HashMap<String, Long>()

  override fun onServiceConnected() {
    super.onServiceConnected()
    wm = getSystemService(WINDOW_SERVICE) as WindowManager
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
    val pkg = event.packageName?.toString() ?: return

    // Our own overlay, the notification shade and keyboards also raise "window changed": they must not close or open anything.
    val ignorable = pkg == packageName || pkg.startsWith("com.android.systemui") || pkg.contains("inputmethod") || pkg.contains("keyboard")

    if (overlay != null) {
      if (pkg == overlayPkg || ignorable) return
      if (System.currentTimeMillis() - shownAt < 600) return
      removeOverlay()                                     // user is somewhere else now
    }
    if (ignorable) return
    if (!AppRestrictionManager.isPackageRestricted(applicationContext, pkg)) return   // only the chosen payment apps

    scope.launch {
      val (reached, spent, limit) = AppRestrictionManager.checkSpendingLimitExceeded(applicationContext)
      if (!reached || overlay != null) return@launch
      val name = appName(pkg)
      val now = System.currentTimeMillis()
      if (now - (lastLogged[pkg] ?: 0L) > 60_000L) {
        lastLogged[pkg] = now
        launch { AppRestrictionManager.logEvent(applicationContext, pkg, name, limit, spent, "BLOCKED") }
      }
      showOverlay(pkg, name, spent, limit)
    }
  }

  // ------------------------------------------------------------------ overlay
  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
  private fun money(v: Double) = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(v)

  private fun pill(text: String, filled: Boolean, onClick: () -> Unit): TextView = TextView(this).apply {
    this.text = text
    setTextColor(0xFFFFFFFF.toInt()); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
    background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
      if (filled) intArrayOf(0xFF3B82F6.toInt(), 0xFF7C3AED.toInt()) else intArrayOf(0x33FFFFFF, 0x1AFFFFFF)).apply {
      cornerRadius = dp(18).toFloat(); setStroke(dp(1), 0x55FFFFFF)
    }
    setOnClickListener { onClick() }
    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(10) }
  }

  private fun showOverlay(pkg: String, appName: String, spent: Double, limit: Double) {
    val manager = wm ?: return
    removeOverlay()

    val iconWrap = FrameLayout(this).apply {
      background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0x33FB7185); setStroke(dp(1), 0x99FB7185.toInt()) }
      layoutParams = LinearLayout.LayoutParams(dp(68), dp(68))
      addView(ImageView(context).apply {
        setImageResource(android.R.drawable.ic_lock_idle_lock)
        setColorFilter(0xFFFB7185.toInt())
        layoutParams = FrameLayout.LayoutParams(dp(32), dp(32), Gravity.CENTER)
      })
    }
    fun label(t: String, size: Float, color: Int, bold: Boolean = false, top: Int = 0) = TextView(this).apply {
      text = t; textSize = size; setTextColor(color); gravity = Gravity.CENTER
      if (bold) typeface = Typeface.DEFAULT_BOLD
      layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top) }
    }

    val card = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
      setPadding(dp(24), dp(26), dp(24), dp(20))
      background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(0xF21E2A5A.toInt(), 0xF2141B3A.toInt())).apply {
        cornerRadius = dp(30).toFloat(); setStroke(dp(1), 0x66FFFFFF)
      }
      elevation = dp(16).toFloat()
      addView(iconWrap)
      addView(label("Daily limit reached", 22f, 0xFFF1F5FF.toInt(), true, 16))
      addView(label("You've spent ${money(spent)} of your ${money(limit)} limit today, so $appName is paused for now.", 14f, 0xFFA5B1CC.toInt(), false, 8))
      addView(pill("Back to home", true) { goHome() })
      addView(pill("I have an emergency", false) { openEmergency() })
    }

    val root = FrameLayout(this).apply {
      setBackgroundColor(0xB3070B16.toInt())
      isClickable = true
      isFocusableInTouchMode = true
      setOnKeyListener { _, code, ev -> if (code == KeyEvent.KEYCODE_BACK && ev.action == KeyEvent.ACTION_UP) { goHome(); true } else false }
      addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
        leftMargin = dp(24); rightMargin = dp(24)
      })
      alpha = 0f
    }

    val lp = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      PixelFormat.TRANSLUCENT
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
      lp.blurBehindRadius = 40
    }
    try {
      manager.addView(root, lp)
      root.requestFocus()
      root.animate().alpha(1f).setDuration(180).start()
      overlay = root; overlayPkg = pkg; shownAt = System.currentTimeMillis()
      // If nobody taps, leave the payment app after a few seconds so it cannot stay open behind the reminder.
      autoHome = Runnable { goHome() }.also { handler.postDelayed(it, 12_000) }
    } catch (_: Exception) {
      overlay = null; overlayPkg = null
    }
  }

  private fun removeOverlay() {
    autoHome?.let { handler.removeCallbacks(it) }; autoHome = null
    overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
    overlay = null; overlayPkg = null
  }

  private fun goHome() {
    removeOverlay()
    performGlobalAction(GLOBAL_ACTION_HOME)
  }

  private fun openEmergency() {
    removeOverlay()
    startActivity(Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra("open_emergency_approval", true)
    })
  }

  private fun appName(pkg: String): String = try {
    packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
  } catch (_: Exception) {
    AppRestrictionManager.KNOWN_PAYMENT_PACKAGES.firstOrNull { it.packageName == pkg }?.appName ?: "This app"
  }

  override fun onInterrupt() { removeOverlay() }

  override fun onDestroy() {
    removeOverlay()
    scope.cancel()
    super.onDestroy()
  }
}
