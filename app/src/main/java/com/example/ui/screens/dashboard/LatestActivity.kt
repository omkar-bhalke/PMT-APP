package com.example.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PendingTransactionDto
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.ui.components.Glass
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassIconBadge
import com.example.ui.screens.sbi.SbiFmt
import com.example.util.NotificationHelper

/**
 * The first thing on the dashboard: what just happened with your money.
 * Newest bank entries that still need a purpose come first (with a one-tap "Add details"),
 * followed by the latest confirmed transactions.
 */
@Composable
fun LatestActivityPanel(
  pending: List<PendingTransactionDto>,
  recent: List<TransactionEntity>,
  autoMode: Boolean,
  onOpenPending: (String) -> Unit,
  onViewAllPending: () -> Unit,
  onViewAll: () -> Unit,
  onTransactionClick: (TransactionEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  GlassBackground(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
    Column(Modifier.fillMaxWidth().padding(18.dp)) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Latest activity", color = Glass.TextHi, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        TextButton(onClick = onViewAll) { Text("View all", color = Glass.Sky, fontSize = 13.sp) }
      }

      if (pending.isNotEmpty()) {
        Text(
          if (pending.size == 1) "1 new entry needs your input" else "${pending.size} new entries need your input",
          color = Glass.Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        // newest first
        pending.asReversed().take(3).forEach { p ->
          Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpenPending(p.id) },
            verticalAlignment = Alignment.CenterVertically
          ) {
            GlassIconBadge(if (p.isCredit) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, if (p.isCredit) Glass.Mint else Glass.Rose, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
              Text(if (p.isCredit) "Money received" else "Money spent", color = Glass.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
              Text("${SbiFmt.date(p.transactionDate)} • What was this for?", color = Glass.TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
              (if (p.isCredit) "+" else "−") + NotificationHelper.money(p.amount),
              color = if (p.isCredit) Glass.Mint else Glass.Rose, fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
          }
        }
        Spacer(Modifier.height(6.dp))
        GlassButton(
          if (pending.size > 3) "Answer all ${pending.size}" else "Add details", { onOpenPending(pending.first().id) },
          Modifier.fillMaxWidth(), style = com.example.ui.components.GlassButtonStyle.Primary
        )
        if (pending.size > 3) TextButton(onClick = onViewAllPending) { Text("See the list", color = Glass.Sky, fontSize = 12.sp) }
        Spacer(Modifier.height(12.dp))
      }

      if (recent.isNotEmpty()) {
        if (pending.isNotEmpty()) Text("Recent", color = Glass.TextLo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        recent.take(5).forEach { t ->
          val income = t.type == TransactionType.INCOME
          Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(16.dp)).clickable { onTransactionClick(t) }, verticalAlignment = Alignment.CenterVertically) {
            GlassIconBadge(if (income) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, if (income) Glass.Mint else Glass.Rose, 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
              Text(t.description.ifBlank { t.categoryName }, color = Glass.TextHi, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text("${t.categoryName} • ${t.dateString}", color = Glass.TextLo, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text((if (income) "+" else "−") + NotificationHelper.money(t.amount), color = if (income) Glass.Mint else Glass.TextHi, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }
      }

      if (pending.isEmpty() && recent.isEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(
          if (autoMode) "Nothing yet. New credits and debits from your bank statement will appear here the moment they are detected."
          else "No transactions yet. Tap + to add your first one.",
          color = Glass.TextLo, fontSize = 13.sp, lineHeight = 19.sp
        )
      }
    }
  }
}
