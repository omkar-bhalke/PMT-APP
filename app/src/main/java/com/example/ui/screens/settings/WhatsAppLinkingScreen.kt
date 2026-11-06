package com.example.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavySurface
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.StatementAutomationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppLinkingScreen(
  viewModel: SettingsViewModel,
  automationViewModel: StatementAutomationViewModel? = null,
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val settings by viewModel.userSettings.collectAsState()
  val whatsAppStatus by viewModel.whatsAppStatus.collectAsState()

  var selectedTab by remember { mutableIntStateOf(0) } // 0: Railway Worker, 1: Meta Cloud API

  // Meta State
  var metaPhoneNumberText by remember { mutableStateOf(settings.whatsAppPhoneNumber ?: "+91 ") }
  var accessToken by remember { mutableStateOf("") }
  var phoneNumberId by remember { mutableStateOf("") }
  var wabaId by remember { mutableStateOf("") }
  var showAdvancedMetaConfig by remember { mutableStateOf(false) }
  var metaLoading by remember { mutableStateOf(false) }
  var metaErrorMessage by remember { mutableStateOf<String?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("WhatsApp Integration", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        navigationIcon = {
          IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Tab switcher
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = MaterialTheme.colorScheme.primary
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("Railway SBI Worker", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("Meta Cloud API", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
        )
      }

      if (selectedTab == 0) {
        com.example.ui.screens.sbi.SbiWhatsAppLinkPanel(automationViewModel)
      } else {
        // ==========================================
        // TAB 1: META GRAPH API GATEWAY (TASK 3)
        // ==========================================
        val isMetaConnected = whatsAppStatus?.connected == true || settings.isWhatsAppConnected

        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = NavySurface)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Meta WhatsApp Business Cloud API Gateway",
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Official Meta Cloud API webhook gateway. Requires Meta Developer Graph credentials. Fully compliant with Meta TOS.",
              color = Color(0xFF94A3B8),
              fontSize = 12.sp,
              lineHeight = 17.sp
            )
          }
        }

        // Live Connection Status
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Meta Gateway Status", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Text(
                text = if (isMetaConnected) "Active with ${whatsAppStatus?.phoneNumber ?: settings.whatsAppPhoneNumber}" else "No active gateway link",
                fontSize = 12.sp,
                color = if (isMetaConnected) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background((if (isMetaConnected) IncomeGreen else Color(0xFF64748B)).copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = if (isMetaConnected) "CONNECTED" else "NOT CONNECTED",
                color = if (isMetaConnected) IncomeGreen else Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        if (metaErrorMessage != null) {
          Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.15f))
          ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Error, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = metaErrorMessage!!, color = ExpenseRed, fontSize = 12.sp)
            }
          }
        }

        // Phone Input Field
        Column {
          Text("WhatsApp Business Phone Number", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.height(4.dp))
          OutlinedTextField(
            value = metaPhoneNumberText,
            onValueChange = {
              metaPhoneNumberText = it
              metaErrorMessage = null
            },
            placeholder = { Text("+91 9876543210") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("meta_whatsapp_phone_input")
          )
        }

        OutlinedButton(
          onClick = { showAdvancedMetaConfig = !showAdvancedMetaConfig },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (showAdvancedMetaConfig) "Hide Meta Graph Credentials" else "Custom Meta Credentials (Optional)")
        }

        if (showAdvancedMetaConfig) {
          Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Text("Graph API Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              OutlinedTextField(
                value = accessToken,
                onValueChange = { accessToken = it },
                label = { Text("System User Access Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meta_access_token_input")
              )
              OutlinedTextField(
                value = phoneNumberId,
                onValueChange = { phoneNumberId = it },
                label = { Text("Phone Number ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meta_phone_id_input")
              )
              OutlinedTextField(
                value = wabaId,
                onValueChange = { wabaId = it },
                label = { Text("WABA Account ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meta_waba_id_input")
              )
            }
          }
        }

        if (metaLoading) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = IncomeGreen)
          }
        } else if (!isMetaConnected) {
          Button(
            onClick = {
              if (metaPhoneNumberText.trim().length < 10) {
                metaErrorMessage = "Please enter a valid phone number with country code."
                return@Button
              }
              metaLoading = true
              metaErrorMessage = null
              viewModel.connectMetaWhatsApp(
                phoneNumber = metaPhoneNumberText.trim(),
                accessToken = accessToken.ifBlank { null },
                phoneNumberId = phoneNumberId.ifBlank { null },
                wabaId = wabaId.ifBlank { null },
                onSuccess = {
                  metaLoading = false
                  Toast.makeText(context, "Meta WhatsApp Cloud API linked!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                  metaLoading = false
                  metaErrorMessage = err
                }
              )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("link_meta_whatsapp_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Connect Meta Gateway", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          OutlinedButton(
            onClick = {
              metaLoading = true
              metaErrorMessage = null
              viewModel.disconnectMetaWhatsApp(
                onSuccess = {
                  metaLoading = false
                  Toast.makeText(context, "WhatsApp disconnected.", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                  metaLoading = false
                  metaErrorMessage = err
                }
              )
            },
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("disconnect_meta_whatsapp_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
          ) {
            Text("Disconnect Meta Gateway", fontWeight = FontWeight.Bold)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
