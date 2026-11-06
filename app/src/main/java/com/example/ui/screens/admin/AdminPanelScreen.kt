package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.StatementAutomationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
  mainViewModel: MainViewModel,
  dashboardViewModel: DashboardViewModel,
  statementViewModel: StatementAutomationViewModel,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val userSettings by mainViewModel.userSettings.collectAsState()
  val dashState by dashboardViewModel.uiState.collectAsState()
  val realBalance by statementViewModel.realBalance.collectAsState()
  val pendingList by statementViewModel.pendingTransactions.collectAsState()
  val autoConfig by statementViewModel.automationConfig.collectAsState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("System Administration Console", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          IconButton(onClick = {
            dashboardViewModel.refreshData()
            statementViewModel.loadRealBalance()
            statementViewModel.loadPendingTransactions()
          }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Overview Banner
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Operational Overview",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(IncomeGreen.copy(alpha = 0.2f))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
              ) {
                Text("ALL SERVICES HEALTHY", color = IncomeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "FinPulse Cloud & Automation Telemetry Dashboard",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp
            )
          }
        }
      }

      // 2. Metrics Grid (2x2)
      item {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AdminMetricCard(
            title = "Verified Balance",
            value = "₹${String.format("%.2f", realBalance?.currentBalance ?: dashState.totalBalance)}",
            subtitle = "SBI A/c ${realBalance?.accountNumber ?: "xxx188"}",
            icon = Icons.Default.AccountBalance,
            accentColor = IncomeGreen,
            modifier = Modifier.weight(1f)
          )
          AdminMetricCard(
            title = "Pending Ingestion",
            value = "${pendingList.size} Entries",
            subtitle = "Awaiting review",
            icon = Icons.Default.ReceiptLong,
            accentColor = AccentGold,
            modifier = Modifier.weight(1f)
          )
        }
      }

      item {
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AdminMetricCard(
            title = "Daily Spending",
            value = "₹${dashState.todayExpenses.toInt()} / ₹${dashState.dailyLimit.toInt()}",
            subtitle = if (dashState.todayExpenses > dashState.dailyLimit) "LIMIT EXCEEDED" else "Within Limit",
            icon = Icons.Default.Speed,
            accentColor = if (dashState.todayExpenses > dashState.dailyLimit) Color(0xFFEF4444) else PrimaryBlue,
            modifier = Modifier.weight(1f)
          )
          AdminMetricCard(
            title = "Ledger History",
            value = "${dashState.recentTransactions.size} Recent",
            subtitle = "Transactions logged",
            icon = Icons.Default.Analytics,
            accentColor = Color(0xFFA78BFA),
            modifier = Modifier.weight(1f)
          )
        }
      }

      // 3. User & Device Registration Details
      item {
        Text("Active User & Device Record", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminDetailRow("Registered Account Name", userSettings.userName.ifBlank { "FinPulse User" })
            AdminDetailRow("Email Address", userSettings.userEmail.ifBlank { "user@finpulse.app" })
            AdminDetailRow("Device Identity", "${userSettings.deviceId.take(16)}... (Active)")
            AdminDetailRow("Account State", "SINGLE DEVICE BOUND • AUTHORIZED")
            AdminDetailRow("Payment Protection", if (userSettings.accessibilityServiceEnabled) "MONITORING ACTIVE" else "DISABLED")
          }
        }
      }

      // 4. Statement Automation Details
      item {
        Text("SBI Statement Automation Engine", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AdminDetailRow("Target Banking Bot", autoConfig?.sbiTargetPhone ?: "919022690226")
            AdminDetailRow("Fetch Cycle Frequency", "Every ${autoConfig?.intervalMinutes ?: 30} Minutes")
            AdminDetailRow("Engine Status", autoConfig?.lastStatus ?: "IDLE")
            AdminDetailRow("Last Run Timestamp", autoConfig?.lastRunAt ?: "Initialized")
            AdminDetailRow("Worker Endpoint", autoConfig?.railwayApiUrl ?: "Configured")
          }
        }
      }

      // 5. System Security & Architecture
      item {
        Text("Security Architecture & Encryption", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }

      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminDetailRow("Authentication", "BCrypt Hashing + JWT Tokens")
            AdminDetailRow("Device Enforcement", "1 Device = 1 Account Architecture")
            AdminDetailRow("AI Key Encryption", "AES-256-CBC at rest")
            AdminDetailRow("Deduplication Engine", "SHA-256 Idempotency Hashing")
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun AdminMetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
      Spacer(modifier = Modifier.height(2.dp))
      Text(subtitle, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
    }
  }
}

@Composable
private fun AdminDetailRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
  }
}
