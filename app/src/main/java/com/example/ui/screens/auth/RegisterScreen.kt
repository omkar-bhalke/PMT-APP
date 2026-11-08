package com.example.ui.screens.auth

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassIconBadge
import com.example.ui.components.GlassMessage
import com.example.ui.components.GlassTextField
import com.example.viewmodel.MainViewModel

@Composable
fun RegisterScreen(
  viewModel: MainViewModel,
  onRegisterSuccess: (isFirstTime: Boolean) -> Unit,
  onNavigateToLogin: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler { onNavigateToLogin() }

  var fullName by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // strength: length, upper-case, digit, symbol
  val score = listOf(password.length >= 8, password.any { it.isUpperCase() }, password.any { it.isDigit() }, password.any { !it.isLetterOrDigit() }).count { it }
  val strengthColor = when (score) { 0, 1 -> Glass.Rose; 2 -> Glass.Amber; else -> Glass.Mint }

  GlassBackground(modifier) {
    Column(
      Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(Modifier.height(28.dp))
      GlassIconBadge(Icons.Default.TrendingUp, Glass.Violet, size = 64.dp)
      Spacer(Modifier.height(12.dp))
      Text("Create your account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Glass.TextHi)
      Text("It takes less than a minute.", fontSize = 13.sp, color = Glass.TextLo)
      Spacer(Modifier.height(22.dp))

      GlassCard(Modifier.fillMaxWidth(), corner = 30.dp, contentPadding = 22.dp) {
        errorMessage?.let { GlassMessage(it); Spacer(Modifier.height(12.dp)) }
        GlassTextField(fullName, { fullName = it; errorMessage = null }, "Full name", leading = Icons.Default.Person)
        Spacer(Modifier.height(12.dp))
        GlassTextField(email, { email = it; errorMessage = null }, "Email", leading = Icons.Default.Email, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(12.dp))
        GlassTextField(
          password, { password = it; errorMessage = null }, "Password", leading = Icons.Default.Lock, password = true, passwordVisible = passwordVisible,
          trailing = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Show password", tint = Glass.TextLo) } }
        )
        if (password.isNotEmpty()) {
          Spacer(Modifier.height(8.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..4) Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(if (i <= score) strengthColor else Color.White.copy(alpha = 0.15f)))
          }
          Text("Use 8+ characters with a capital letter, a number and a symbol.", fontSize = 11.sp, color = Glass.TextLo, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        GlassTextField(confirmPassword, { confirmPassword = it; errorMessage = null }, "Confirm password", leading = Icons.Default.Lock, password = true, passwordVisible = passwordVisible)
        Spacer(Modifier.height(18.dp))
        GlassButton(
          text = "Create account", loading = isLoading, modifier = Modifier.fillMaxWidth(),
          onClick = {
            if (fullName.trim().length < 2) { errorMessage = "Please enter your full name."; return@GlassButton }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { errorMessage = "Please enter a valid email address."; return@GlassButton }
            if (password.length < 8) { errorMessage = "Password must be at least 8 characters long."; return@GlassButton }
            if (password != confirmPassword) { errorMessage = "Passwords do not match."; return@GlassButton }
            isLoading = true; errorMessage = null
            viewModel.register(
              name = fullName.trim(), email = email.trim(), pass = password, confirmPass = confirmPassword,
              onSuccess = { isFirstTime -> isLoading = false; onRegisterSuccess(isFirstTime) },
              onError = { err -> isLoading = false; errorMessage = err }
            )
          }
        )
      }

      Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account?", color = Glass.TextLo, fontSize = 14.sp)
        TextButton(onClick = onNavigateToLogin) { Text("Sign in", color = Glass.Sky, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}
