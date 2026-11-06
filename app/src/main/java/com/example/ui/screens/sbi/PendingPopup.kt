package com.example.ui.screens.sbi

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.util.NotificationHelper
import com.example.viewmodel.StatementAutomationViewModel

/**
 * In-app popup shown while the app is open when a NEW bank transaction is waiting.
 * It is never forced: "Later" just closes it for now. The transaction stays PENDING (and keeps its
 * notification) until the user answers it. Shown one at a time, oldest first.
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

  AlertDialog(
    onDismissRequest = { laterIds = laterIds + next.id },
    title = { Text(if (next.isCredit) "Money received" else "New transaction detected", fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Text(NotificationHelper.question(next), fontSize = 15.sp)
        Text("Date: ${SbiFmt.date(next.transactionDate)}  •  ${next.description.ifBlank { "Bank entry" }}", fontSize = 12.sp)
        if (items.size > 1) Text("${items.size} transactions are waiting for you.", fontSize = 12.sp)
      }
    },
    confirmButton = {
      TextButton(onClick = { laterIds = laterIds + next.id; onOpenForm(next.id) }) { Text("Add details") }
    },
    dismissButton = {
      Column {
        TextButton(onClick = { laterIds = laterIds + next.id }) { Text("Later") }
        TextButton(onClick = { viewModel.snooze(next, 60); laterIds = laterIds + next.id }) { Text("Snooze 1h") }
      }
    }
  )
}
