package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.example.ui.navigation.FinPulseNavHost
import com.example.ui.theme.FinPulseTheme
import com.example.util.NotificationHelper
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  private val mainViewModel: MainViewModel by viewModels()

  // Set when the user taps a transaction notification; consumed by the nav host.
  private val deepLinkPendingId = mutableStateOf<String?>(null)

  // Android 13+: runtime permission so transaction reminders can be shown.
  private val notificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* user choice respected */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    NotificationHelper.createChannels(this)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.canNotify(this)) {
      notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    readDeepLink(intent)
    setContent {
      val userSettings by mainViewModel.userSettings.collectAsState()
      val systemInDark = isSystemInDarkTheme()
      val isDark = when (userSettings.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
      }

      FinPulseTheme(darkTheme = isDark) {
        FinPulseNavHost(
          mainViewModel = mainViewModel,
          deepLinkPendingId = deepLinkPendingId.value,
          onDeepLinkConsumed = { deepLinkPendingId.value = null }
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    readDeepLink(intent)
  }

  private fun readDeepLink(i: Intent?) {
    i?.getStringExtra(NotificationHelper.EXTRA_PENDING_ID)?.let {
      deepLinkPendingId.value = it
      i.removeExtra(NotificationHelper.EXTRA_PENDING_ID)
    }
  }
}
