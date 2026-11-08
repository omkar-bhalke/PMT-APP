package com.example.ui.screens.sbi

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.NotificationHelper
import com.example.viewmodel.StatementAutomationViewModel

/** All unanswered bank transactions, oldest first. Nothing is ever skipped or removed until answered. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
  viewModel: StatementAutomationViewModel,
  onNavigateBack: () -> Unit,
  onOpenForm: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val items by viewModel.pendingTransactions.collectAsState()
  LaunchedEffect(Unit) { viewModel.syncPending() }

  Scaffold(topBar = {
    TopAppBar(
      title = { Text("Transactions to review (${items.size})", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
      actions = { IconButton(onClick = { viewModel.syncPending() }) { Icon(Icons.Default.Refresh, contentDescription = "Sync") } }
    )
  }) { inner ->
    if (items.isEmpty()) {
      Column(Modifier.fillMaxSize().padding(inner).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("All caught up", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("New debits and credits from your SBI statement will appear here and as a notification.", fontSize = 13.sp)
      }
    } else {
      LazyColumn(modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
          Button(onClick = { onOpenForm(items.first().id) }, modifier = Modifier.fillMaxWidth()) { Text("Answer one by one") }
        }
        items(items, key = { it.id }) { p ->
          Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { onOpenForm(p.id) }) {
            Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text(if (p.isCredit) "Money received" else "Money spent", fontWeight = FontWeight.SemiBold)
                Text("${SbiFmt.date(p.transactionDate)} • ${p.description}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (p.isSnoozed) Text("Reminder snoozed (still pending)", fontSize = 11.sp, color = Color(0xFFB45309))
              }
              Text(
                (if (p.isCredit) "+" else "−") + NotificationHelper.money(p.amount),
                fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (p.isCredit) Color(0xFF059669) else Color(0xFFDC2626)
              )
            }
          }
        }
      }
    }
  }
}
