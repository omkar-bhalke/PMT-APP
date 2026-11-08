package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Glass
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconBadge
import com.example.ui.components.GlassMessage
import com.example.ui.components.GlassTextField
import com.example.viewmodel.MainViewModel

@Composable
fun LoginScreen(
  viewModel: MainViewModel,
  onLoginSuccess: (isFirstTime: Boolean) -> Unit,
  onNavigateToRegister: () -> Unit,
  modifier: Modifier = Modifier
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  val notice by viewModel.notice.collectAsState()

  GlassBackground(modifier) {
    Column(
      Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Spacer(Modifier.height(32.dp))
      GlassIconBadge(Icons.Default.TrendingUp, Glass.Sky, size = 76.dp)
      Spacer(Modifier.height(16.dp))
      Text("FinPulse", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
      Text("Know where every rupee goes.", fontSize = 14.sp, color = Glass.TextLo, textAlign = TextAlign.Center)
      Spacer(Modifier.height(28.dp))

      GlassCard(Modifier.fillMaxWidth(), corner = 30.dp, contentPadding = 22.dp) {
        Text("Welcome back", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
        Spacer(Modifier.height(4.dp))
        Text("Sign in to continue", fontSize = 13.sp, color = Glass.TextLo)
        Spacer(Modifier.height(16.dp))

        notice?.let { GlassMessage(it, isError = false); Spacer(Modifier.height(12.dp)) }
        errorMessage?.let { GlassMessage(it); Spacer(Modifier.height(12.dp)) }

        GlassTextField(email, { email = it; errorMessage = null }, "Email", leading = Icons.Default.Email, keyboardType = KeyboardType.Email, modifier = Modifier.testTag("login_email"))
        Spacer(Modifier.height(12.dp))
        GlassTextField(
          password, { password = it; errorMessage = null }, "Password", leading = Icons.Default.Lock, password = true, passwordVisible = passwordVisible,
          trailing = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show password", tint = Glass.TextLo) } },
          modifier = Modifier.testTag("login_password")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          TextButton(onClick = { showForgotPasswordDialog = true }) { Text("Forgot password?", color = Glass.Sky, fontSize = 13.sp) }
        }
        Spacer(Modifier.height(6.dp))
        GlassButton(
          text = "Sign in", loading = isLoading, modifier = Modifier.fillMaxWidth().testTag("login_submit_button"),
          onClick = {
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { errorMessage = "Please enter a valid email address."; return@GlassButton }
            if (password.isBlank()) { errorMessage = "Please enter your password."; return@GlassButton }
            isLoading = true; errorMessage = null; viewModel.clearNotice()
            viewModel.loginWithEmailPassword(
              email = email.trim(), pass = password, rememberMe = true,
              onSuccess = { isFirstTime -> isLoading = false; onLoginSuccess(isFirstTime) },
              onError = { err -> isLoading = false; errorMessage = err }
            )
          }
        )
      }

      Spacer(Modifier.height(18.dp))
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text("New to FinPulse?", color = Glass.TextLo, fontSize = 14.sp)
        TextButton(onClick = onNavigateToRegister, modifier = Modifier.testTag("create_account_button")) {
          Text("Create account", color = Glass.Sky, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
      Spacer(Modifier.height(24.dp))
    }
  }

  if (showForgotPasswordDialog) {
    ForgotPasswordDialog(initialEmail = email, viewModel = viewModel, onDismiss = { showForgotPasswordDialog = false })
  }
}
