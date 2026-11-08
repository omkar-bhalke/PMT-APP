package com.example.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Glass
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconBadge
import com.example.ui.components.GlassMessage
import com.example.ui.components.GlassTextField
import com.example.viewmodel.MainViewModel

private val BANKS = listOf(
  "State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Axis Bank", "Punjab National Bank",
  "Bank of Baroda", "Kotak Mahindra Bank", "Canara Bank", "Union Bank of India", "Other"
)

/**
 * One-time setup, shown ONLY for a brand-new account (the choice is saved on the account, so signing in again
 * on any phone never shows it). Automatic tracking never asks for a balance: it comes from the bank statement.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onFinished: (mode: String) -> Unit, modifier: Modifier = Modifier) {
  var step by remember { mutableStateOf(0) }            // 0 mode, 1 manual balance, 2 daily limit
  var mode by remember { mutableStateOf("") }            // AUTO | MANUAL
  var bank by remember { mutableStateOf(BANKS[0]) }
  var balanceText by remember { mutableStateOf("") }
  var limitText by remember { mutableStateOf("500") }
  var saving by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }

  BackHandler(enabled = step > 0) { step = if (step == 2 && mode == "AUTO") 0 else step - 1 }

  fun finish(skipLimit: Boolean) {
    val limit = if (skipLimit) null else limitText.toDoubleOrNull()?.takeIf { it > 0 }
    val balance = if (mode == "MANUAL") balanceText.toDoubleOrNull() else null
    saving = true; error = null
    viewModel.completeSetup(
      mode = mode, initialBalance = balance, bankName = if (mode == "AUTO") "State Bank of India (SBI)" else bank, dailyLimit = limit,
      onDone = { saving = false; onFinished(mode) },
      onError = { saving = false; error = it }
    )
  }

  GlassBackground(modifier) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
      // progress dots
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        val total = if (mode == "MANUAL") 3 else 2
        val shown = if (step == 2 && mode == "AUTO") 1 else step
        for (i in 0 until total) {
          Spacer(Modifier.width(4.dp))
          Row(Modifier.width(if (i == shown) 26.dp else 8.dp).height(8.dp)) {
            GlassCard(Modifier.fillMaxSize(), corner = 4.dp, tint = if (i <= shown) Glass.Sky else Color.White, contentPadding = 0.dp) {}
          }
        }
      }
      Spacer(Modifier.height(28.dp))

      when (step) {
        0 -> {
          GlassIconBadge(Icons.Default.AutoAwesome, Glass.Violet, 64.dp)
          Spacer(Modifier.height(14.dp))
          Text("How should we track your balance?", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi, lineHeight = 32.sp)
          Spacer(Modifier.height(6.dp))
          Text("You can change this any time in Settings.", fontSize = 14.sp, color = Glass.TextLo)
          Spacer(Modifier.height(22.dp))
          ModeCard(Icons.Default.Speed, "Automatic", "Your SBI balance and new transactions are picked up from your bank statement. No balance to type.", mode == "AUTO", "Recommended") { mode = "AUTO" }
          Spacer(Modifier.height(12.dp))
          ModeCard(Icons.Default.Edit, "Manual", "You enter your current balance and add every transaction yourself.", mode == "MANUAL", null) { mode = "MANUAL" }
          Spacer(Modifier.height(24.dp))
          GlassButton("Continue", { step = if (mode == "MANUAL") 1 else 2 }, Modifier.fillMaxWidth(), enabled = mode.isNotEmpty())
        }
        1 -> {
          GlassIconBadge(Icons.Default.AccountBalance, Glass.Sky, 64.dp)
          Spacer(Modifier.height(14.dp))
          Text("Your starting balance", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
          Spacer(Modifier.height(6.dp))
          Text("Pick your bank and type what you have right now.", fontSize = 14.sp, color = Glass.TextLo)
          Spacer(Modifier.height(18.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BANKS.forEach { b ->
              FilterChip(
                selected = bank == b, onClick = { bank = b }, label = { Text(b, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                  labelColor = Glass.TextHi, selectedLabelColor = Color.White,
                  containerColor = Color.White.copy(alpha = 0.08f), selectedContainerColor = Glass.Indigo.copy(alpha = 0.7f)
                )
              )
            }
          }
          Spacer(Modifier.height(18.dp))
          GlassTextField(balanceText, { balanceText = it.filter { c -> c.isDigit() || c == '.' }.take(12) }, "Current balance (₹)", keyboardType = KeyboardType.Decimal)
          Spacer(Modifier.height(24.dp))
          GlassButton("Continue", { step = 2 }, Modifier.fillMaxWidth(), enabled = (balanceText.toDoubleOrNull() ?: -1.0) >= 0.0)
        }
        else -> {
          GlassIconBadge(Icons.Default.CheckCircle, Glass.Mint, 64.dp)
          Spacer(Modifier.height(14.dp))
          Text("Daily spending limit", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
          Spacer(Modifier.height(6.dp))
          Text("FinPulse can stop payment apps once you cross this in a day. You can skip this and set it later.", fontSize = 14.sp, color = Glass.TextLo)
          Spacer(Modifier.height(18.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("300", "500", "1000", "2000").forEach { v ->
              FilterChip(
                selected = limitText == v, onClick = { limitText = v }, label = { Text("₹$v") },
                colors = FilterChipDefaults.filterChipColors(
                  labelColor = Glass.TextHi, selectedLabelColor = Color.White,
                  containerColor = Color.White.copy(alpha = 0.08f), selectedContainerColor = Glass.Indigo.copy(alpha = 0.7f)
                )
              )
            }
          }
          Spacer(Modifier.height(12.dp))
          GlassTextField(limitText, { limitText = it.filter { c -> c.isDigit() }.take(7) }, "Custom limit (₹)", keyboardType = KeyboardType.Number)
          error?.let { Spacer(Modifier.height(12.dp)); GlassMessage(it) }
          Spacer(Modifier.height(24.dp))
          GlassButton("Finish", { finish(false) }, Modifier.fillMaxWidth(), loading = saving, enabled = (limitText.toDoubleOrNull() ?: 0.0) > 0)
          Spacer(Modifier.height(8.dp))
          GlassButton("Skip for now", { finish(true) }, Modifier.fillMaxWidth(), enabled = !saving, style = GlassButtonStyle.Soft)
        }
      }
    }
  }
}

@Composable
private fun ModeCard(icon: ImageVector, title: String, text: String, selected: Boolean, badge: String?, onClick: () -> Unit) {
  GlassCard(Modifier.fillMaxWidth(), tint = if (selected) Glass.Indigo else null, onClick = onClick) {
    Row(verticalAlignment = Alignment.Top) {
      GlassIconBadge(icon, if (selected) Glass.Sky else Glass.TextLo, 46.dp)
      Spacer(Modifier.width(14.dp))
      Column(Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
          if (badge != null) { Spacer(Modifier.width(8.dp)); Text(badge, fontSize = 11.sp, color = Glass.Mint, fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(4.dp))
        Text(text, fontSize = 13.sp, color = Glass.TextLo, lineHeight = 18.sp)
      }
      if (selected) Icon(Icons.Default.CheckCircle, null, tint = Glass.Sky, modifier = Modifier.size(22.dp))
    }
  }
}
