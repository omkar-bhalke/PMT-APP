package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VpnKey
import com.example.ui.components.GlassAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionType
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.ui.theme.PrimaryBlue
import com.example.viewmodel.SettingsViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  onNavigateToWhatsApp: () -> Unit,
  onNavigateToAppRestrictions: () -> Unit,
  onNavigateToAutomationSettings: () -> Unit = {},
  onNavigateToPendingTransactions: () -> Unit = {},
  onReplayTutorial: () -> Unit = {},
  onNavigateToProfile: () -> Unit = {},
  mainViewModel: com.example.viewmodel.MainViewModel? = null,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val settings by viewModel.userSettings.collectAsState()
  val categories by viewModel.categories.collectAsState()
  val aiConfig by viewModel.aiConfig.collectAsState()

  var showDailyLimitDialog by remember { mutableStateOf(false) }
  var showInitialBalanceDialog by remember { mutableStateOf(false) }
  var showBankSelectionDialog by remember { mutableStateOf(false) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var showRealAiConfigDialog by remember { mutableStateOf(false) }
  var showThemeDialog by remember { mutableStateOf(false) }
  var showSecurityDialog by remember { mutableStateOf(false) }
  var showTrackingDialog by remember { mutableStateOf(false) }
  var showBankIntegrationInfoDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.background
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header
      item {
        Column {
          Text(
            text = "Settings",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Text(
            text = "Your profile, balance tracking, AI and payment-app limits",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      // 1. Profile + how the balance is tracked
      item {
        com.example.ui.components.GlassBackground(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
          Column(Modifier.fillMaxWidth().padding(18.dp).testTag("profile_settings_card")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(Modifier.size(54.dp).clip(CircleShape).background(com.example.ui.components.Glass.Primary), contentAlignment = Alignment.Center) {
                Text((settings.userName.takeIf { it.isNotBlank() } ?: "U").take(1).uppercase(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
              }
              Spacer(Modifier.width(14.dp))
              Column(Modifier.weight(1f)) {
                Text(settings.userName.ifBlank { "Your name" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(settings.userEmail, fontSize = 12.sp, color = Color(0xFFA5B1CC))
              }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              com.example.ui.components.GlassButton("Edit profile", onNavigateToProfile, Modifier.weight(1f), style = com.example.ui.components.GlassButtonStyle.Soft)
              com.example.ui.components.GlassButton(
                if (settings.trackingMode == "AUTO") "Automatic balance" else "Manual balance",
                { showTrackingDialog = true }, Modifier.weight(1f), style = com.example.ui.components.GlassButtonStyle.Soft
              )
            }
          }
        }
      }

      // 2. Financial Controls Section
      item {
        Text(
          text = "Financial Controls",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.TrackChanges,
              title = "Daily Spending Limit",
              subtitle = "Current: ₹${settings.dailySpendingLimit.toInt()} / day",
              onClick = { showDailyLimitDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.AccountBalance,
              title = "Opening / Initial Balance",
              subtitle = "Current: ₹${settings.initialBalance.toInt()}",
              onClick = { showInitialBalanceDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.AccountBalance,
              title = "Selected Primary Bank",
              subtitle = settings.selectedBank,
              onClick = { showBankSelectionDialog = true }
            )
          }
        }
      }

      // 3. App Restriction & Accessibility Service (Task 5)
      item {
        Text(
          text = "Security & Budget Enforcement (Task 5)",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.Block,
              title = "Payment App Restrictions",
              subtitle = if (settings.accessibilityServiceEnabled) "Enforcement Service ACTIVE • Monitored" else "Configure UPI app blocking when limit exceeded",
              onClick = onNavigateToAppRestrictions
            )
          }
        }
      }

      // 4. Categories Management
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Categories (${categories.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
          TextButton(onClick = { showAddCategoryDialog = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add Custom", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            categories.take(6).forEach { cat ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary)
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                if (!cat.isDefault) {
                  IconButton(
                    onClick = { viewModel.deleteCategory(cat) },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                  }
                } else {
                  Text("Default", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
              }
            }
          }
        }
      }

      // 5. Integrations & Real AI (Tasks 3 & 4)
      item {
        Text(
          text = "Real Integrations & AI Engine",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.TrackChanges,
              title = "SBI Statement Automation",
              subtitle = "Automated WhatsApp Mini Statement extraction & balance sync",
              onClick = onNavigateToAutomationSettings
            )
            SettingsItemRow(
              icon = Icons.Default.Category,
              title = "Pending Statement Review",
              subtitle = "Review & confirm unassigned bank transactions",
              onClick = onNavigateToPendingTransactions
            )
            SettingsItemRow(
              icon = Icons.Default.AutoAwesome,
              title = "AI Intelligence Engine",
              subtitle = if (aiConfig?.isConfigured == true) "Active: ${aiConfig?.provider} (Validated Live)" else "Configure Gemini / OpenAI API Key",
              onClick = { showRealAiConfigDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.Phone,
              title = "WhatsApp Integration",
              subtitle = if (settings.isWhatsAppConnected) "Active with ${settings.whatsAppPhoneNumber}" else "Link WhatsApp via QR/Pairing code or Meta API",
              onClick = onNavigateToWhatsApp
            )
            SettingsItemRow(
              icon = Icons.Default.AccountBalance,
              title = "Bank Transaction Integration",
              subtitle = "SBI Supported • WhatsApp & SMS Statement Engine",
              onClick = { showBankIntegrationInfoDialog = true }
            )
          }
        }
      }

      // 6. App Preferences & Security
      item {
        Text(
          text = "Preferences & System Integrity",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column {
            SettingsItemRow(
              icon = Icons.Default.DarkMode,
              title = "Theme Appearance",
              subtitle = settings.themeMode,
              onClick = { showThemeDialog = true }
            )
            SettingsItemRow(
              icon = Icons.Default.Security,
              title = "Privacy & security",
              subtitle = "How your data is protected",
              onClick = { showSecurityDialog = true }
            )
          }
        }
      }

      // Logout Button
      item {
        Button(
          onClick = onLogout,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("logout_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.12f), contentColor = ExpenseRed)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out from FinPulse", fontWeight = FontWeight.Bold, fontSize = 14.sp)
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(30.dp))
      }
    }
  }

  // --- Dialogs ---

  // Real AI Configuration Dialog (Task 4)
  if (showRealAiConfigDialog) {
    val providersFromServer by viewModel.aiProviders.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadAiProviders() }
    val providers = providersFromServer.ifEmpty {
      listOf("GEMINI" to "Gemini", "OPENAI" to "ChatGPT", "ANTHROPIC" to "Claude", "DEEPSEEK" to "DeepSeek", "GROQ" to "Groq", "MISTRAL" to "Mistral",
        "OPENROUTER" to "OpenRouter", "XAI" to "Grok", "TOGETHER" to "Together", "PERPLEXITY" to "Perplexity", "CUSTOM" to "Other")
        .map { com.example.data.model.AiProviderDto(it.first, it.second, it.first == "CUSTOM") }
    }
    var selectedProvider by remember { mutableStateOf(aiConfig?.provider ?: "GEMINI") }
    var apiKeyInput by remember { mutableStateOf("") }
    var modelInput by remember { mutableStateOf("") }
    var baseUrlInput by remember { mutableStateOf("") }
    var isValidating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val needsBase = providers.firstOrNull { it.id == selectedProvider }?.needsBaseUrl == true

    com.example.ui.components.GlassDialog(
      onDismiss = { showRealAiConfigDialog = false },
      title = "Connect your AI",
      icon = Icons.Default.AutoAwesome,
      iconTint = com.example.ui.components.Glass.Violet,
      message = "Paste your own API key. It is tested right away and stored encrypted.",
      dismissOnOutside = false,
      actions = {
        com.example.ui.components.GlassButton(
          "Connect", {
            if (apiKeyInput.trim().isBlank()) { validationError = "Please enter your API key."; return@GlassButton }
            if (needsBase && baseUrlInput.isBlank()) { validationError = "Enter the https address of the service."; return@GlassButton }
            isValidating = true; validationError = null
            viewModel.validateAndSaveAiConfig(
              provider = selectedProvider, apiKey = apiKeyInput.trim(),
              onSuccess = { msg -> isValidating = false; Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); showRealAiConfigDialog = false },
              onError = { err -> isValidating = false; validationError = err },
              model = modelInput, baseUrl = baseUrlInput
            )
          }, Modifier.fillMaxWidth().testTag("validate_save_ai_key_button"), loading = isValidating
        )
        com.example.ui.components.GlassButton("Cancel", { showRealAiConfigDialog = false }, Modifier.fillMaxWidth(), style = com.example.ui.components.GlassButtonStyle.Soft)
      },
      body = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          validationError?.let { com.example.ui.components.GlassMessage(it) }
          androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            providers.forEach { p ->
              androidx.compose.material3.FilterChip(
                selected = selectedProvider == p.id, onClick = { selectedProvider = p.id; validationError = null },
                label = { Text(p.label.substringBefore(" ("), fontSize = 12.sp) },
                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                  labelColor = com.example.ui.components.Glass.TextHi, selectedLabelColor = Color.White,
                  containerColor = Color.White.copy(alpha = 0.08f), selectedContainerColor = com.example.ui.components.Glass.Indigo.copy(alpha = 0.75f)
                )
              )
            }
          }
          com.example.ui.components.GlassTextField(apiKeyInput, { apiKeyInput = it; validationError = null }, "API key", password = true, modifier = Modifier.testTag("ai_api_key_input"))
          if (needsBase) com.example.ui.components.GlassTextField(baseUrlInput, { baseUrlInput = it }, "Service address (https://…/v1)")
          com.example.ui.components.GlassTextField(modelInput, { modelInput = it }, "Model (optional)", supportingText = "Leave empty to pick automatically")
        }
      }
    )
  }


  // Daily Spending Limit Dialog
  if (showDailyLimitDialog) {
    var limitInput by remember { mutableStateOf(settings.dailySpendingLimit.toInt().toString()) }
    GlassAlertDialog(
      onDismissRequest = { showDailyLimitDialog = false },
      title = { Text("Daily Spending Limit") },
      text = {
        Column {
          Text("Set your daily expense threshold in ₹. The app will warn you and trigger restrictions when exceeded.")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = limitInput,
            onValueChange = { limitInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("edit_daily_limit_field")
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          limitInput.toDoubleOrNull()?.let { viewModel.updateDailyLimit(it) }
          showDailyLimitDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDailyLimitDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Initial Balance Dialog
  if (showInitialBalanceDialog) {
    var balInput by remember { mutableStateOf(settings.initialBalance.toInt().toString()) }
    GlassAlertDialog(
      onDismissRequest = { showInitialBalanceDialog = false },
      title = { Text("Initial Account Balance") },
      text = {
        Column {
          Text("Adjust your opening bank/cash balance. Total balance will automatically recalculate.")
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = balInput,
            onValueChange = { balInput = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(onClick = {
          balInput.toDoubleOrNull()?.let { viewModel.updateInitialBalance(it) }
          showInitialBalanceDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showInitialBalanceDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Bank Selection Dialog
  if (showBankSelectionDialog) {
    val banks = listOf("State Bank of India (SBI)", "HDFC Bank", "ICICI Bank", "Punjab National Bank (PNB)", "Bank of Baroda", "Other Indian Bank")
    GlassAlertDialog(
      onDismissRequest = { showBankSelectionDialog = false },
      title = { Text("Select Primary Bank") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          banks.forEach { bank ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.updateBank(bank)
                  showBankSelectionDialog = false
                }
                .padding(vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = bank,
                fontWeight = if (settings.selectedBank == bank) FontWeight.Bold else FontWeight.Normal,
                color = if (settings.selectedBank == bank) PrimaryBlue else MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showBankSelectionDialog = false }) { Text("Close") }
      }
    )
  }

  // Add Category Dialog
  if (showAddCategoryDialog) {
    var catName by remember { mutableStateOf("") }
    var catType by remember { mutableStateOf(TransactionType.EXPENSE) }
    GlassAlertDialog(
      onDismissRequest = { showAddCategoryDialog = false },
      title = { Text("Add Custom Category") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = catName,
            onValueChange = { catName = it },
            placeholder = { Text("e.g. Gym, Pet Care, Streaming") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { catType = TransactionType.EXPENSE },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (catType == TransactionType.EXPENSE) ExpenseRed else Color.LightGray
              )
            ) { Text("Expense") }
            Button(
              onClick = { catType = TransactionType.INCOME },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (catType == TransactionType.INCOME) IncomeGreen else Color.LightGray
              )
            ) { Text("Income") }
          }
        }
      },
      confirmButton = {
        Button(onClick = {
          if (catName.isNotBlank()) {
            viewModel.addCustomCategory(catName.trim(), catType, "#1E3E62", "category")
            showAddCategoryDialog = false
          }
        }) { Text("Add") }
      },
      dismissButton = {
        TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
      }
    )
  }

  // Theme Appearance Dialog
  if (showThemeDialog) {
    GlassAlertDialog(
      onDismissRequest = { showThemeDialog = false },
      title = { Text("Theme Appearance") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.updateTheme(mode)
                  showThemeDialog = false
                }
                .padding(8.dp)
            ) {
              Text(mode, fontWeight = if (settings.themeMode == mode) FontWeight.Bold else FontWeight.Normal)
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Close") } }
    )
  }

  // Future Bank Integration Info
  if (showBankIntegrationInfoDialog) {
    GlassAlertDialog(
      onDismissRequest = { showBankIntegrationInfoDialog = false },
      title = { Text("Bank Transaction Architecture") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Bank Integration Architecture Specifications:")
          Text("• Default: State Bank of India (SBI).")
          Text("• Non-invasive legal architecture: Authorized bank SMS pattern recognition & bank-authorized push notifications.")
          Text("• Strict Zero-Trust: Never requests banking passwords, OTPs, PINs, or credentials.")
          Text("• Interactive category prompt after detected transaction.")
        }
      },
      confirmButton = { Button(onClick = { showBankIntegrationInfoDialog = false }) { Text("Understood") } }
    )
  }

  // Security & Cloud Architecture Info
  if (showSecurityDialog) {
    com.example.ui.components.GlassDialog(
      onDismiss = { showSecurityDialog = false },
      title = "Privacy & security",
      icon = Icons.Default.Security,
      iconTint = com.example.ui.components.Glass.Mint,
      actions = { com.example.ui.components.GlassButton("Done", { showSecurityDialog = false }, Modifier.fillMaxWidth()) },
      body = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            "Your account works on one phone at a time. Signing in somewhere else signs this phone out.",
            "We never ask for your bank password, PIN or OTP.",
            "Your data is saved on your account and comes back after a reinstall.",
            "Your AI key is stored encrypted and is never shown again.",
            "Only your own SBI statement chat is read. Other chats are never opened."
          ).forEach { Text("• $it", color = com.example.ui.components.Glass.TextHi, fontSize = 13.sp, lineHeight = 18.sp) }
        }
      }
    )
  }

  if (showTrackingDialog && mainViewModel != null) {
    var pickMode by remember { mutableStateOf(if (settings.trackingMode == "MANUAL") "MANUAL" else "AUTO") }
    var balanceInput by remember { mutableStateOf("") }
    var trackErr by remember { mutableStateOf<String?>(null) }
    var trackBusy by remember { mutableStateOf(false) }
    com.example.ui.components.GlassDialog(
      onDismiss = { showTrackingDialog = false },
      title = "Balance tracking",
      icon = Icons.Default.AccountBalance,
      message = "Choose how FinPulse keeps your balance.",
      actions = {
        com.example.ui.components.GlassButton(
          "Save", {
            val bal = balanceInput.toDoubleOrNull()
            if (pickMode == "MANUAL" && bal == null) { trackErr = "Enter your current balance."; return@GlassButton }
            trackBusy = true; trackErr = null
            mainViewModel.changeTrackingMode(pickMode, bal, onDone = { trackBusy = false; showTrackingDialog = false }, onError = { trackBusy = false; trackErr = it })
          }, Modifier.fillMaxWidth(), loading = trackBusy
        )
        com.example.ui.components.GlassButton("Cancel", { showTrackingDialog = false }, Modifier.fillMaxWidth(), style = com.example.ui.components.GlassButtonStyle.Soft)
      },
      body = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          com.example.ui.components.GlassButton("Automatic: from my bank statement" + if (pickMode == "AUTO") "  ✓" else "", { pickMode = "AUTO" }, Modifier.fillMaxWidth(), style = if (pickMode == "AUTO") com.example.ui.components.GlassButtonStyle.Primary else com.example.ui.components.GlassButtonStyle.Soft)
          com.example.ui.components.GlassButton("Manual: I type everything" + if (pickMode == "MANUAL") "  ✓" else "", { pickMode = "MANUAL" }, Modifier.fillMaxWidth(), style = if (pickMode == "MANUAL") com.example.ui.components.GlassButtonStyle.Primary else com.example.ui.components.GlassButtonStyle.Soft)
          if (pickMode == "MANUAL") com.example.ui.components.GlassTextField(balanceInput, { balanceInput = it.filter { c -> c.isDigit() || c == '.' }.take(12) }, "Current balance (₹)", keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
          trackErr?.let { com.example.ui.components.GlassMessage(it) }
        }
      }
    )
  }

}

@Composable
private fun SettingsItemRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Icon(
      imageVector = Icons.Default.ChevronRight,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp)
    )
  }
}
