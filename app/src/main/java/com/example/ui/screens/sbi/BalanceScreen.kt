package com.example.ui.screens.sbi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StatementAutomationViewModel

/** Current bank balance (as reported by SBI) and the history of every balance change. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceScreen(viewModel: StatementAutomationViewModel, onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
  val balance by viewModel.realBalance.collectAsState()
  val history by viewModel.history.collectAsState()
  val error by viewModel.error.collectAsState()
  LaunchedEffect(Unit) { viewModel.refreshBalanceAndHistory() }

  Scaffold(topBar = {
    TopAppBar(
      title = { Text("SBI Balance", fontWeight = FontWeight.Bold) },
      navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
      actions = { IconButton(onClick = { viewModel.refreshBalanceAndHistory() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") } }
    )
  }) { inner ->
    LazyColumn(modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      item {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
          Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Available balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (balance?.hasData == true) {
              Text(SbiFmt.rupees(balance?.currentBalance), fontSize = 32.sp, fontWeight = FontWeight.Bold)
              Text("A/c ${balance?.accountNumber ?: "—"} • ${balance?.bankName ?: "SBI"}", fontSize = 12.sp)
              balance?.balanceDifference?.let {
                Text("Change since previous statement: ${SbiFmt.signedRupees(it)}", fontSize = 13.sp, color = if (it < 0) Color(0xFFDC2626) else Color(0xFF059669))
              }
              Text("Statement time: ${SbiFmt.time(balance?.lastStatementAt)}", fontSize = 11.sp)
            } else {
              Text("No statement received yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
              Text("Link WhatsApp and start the automation. The balance shown here always comes from your real SBI statement.", fontSize = 12.sp)
            }
          }
        }
      }
      error?.let { item { Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp) } }
      item { Text("History", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }
      if (history.isEmpty()) item { Text("Nothing yet.", fontSize = 12.sp) }
      items(history, key = { it.id }) { h ->
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
          Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
              Text(SbiFmt.rupees(h.balance), fontWeight = FontWeight.SemiBold)
              Text(SbiFmt.time(h.statementAt ?: h.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              if (h.isBaseline) Text("Starting balance", fontSize = 11.sp)
              else Text("${h.newEntries} new transaction(s)", fontSize = 11.sp)
              h.discrepancy?.let { Text("Heads-up: balance moved ${SbiFmt.rupees(it)} more/less than the listed entries explain (an entry may have scrolled out of the statement).", fontSize = 10.sp, color = Color(0xFFB45309)) }
            }
            h.balanceDifference?.let {
              Text(SbiFmt.signedRupees(it), fontWeight = FontWeight.Bold, color = if (it < 0) Color(0xFFDC2626) else Color(0xFF059669))
            }
          }
        }
      }
    }
  }
}
