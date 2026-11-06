package com.example.ui.screens.sbi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PendingTransactionDto
import com.example.util.NotificationHelper
import com.example.viewmodel.StatementAutomationViewModel

/**
 * The form opened from the notification (or the list). It always shows the ORIGINAL amount, date and type.
 * Nothing is pre-selected: an AI/keyword suggestion is shown as a chip the user may tap, never auto-saved.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PendingTransactionFormScreen(
  pendingId: String,
  viewModel: StatementAutomationViewModel,
  onNavigateBack: () -> Unit,
  onNext: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var item by remember(pendingId) { mutableStateOf<PendingTransactionDto?>(null) }
  var loaded by remember(pendingId) { mutableStateOf(false) }
  val categories by viewModel.categories.collectAsState()
  val busy by viewModel.busy.collectAsState()

  var category by remember(pendingId) { mutableStateOf("") }
  var purpose by remember(pendingId) { mutableStateOf("") }
  var showNewCategory by remember { mutableStateOf(false) }
  var newCategory by remember { mutableStateOf("") }
  var extraCategories by remember { mutableStateOf(listOf<String>()) }
  var savedOfflineNote by remember { mutableStateOf(false) }

  LaunchedEffect(pendingId) {
    viewModel.loadCategories()
    item = viewModel.loadPendingItem(pendingId)
    loaded = true
  }

  Scaffold(topBar = {
    TopAppBar(
      title = { Text("What was this for?", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
      navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
    )
  }) { inner ->
    val p = item
    if (p == null) {
      Column(Modifier.fillMaxSize().padding(inner).padding(24.dp)) {
        Text(if (loaded) "This transaction was already answered or is no longer available." else "Loading…")
        if (loaded) OutlinedButton(onClick = onNavigateBack, modifier = Modifier.padding(top = 12.dp)) { Text("Back") }
      }
      return@Scaffold
    }

    val wanted = if (p.isCredit) "INCOME" else "EXPENSE"
    val names = (categories.filter { it.type == wanted }.map { it.name } + extraCategories).distinct()

    Column(
      modifier.fillMaxSize().padding(inner).padding(16.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (p.isCredit) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))) {
        Column(Modifier.padding(18.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(NotificationHelper.question(p), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFF111827))
          Text("Amount: ${NotificationHelper.money(p.amount)}   •   ${if (p.isCredit) "Credit" else "Debit"}", fontSize = 13.sp, color = Color(0xFF111827))
          Text("Date: ${SbiFmt.date(p.transactionDate)}   •   Bank note: ${p.description.ifBlank { "—" }}", fontSize = 12.sp, color = Color(0xFF374151))
          p.accountNumber?.let { Text("Account $it", fontSize = 11.sp, color = Color(0xFF374151)) }
        }
      }

      Text("Category", fontWeight = FontWeight.SemiBold)
      p.suggestedCategory?.takeIf { it.isNotBlank() }?.let { s ->
        AssistChip(onClick = { category = s }, label = { Text("Suggested: $s (tap to use)") })
      }
      androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEach { n -> FilterChip(selected = category == n, onClick = { category = n }, label = { Text(n) }) }
        AssistChip(onClick = { showNewCategory = true }, label = { Text("+ New category") })
      }

      OutlinedTextField(
        value = purpose, onValueChange = { purpose = it.take(200) },
        label = { Text(if (p.isCredit) "What was this money for?" else "What did you spend it on?") },
        placeholder = { Text("e.g. Lunch with friends") },
        modifier = Modifier.fillMaxWidth(), minLines = 2
      )

      if (savedOfflineNote) Text("Saved on your phone. It will be sent as soon as you're online.", fontSize = 12.sp, color = Color(0xFFB45309))

      Button(
        onClick = {
          viewModel.submitAnswer(p, category, purpose) { offline, next ->
            savedOfflineNote = offline
            if (next != null) onNext(next) else onNavigateBack()
          }
        },
        enabled = !busy && category.isNotBlank() && purpose.trim().length >= 2,
        modifier = Modifier.fillMaxWidth()
      ) { Text("Save") }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { viewModel.snooze(p, 60); onNavigateBack() }) { Text("Snooze 1 hour") }
        OutlinedButton(onClick = onNavigateBack) { Text("Later") }
      }
      Text("Until you save, this transaction stays pending — closing this screen never deletes it.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showNewCategory) {
      AlertDialog(
        onDismissRequest = { showNewCategory = false },
        title = { Text("New category") },
        text = { OutlinedTextField(value = newCategory, onValueChange = { newCategory = it.take(40) }, singleLine = true, label = { Text("Name") }) },
        confirmButton = {
          TextButton(onClick = {
            val n = newCategory.trim()
            if (n.isNotEmpty()) { extraCategories = extraCategories + n; category = n }
            newCategory = ""; showNewCategory = false
          }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = { showNewCategory = false }) { Text("Cancel") } }
      )
    }
  }
}
