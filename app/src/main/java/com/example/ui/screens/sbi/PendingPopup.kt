package com.example.ui.screens.sbi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Glass
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.GlassDialog
import com.example.util.NotificationHelper
import com.example.viewmodel.StatementAutomationViewModel

/**
 * In-app popup for a NEW bank transaction while the app is open. Same glass look as the rest of the app.
 * Never forced: "Later" just closes it. The transaction stays pending (and keeps its notification) until answered.
 * One at a time, oldest first.
 */
@Composable
fun PendingPopupHost(
  viewModel: StatementAutomationViewModel,
  enabled: Boolean,
  onOpenForm: (String) -> Unit
) {
  val items by viewModel.pendingTransactions.collectAsState()
  var laterIds by remember { mutableStateOf(setOf<String>()) }
  val next = items.firstOrNull { it.id !in laterIds && !it.isSnoozed && !viewModel.isSnoozedLocally(it.id) }
  if (!enabled || next == null) return

  GlassDialog(
    onDismiss = { laterIds = laterIds + next.id },
    title = if (next.isCredit) "Money received" else "New transaction",
    icon = if (next.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
    iconTint = if (next.isCredit) Glass.Mint else Glass.Rose,
    message = if (next.isCredit) "${NotificationHelper.money(next.amount)} was credited to your account. What was this money for?"
              else "${NotificationHelper.money(next.amount)} was spent. What was it for?",
    actions = {
      GlassButton("Add details", { laterIds = laterIds + next.id; onOpenForm(next.id) }, Modifier.fillMaxWidth())
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton("Later", { laterIds = laterIds + next.id }, Modifier.weight(1f), style = GlassButtonStyle.Soft)
        GlassButton("Snooze 1h", { viewModel.snooze(next, 60); laterIds = laterIds + next.id }, Modifier.weight(1f), style = GlassButtonStyle.Soft)
      }
    },
    body = {
      Column(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${SbiFmt.date(next.transactionDate)}  •  ${next.description.ifBlank { "Bank entry" }}", color = Glass.TextLo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (items.size > 1) Text("${items.size} entries are waiting", color = Glass.Amber, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
      }
    }
  )
}
