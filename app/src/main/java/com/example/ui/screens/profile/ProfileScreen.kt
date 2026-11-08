package com.example.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Glass
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassButtonStyle
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassDialog
import com.example.ui.components.GlassMessage
import com.example.ui.components.GlassTextField
import com.example.viewmodel.MainViewModel

/** View and edit the profile, change the password, sign out. */
@Composable
fun ProfileScreen(viewModel: MainViewModel, onBack: () -> Unit, onSignedOut: () -> Unit, modifier: Modifier = Modifier) {
  BackHandler { onBack() }
  val settings by viewModel.userSettings.collectAsState()
  var name by remember { mutableStateOf(settings.userName) }
  var phone by remember { mutableStateOf("") }
  var loaded by remember { mutableStateOf(false) }
  var profileMsg by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
  var savingProfile by remember { mutableStateOf(false) }

  var current by remember { mutableStateOf("") }
  var newPass by remember { mutableStateOf("") }
  var confirm by remember { mutableStateOf("") }
  var passMsg by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
  var savingPass by remember { mutableStateOf(false) }
  var askSignOut by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.loadProfile { p -> if (p != null && !loaded) { name = p.name; phone = p.phone.orEmpty(); loaded = true } }
  }

  GlassBackground(modifier) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Glass.TextHi) }
        Text("Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
      }
      Spacer(Modifier.height(10.dp))
      Box(Modifier.align(Alignment.CenterHorizontally).size(84.dp).clip(CircleShape).background(Glass.Primary), contentAlignment = Alignment.Center) {
        Text((name.ifBlank { "U" }).take(1).uppercase(), fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }
      Spacer(Modifier.height(18.dp))

      GlassCard(Modifier.fillMaxWidth()) {
        Text("Your details", fontWeight = FontWeight.Bold, color = Glass.TextHi, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        profileMsg?.let { GlassMessage(it.first, isError = !it.second); Spacer(Modifier.height(10.dp)) }
        GlassTextField(name, { name = it; profileMsg = null }, "Full name", leading = Icons.Default.Person)
        Spacer(Modifier.height(10.dp))
        GlassTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' }.take(16); profileMsg = null }, "Mobile number (optional)", leading = Icons.Default.Phone, keyboardType = KeyboardType.Phone)
        Spacer(Modifier.height(10.dp))
        GlassTextField(settings.userEmail, {}, "Email", leading = Icons.Default.Email, supportingText = "Email cannot be changed")
        Spacer(Modifier.height(14.dp))
        GlassButton("Save changes", {
          if (name.trim().length < 2) { profileMsg = "Please enter your name." to false; return@GlassButton }
          savingProfile = true
          viewModel.saveProfile(name, phone) { err -> savingProfile = false; profileMsg = (err ?: "Profile saved.") to (err == null) }
        }, Modifier.fillMaxWidth(), loading = savingProfile)
      }

      Spacer(Modifier.height(16.dp))
      GlassCard(Modifier.fillMaxWidth()) {
        Text("Change password", fontWeight = FontWeight.Bold, color = Glass.TextHi, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        passMsg?.let { GlassMessage(it.first, isError = !it.second); Spacer(Modifier.height(10.dp)) }
        GlassTextField(current, { current = it; passMsg = null }, "Current password", leading = Icons.Default.Lock, password = true)
        Spacer(Modifier.height(10.dp))
        GlassTextField(newPass, { newPass = it; passMsg = null }, "New password", leading = Icons.Default.Lock, password = true)
        Spacer(Modifier.height(10.dp))
        GlassTextField(confirm, { confirm = it; passMsg = null }, "Confirm new password", leading = Icons.Default.Lock, password = true)
        Spacer(Modifier.height(14.dp))
        GlassButton("Update password", {
          if (newPass.length < 8) { passMsg = "New password must be at least 8 characters." to false; return@GlassButton }
          if (newPass != confirm) { passMsg = "Passwords do not match." to false; return@GlassButton }
          savingPass = true
          viewModel.changePassword(current, newPass) { err ->
            savingPass = false
            passMsg = (err ?: "Password updated.") to (err == null)
            if (err == null) { current = ""; newPass = ""; confirm = "" }
          }
        }, Modifier.fillMaxWidth(), loading = savingPass, style = GlassButtonStyle.Soft)
      }

      Spacer(Modifier.height(16.dp))
      GlassButton("Sign out", { askSignOut = true }, Modifier.fillMaxWidth(), style = GlassButtonStyle.Danger)
      Spacer(Modifier.height(30.dp))
    }
  }

  if (askSignOut) {
    GlassDialog(
      onDismiss = { askSignOut = false }, title = "Sign out?", message = "You can sign back in any time. Your data stays safe on your account.",
      actions = {
        GlassButton("Sign out", { askSignOut = false; viewModel.logout(); onSignedOut() }, Modifier.fillMaxWidth(), style = GlassButtonStyle.Danger)
        GlassButton("Stay signed in", { askSignOut = false }, Modifier.fillMaxWidth(), style = GlassButtonStyle.Soft)
      }
    )
  }
}
