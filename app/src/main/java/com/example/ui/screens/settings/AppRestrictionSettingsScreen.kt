package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.Glass
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconBadge
import com.example.ui.screens.restriction.AppRestrictionManager
import java.text.NumberFormat
import java.util.Locale

private fun rupees(v: Double) = "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(v)

/** Choose which payment apps are paused after today's limit is reached. */
@Composable
fun AppRestrictionSettingsScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var featureOn by remember { mutableStateOf(AppRestrictionManager.isFeatureEnabled(context)) }
  var granted by remember { mutableStateOf(AppRestrictionManager.isAccessibilityServiceEnabled(context)) }
  var today by remember { mutableStateOf<Triple<Boolean, Double, Double>?>(null) }
  val appOn = remember {
    mutableStateMapOf<String, Boolean>().apply {
      AppRestrictionManager.KNOWN_PAYMENT_PACKAGES.forEach { put(it.packageName, AppRestrictionManager.isPackageRestricted(context, it.packageName)) }
    }
  }

  DisposableEffect(lifecycleOwner) {
    val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) granted = AppRestrictionManager.isAccessibilityServiceEnabled(context) }
    lifecycleOwner.lifecycle.addObserver(obs)
    onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
  }
  LaunchedEffect(Unit) { today = AppRestrictionManager.checkSpendingLimitExceeded(context) }

  val active = featureOn && granted

  GlassBackground(modifier) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Glass.TextHi) }
        Text("Spending Guardian", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
      }
      Spacer(Modifier.height(8.dp))

      GlassCard(Modifier.fillMaxWidth(), tint = if (active) Glass.Mint else Glass.Amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          GlassIconBadge(Icons.Default.Shield, if (active) Glass.Mint else Glass.Amber, 52.dp)
          Spacer(Modifier.width(14.dp))
          Column(Modifier.weight(1f)) {
            Text(if (active) "Guardian is on" else if (!granted) "One permission needed" else "Guardian is paused", fontWeight = FontWeight.Bold, color = Glass.TextHi, fontSize = 17.sp)
            val t = today
            Text(
              if (t != null && t.third > 0) "Today: ${rupees(t.second)} of ${rupees(t.third)}" else "Set a daily limit in Settings",
              color = Glass.TextLo, fontSize = 13.sp
            )
          }
          Switch(
            checked = featureOn, onCheckedChange = { featureOn = it; AppRestrictionManager.setFeatureEnabled(context, it) },
            colors = SwitchDefaults.colors(checkedTrackColor = Glass.Mint, checkedThumbColor = Color.White)
          )
        }
      }

      if (!granted) {
        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
          Text("Turn it on in 3 steps", fontWeight = FontWeight.Bold, color = Glass.TextHi, fontSize = 16.sp)
          Spacer(Modifier.height(8.dp))
          listOf(
            "Tap “Open accessibility settings”.",
            "Open “Installed apps” (or “Downloaded apps”) and choose FinPulse.",
            "Switch it ON and press Allow."
          ).forEachIndexed { i, s -> Text("${i + 1}. $s", color = Glass.TextLo, fontSize = 13.sp, lineHeight = 19.sp) }
          Spacer(Modifier.height(12.dp))
          GlassButton("Open accessibility settings", { AppRestrictionManager.openAccessibilitySettings(context) }, Modifier.fillMaxWidth())
          Spacer(Modifier.height(14.dp))
          Text("The switch is greyed out?", fontWeight = FontWeight.SemiBold, color = Glass.Amber, fontSize = 13.sp)
          Text(
            "Android 13+ locks this for apps installed from a file. Open App info, tap the ⋮ menu (top right) and choose “Allow restricted settings”, then go back and repeat the steps.",
            color = Glass.TextLo, fontSize = 12.sp, lineHeight = 18.sp
          )
          Spacer(Modifier.height(10.dp))
          GlassButton("Open App info", { AppRestrictionManager.openAppInfo(context) }, Modifier.fillMaxWidth(), style = GlassButtonStyle.Soft)
        }
      }

      Spacer(Modifier.height(18.dp))
      Text("Payment apps to pause", fontWeight = FontWeight.Bold, color = Glass.TextHi, fontSize = 16.sp)
      Text("Only the apps switched on here are paused, and only after today's limit is reached. Nothing else on your phone is affected.", color = Glass.TextLo, fontSize = 12.sp, lineHeight = 17.sp)
      Spacer(Modifier.height(10.dp))

      val items = AppRestrictionManager.KNOWN_PAYMENT_PACKAGES.sortedByDescending { AppRestrictionManager.isInstalled(context, it.packageName) }
      items.forEach { item ->
        val installed = AppRestrictionManager.isInstalled(context, item.packageName)
        GlassCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), corner = 18.dp, contentPadding = 14.dp) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
              Text(item.appName, color = if (installed) Glass.TextHi else Glass.TextLo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
              Text(if (installed) "Installed" else "Not installed on this phone", color = Glass.TextLo, fontSize = 11.sp)
            }
            Switch(
              checked = appOn[item.packageName] == true,
              onCheckedChange = { appOn[item.packageName] = it; AppRestrictionManager.setPackageRestricted(context, item.packageName, it) },
              colors = SwitchDefaults.colors(checkedTrackColor = Glass.Indigo, checkedThumbColor = Color.White)
            )
          }
        }
      }

      Spacer(Modifier.height(16.dp))
      GlassCard(Modifier.fillMaxWidth()) {
        Text("How it works", fontWeight = FontWeight.Bold, color = Glass.TextHi, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
          "When you open one of these apps after reaching your daily limit, a small reminder appears over that app only and takes you back to the home screen. " +
            "It never reads your screen, messages or passwords. If it is really urgent, tap “I have an emergency”.",
          color = Glass.TextLo, fontSize = 12.sp, lineHeight = 18.sp
        )
      }
      Spacer(Modifier.height(30.dp))
    }
  }
}
