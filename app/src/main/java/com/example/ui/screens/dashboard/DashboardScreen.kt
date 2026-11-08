package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.components.CurrentBalanceCard
import com.example.ui.components.MonthlyOverviewCard
import com.example.ui.components.SpendingLimitCard
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
  viewModel: DashboardViewModel,
  statementViewModel: com.example.viewmodel.StatementAutomationViewModel? = null,
  onAddTransactionClick: () -> Unit,
  onViewAllTransactionsClick: () -> Unit,
  onPendingTransactionsClick: () -> Unit = {},
  onSbiBalanceClick: () -> Unit = {},
  onOpenPending: (String) -> Unit = {},
  onLinkWhatsAppClick: () -> Unit = {},
  trackingMode: String = "",
  onTransactionClick: (TransactionEntity) -> Unit,
  onAiAssistantClick: () -> Unit,
  onConfigureLimitsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val pendingList by statementViewModel?.pendingTransactions?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
  val realBalance by statementViewModel?.realBalance?.collectAsState() ?: remember { mutableStateOf(null) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background,
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddTransactionClick,
        containerColor = PrimaryBlue,
        contentColor = Color.White,
        shape = CircleShape,
        modifier = Modifier.testTag("add_transaction_fab")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header with App Title and Sync/Refresh button
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "FinPulse",
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = "Your money at a glance",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = { viewModel.refreshData() },
            modifier = Modifier.testTag("refresh_dashboard_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Sync",
              tint = MaterialTheme.colorScheme.primary
            )
          }
        }
      }

      // Latest activity FIRST: new bank entries waiting for you, then the most recent transactions
      item {
        com.example.ui.screens.dashboard.LatestActivityPanel(
          pending = pendingList,
          recent = uiState.recentTransactions,
          autoMode = trackingMode == "AUTO",
          onOpenPending = onOpenPending,
          onViewAllPending = onPendingTransactionsClick,
          onViewAll = onViewAllTransactionsClick,
          onTransactionClick = onTransactionClick
        )
      }

      // Automatic tracking is chosen but no statement has arrived yet: guide the user
      if (trackingMode == "AUTO" && realBalance?.hasData != true) {
        item {
          com.example.ui.components.GlassBackground(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
              Text("Start automatic tracking", color = com.example.ui.components.Glass.TextHi, fontWeight = FontWeight.Bold, fontSize = 16.sp)
              Spacer(Modifier.height(4.dp))
              Text("Link your WhatsApp once and your SBI balance and new transactions will show up here by themselves.", color = com.example.ui.components.Glass.TextLo, fontSize = 13.sp)
              Spacer(Modifier.height(12.dp))
              com.example.ui.components.GlassButton("Link WhatsApp", onLinkWhatsAppClick, Modifier.fillMaxWidth())
            }
          }
        }
      }

      // 1. Current Available Balance Card
      item {
        CurrentBalanceCard(
          totalBalance = realBalance?.takeIf { it.hasData }?.currentBalance ?: uiState.totalBalance,
          initialBalance = uiState.initialBalance,
          totalIncome = uiState.totalIncome,
          totalExpenses = uiState.totalExpenses,
          bankName = uiState.selectedBank
        )
      }

      // 2. Spending Limit System Card
      item {
        SpendingLimitCard(
          dailyLimit = uiState.dailyLimit,
          spentToday = uiState.todayExpenses,
          onConfigureClick = onConfigureLimitsClick
        )
      }

      // 3. Monthly Overview Card
      item {
        MonthlyOverviewCard(
          monthlyIncome = uiState.monthlyIncome,
          monthlyExpense = uiState.monthlyExpenses
        )
      }

      // 4. AI Insights Mini Banner
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI",
                tint = AccentGold,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.size(12.dp))
              Column {
                Text(
                  text = "AI Financial Advisor",
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Detect repeated micro-purchases and potential savings",
                  color = Color(0xFF94A3B8),
                  fontSize = 11.sp
                )
              }
            }

            Button(
              onClick = onAiAssistantClick,
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text("Ask AI", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(60.dp)) // space for FAB
      }
    }
  }
}
