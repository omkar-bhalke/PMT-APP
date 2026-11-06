package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue

enum class SetupMode {
  AUTOMATIC_WHATSAPP,
  MANUAL_LEDGER
}

@Composable
fun OnboardingScreen(
  onComplete: (initialBalance: Double, bankName: String, dailyLimit: Double) -> Unit,
  modifier: Modifier = Modifier
) {
  var setupMode by remember { mutableStateOf(SetupMode.AUTOMATIC_WHATSAPP) }
  var manualBalanceText by remember { mutableStateOf("5000") }
  var dailyLimitText by remember { mutableStateOf("500") }
  var selectedBank by remember { mutableStateOf("State Bank of India (SBI)") }

  val supportedBanks = listOf(
    "State Bank of India (SBI)",
    "HDFC Bank",
    "ICICI Bank",
    "Punjab National Bank (PNB)",
    "Bank of Baroda",
    "Other Indian Bank"
  )

  Scaffold(
    containerColor = NavyDark
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 24.dp, vertical = 16.dp)
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Icon Badge
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(NavySurface),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = "FinPulse",
            tint = AccentGold,
            modifier = Modifier.size(38.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Welcome to FinPulse",
          fontSize = 26.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Choose how you want to manage your expenses. You can change this anytime in Settings.",
          fontSize = 13.sp,
          color = Color(0xFF94A3B8),
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Mode 1: Automated Sync (Recommended)
        val isAuto = setupMode == SetupMode.AUTOMATIC_WHATSAPP
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = if (isAuto) NavySurface else Color(0xFF0F172A)),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = if (isAuto) 2.dp else 1.dp,
              color = if (isAuto) AccentGold else Color(0xFF334155),
              shape = RoundedCornerShape(16.dp)
            )
            .clickable { setupMode = SetupMode.AUTOMATIC_WHATSAPP }
            .testTag("mode_auto_sync_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isAuto) AccentGold.copy(alpha = 0.2f) else Color(0xFF1E293B)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = if (isAuto) AccentGold else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Automatic Banking Sync",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(IncomeGreen.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text("Recommended", color = IncomeGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Automatically fetches live balance and statements from SBI WhatsApp Banking (919022690226). No manual balance calculation needed!",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 17.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode 2: Manual Ledger
        val isManual = setupMode == SetupMode.MANUAL_LEDGER
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = if (isManual) NavySurface else Color(0xFF0F172A)),
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = if (isManual) 2.dp else 1.dp,
              color = if (isManual) AccentGold else Color(0xFF334155),
              shape = RoundedCornerShape(16.dp)
            )
            .clickable { setupMode = SetupMode.MANUAL_LEDGER }
            .testTag("mode_manual_ledger_card")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isManual) AccentGold.copy(alpha = 0.2f) else Color(0xFF1E293B)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = if (isManual) AccentGold else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Manual Ledger Entry",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Track your opening cash balance and manually enter receipts, tea, groceries, and travel expenses.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 17.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dynamic Configuration Form
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ONLY show opening balance if manual ledger mode is chosen!
            AnimatedVisibility(visible = setupMode == SetupMode.MANUAL_LEDGER) {
              Column {
                Text(
                  text = "Opening Account / Cash Balance (₹)",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                  value = manualBalanceText,
                  onValueChange = { manualBalanceText = it },
                  placeholder = { Text("e.g. 5000") },
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  singleLine = true,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_initial_balance_input")
                )
              }
            }

            // Daily Spending Limit (Asked for both, default 500)
            Column {
              Text(
                text = "Daily Spending Limit (₹)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(6.dp))
              OutlinedTextField(
                value = dailyLimitText,
                onValueChange = { dailyLimitText = it },
                placeholder = { Text("500") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("daily_limit_input")
              )
              Text(
                text = "Alerts you when you approach your daily budget.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 4.dp)
              )
            }

            // Bank Selection
            Column {
              Text(
                text = "Primary Bank",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(Color(0xFF0F172A))
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(selectedBank, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Complete Button
      Button(
        onClick = {
          val balance = if (setupMode == SetupMode.AUTOMATIC_WHATSAPP) {
            0.0 // Will be populated automatically from live SBI statement!
          } else {
            manualBalanceText.toDoubleOrNull() ?: 0.0
          }
          val limit = dailyLimitText.toDoubleOrNull() ?: 500.0
          onComplete(balance, selectedBank, limit)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("onboarding_complete_button"),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Start Managing Finances",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}
