package com.example.ui.screens.sbi

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.StatementAutomationViewModel
import kotlinx.coroutines.delay

/** Link / unlink the user's OWN WhatsApp account (stored on Railway). Shows Connected / Reconnecting / Disconnected. */
@Composable
fun SbiWhatsAppLinkPanel(vm: StatementAutomationViewModel?) {
  if (vm == null) { Text("Automation module not available."); return }
  val wa by vm.whatsapp.collectAsState()
  val busy by vm.busy.collectAsState()
  val storagePersistent by vm.storagePersistent.collectAsState()
  val error by vm.error.collectAsState()
  val info by vm.info.collectAsState()
  var phone by remember { mutableStateOf("91") }
  var showDisconnect by remember { mutableStateOf(false) }
  var showAdvanced by remember { mutableStateOf(false) }
  var railwayUrl by remember { mutableStateOf(vm.currentRailwayUrl().orEmpty()) }

  LaunchedEffect(Unit) {
    while (true) { vm.refreshWhatsapp(); delay(3000) }
  }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Honest disclosure
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4E5))
    ) {
      Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309))
        Text(
          "This uses an UNOFFICIAL open-source WhatsApp Web client (Baileys). It is not an official WhatsApp or SBI API. " +
            "It can stop working when WhatsApp changes, and WhatsApp may restrict accounts that automate messages. " +
            "Use a gap of 30 minutes or more and link only an account you can afford to lose access to.",
          fontSize = 12.sp, lineHeight = 17.sp, color = Color(0xFF7C2D12)
        )
      }
    }

    if (!storagePersistent) {
      Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))) {
        Text(
          "Server problem: the Railway server has no Volume attached, so your WhatsApp link would be deleted on every restart and keep disconnecting. " +
            "Ask the admin to attach a Railway Volume with mount path /data. Linking is disabled until then.",
          modifier = Modifier.padding(14.dp), fontSize = 12.sp, color = Color(0xFF7F1D1D)
        )
      }
    }

    // Status
    val (label, color) = when (wa.state) {
      "CONNECTED" -> "Connected" to Color(0xFF10B981)
      "RECONNECTING" -> "Reconnecting" to Color(0xFFF59E0B)
      "CONNECTING", "QR_READY", "PAIRING_CODE_READY" -> "Linking…" to Color(0xFF0284C7)
      else -> "Disconnected" to Color(0xFFEF4444)
    }
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
      Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(12.dp).clip(CircleShape).background(color))
          Spacer(Modifier.size(8.dp))
          Text(label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        if (wa.state == "CONNECTED" && !wa.phoneMasked.isNullOrBlank()) Text("Linked account: ${wa.phoneMasked}", fontSize = 13.sp)
        wa.lastError?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp) }
        if (wa.state == "RECONNECTING") Text("Automation is paused until the connection is back. No statements are lost.", fontSize = 12.sp)
      }
    }

    error?.let {
      Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(it, fontSize = 12.sp, color = Color(0xFF7F1D1D), modifier = Modifier.weight(1f))
          TextButton(onClick = { vm.clearError() }) { Text("OK") }
        }
      }
    }
    info?.let {
      Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
          Text(it, fontSize = 12.sp, color = Color(0xFF14532D), modifier = Modifier.weight(1f))
          TextButton(onClick = { vm.clearInfo() }) { Text("OK") }
        }
      }
    }

    if (wa.state == "CONNECTED" || wa.state == "RECONNECTING") {
      OutlinedButton(onClick = { showDisconnect = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
        Text("Disconnect WhatsApp")
      }
    } else {
      // QR linking
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.QrCode, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Link with QR code", fontWeight = FontWeight.SemiBold)
          }
          val qr = wa.qrDataUrl
          if (wa.state == "QR_READY" && qr != null) {
            val bmp = remember(qr) {
              runCatching {
                val bytes = Base64.decode(qr.substringAfter(","), Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
              }.getOrNull()
            }
            if (bmp != null) {
              Image(bitmap = bmp, contentDescription = "WhatsApp link QR code", modifier = Modifier.size(240.dp).background(Color.White))
            }
            Text(
              "On the phone that has WhatsApp: Settings → Linked devices → Link a device, then scan this code. " +
                "If this phone is the same one, use the pairing code below instead.",
              fontSize = 12.sp, textAlign = TextAlign.Center
            )
          } else if (wa.state == "CONNECTING") {
            CircularProgressIndicator(Modifier.size(28.dp))
            Text("Preparing QR code…", fontSize = 12.sp)
          }
          Button(onClick = { vm.startQrLinking() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text(if (wa.state == "QR_READY") "Refresh QR code" else "Show QR code")
          }
        }
      }

      // Pairing code
      Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Link with pairing code", fontWeight = FontWeight.SemiBold)
          }
          OutlinedTextField(
            value = phone, onValueChange = { phone = it.filter { c -> c.isDigit() }.take(15) },
            label = { Text("Your WhatsApp number with country code") },
            placeholder = { Text("919876543210") }, singleLine = true, modifier = Modifier.fillMaxWidth()
          )
          if (wa.state == "PAIRING_CODE_READY" && !wa.pairingCode.isNullOrBlank()) {
            Text(wa.pairingCode ?: "", fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("In WhatsApp: Settings → Linked devices → Link a device → “Link with phone number instead”, then type this code.", fontSize = 12.sp)
          }
          Button(onClick = { vm.requestPairingCode(phone) }, enabled = !busy && phone.length >= 10, modifier = Modifier.fillMaxWidth()) {
            Text("Get pairing code")
          }
        }
      }
    }

    TextButton(onClick = { showAdvanced = !showAdvanced }) { Text(if (showAdvanced) "Hide advanced" else "Advanced") }
    if (showAdvanced) {
      OutlinedTextField(
        value = railwayUrl, onValueChange = { railwayUrl = it },
        label = { Text("Railway server URL (only if not provided automatically)") },
        singleLine = true, modifier = Modifier.fillMaxWidth()
      )
      Button(onClick = { vm.setRailwayUrl(railwayUrl); vm.refreshWhatsapp(quiet = false) }, enabled = railwayUrl.startsWith("https://")) { Text("Save URL") }
    }
    Spacer(Modifier.height(8.dp))
  }

  if (showDisconnect) {
    AlertDialog(
      onDismissRequest = { showDisconnect = false },
      title = { Text("Disconnect WhatsApp?") },
      text = { Text("Automation will stop fetching statements and the stored WhatsApp session will be deleted from the server. You can link again any time.") },
      confirmButton = { TextButton(onClick = { showDisconnect = false; vm.disconnectWhatsapp() }) { Text("Disconnect") } },
      dismissButton = { TextButton(onClick = { showDisconnect = false }) { Text("Cancel") } }
    )
  }
}
