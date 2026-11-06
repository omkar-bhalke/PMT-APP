package com.example.ui.screens.sbi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StatementAutomationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AutomationSettingsScreen(
  viewModel: StatementAutomationViewModel,
  onNavigateBack: () -> Unit,
  onOpenWhatsAppLink: () -> Unit,
  onOpenBalance: () -> Unit,
  modifier: Modifier = Modifier
) {
  val auto by viewModel.automation.collectAsState()
  val wa by viewModel.whatsapp.collectAsState()
  val balance by viewModel.realBalance.collectAsState()
  val logs by viewModel.logs.collectAsState()
  val busy by viewModel.busy.collectAsState()
  val error by viewModel.error.collectAsState()
  val info by viewModel.info.collectAsState()

  var customValue by remember { mutableStateOf("") }
  var customHours by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    while (true) { viewModel.refreshOverview(); viewModel.refreshWhatsapp(); viewModel.refreshLogs(); delay(10_000) }
  }

  val presets = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hour", 120 to "2 hours", 360 to "6 hours")
  val minInterval = auto?.minIntervalMinutes ?: 5

  Scaffold(topBar = {
    TopAppBar(
      title = { Text("Statement Automation", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
      actions = { IconButton(onClick = { viewModel.refreshOverview(); viewModel.refreshLogs() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") } }
    )
  }) { inner ->
    Column(
      modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      error?.let { Banner(it, Color(0xFFFEE2E2), Color(0xFF7F1D1D)) { viewModel.clearError() } }
      info?.let { Banner(it, Color(0xFFDCFCE7), Color(0xFF14532D)) { viewModel.clearInfo() } }

      // ---- connection + enable
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          val (label, color) = when (wa.state) {
            "CONNECTED" -> "WhatsApp: Connected" to Color(0xFF10B981)
            "RECONNECTING" -> "WhatsApp: Reconnecting" to Color(0xFFF59E0B)
            else -> "WhatsApp: Disconnected" to Color(0xFFEF4444)
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, color = color, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenWhatsAppLink) { Text(if (wa.state == "CONNECTED") "Manage" else "Link now") }
          }
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
              Text("Automatic mini statement", fontWeight = FontWeight.Bold)
              Text("The server sends “Hi” to SBI, picks “Get Mini Statement” and reads the reply. Works even when this app is closed.", fontSize = 12.sp)
            }
            Switch(
              checked = auto?.isEnabled == true,
              onCheckedChange = { viewModel.setEnabled(it, auto?.intervalMinutes) },
              enabled = !busy && (wa.state == "CONNECTED" || wa.state == "RECONNECTING" || auto?.isEnabled == true)
            )
          }
          if (wa.state != "CONNECTED" && wa.state != "RECONNECTING") Text("Link WhatsApp first to enable automation.", fontSize = 12.sp, color = Color(0xFFB45309))
        }
      }

      // ---- interval
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Repeat every", fontWeight = FontWeight.Bold)
          Text("Current: ${StatementAutomationViewModel.formatInterval(auto?.intervalMinutes ?: 30)}", fontSize = 13.sp)
          androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            presets.forEach { (min, label) ->
              FilterChip(selected = auto?.intervalMinutes == min, onClick = { viewModel.setInterval(min) }, label = { Text(label) }, enabled = !busy)
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
              value = customValue, onValueChange = { customValue = it.filter(Char::isDigit).take(4) },
              label = { Text("Custom") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            FilterChip(selected = !customHours, onClick = { customHours = false }, label = { Text("min") })
            FilterChip(selected = customHours, onClick = { customHours = true }, label = { Text("hours") })
          }
          val customMinutes = (customValue.toIntOrNull() ?: 0) * if (customHours) 60 else 1
          val valid = customMinutes in minInterval..(auto?.maxIntervalMinutes ?: 1440)
          Button(onClick = { viewModel.setInterval(customMinutes); customValue = "" }, enabled = valid && !busy) { Text("Save custom interval") }
          Text("Minimum $minInterval minutes (to protect your WhatsApp account). 30 minutes or more is recommended.", fontSize = 11.sp)
        }
      }

      // ---- status
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Status", fontWeight = FontWeight.Bold)
          InfoRow("State", SbiFmt.statusLabel(auto?.lastStatus ?: "IDLE"))
          InfoRow("Last successful fetch", SbiFmt.time(auto?.lastSuccessAt))
          InfoRow("Next scheduled run", if (auto?.isEnabled == true) SbiFmt.time(auto?.nextRunAt) else "—")
          InfoRow("Last balance", SbiFmt.rupees(balance?.currentBalance))
          if (balance?.balanceDifference != null) InfoRow("Change since previous", SbiFmt.signedRupees(balance?.balanceDifference))
          InfoRow("Account", balance?.accountNumber ?: "—")
          auto?.lastError?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp) }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.runNow() }, enabled = !busy && wa.state == "CONNECTED") { Text("Fetch now") }
            OutlinedButton(onClick = onOpenBalance) { Text("Balance history") }
          }
        }
      }

      // ---- behaviour toggle
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text("Import entries of the first statement", fontWeight = FontWeight.SemiBold)
            Text("Off (recommended): the first statement only sets the starting balance, so old transactions don't flood you with questions.", fontSize = 11.sp)
          }
          Switch(checked = auto?.importInitialEntries == true, onCheckedChange = { viewModel.setImportInitial(it) })
        }
      }

      // ---- history
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Execution history", fontWeight = FontWeight.Bold)
          if (logs.isEmpty()) Text("No runs yet.", fontSize = 12.sp)
          logs.take(30).forEach { l ->
            Column {
              val bad = l.eventType.contains("ERROR") || l.eventType.contains("WARNING") || l.eventType.contains("NO_RESPONSE") || l.eventType.contains("PAUSED")
              Text(l.message, fontSize = 12.sp, color = if (bad) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface)
              Text("${SbiFmt.time(l.createdAt)} • ${l.eventType.removePrefix("CYCLE_").lowercase().replace('_', ' ')}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
      Spacer(Modifier.size(12.dp))
    }
  }
}

@Composable
private fun InfoRow(k: String, v: String) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(k, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(v, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}

@Composable
internal fun Banner(text: String, bg: Color, fg: Color, onClose: () -> Unit) {
  Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(12.dp)) {
    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Text(text, fontSize = 12.sp, color = fg, modifier = Modifier.weight(1f))
      TextButton(onClick = onClose) { Text("OK") }
    }
  }
}
