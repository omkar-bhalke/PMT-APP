package com.example.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * FinPulse "liquid glass" design system: soft glowing colour blobs, translucent frosted cards with a light
 * edge, glossy gradient buttons, and dialogs that blur whatever is behind them (Android 12+; a clean
 * translucent scrim on older phones).
 */
object Glass {
  val Ink = Color(0xFF070B16)
  val Indigo = Color(0xFF4F46E5)
  val Sky = Color(0xFF38BDF8)
  val Violet = Color(0xFF8B5CF6)
  val Mint = Color(0xFF34D399)
  val Rose = Color(0xFFFB7185)
  val Amber = Color(0xFFFBBF24)
  val TextHi = Color(0xFFF1F5FF)
  val TextLo = Color(0xFFA5B1CC)
  val Primary = Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF7C3AED)))
  val Danger = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFBE123C)))
}

/** Full-screen background: deep gradient + slowly drifting colour glows. */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
  val t = rememberInfiniteTransition(label = "glow")
  val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "drift")
  Box(
    modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(listOf(Color(0xFF0B1230), Glass.Ink)))
      .drawBehind {
        drawCircle(Brush.radialGradient(listOf(Glass.Indigo.copy(alpha = 0.55f), Color.Transparent), Offset(size.width * (0.15f + 0.1f * drift), size.height * 0.08f), size.width * 0.75f), radius = size.width * 0.75f, center = Offset(size.width * (0.15f + 0.1f * drift), size.height * 0.08f))
        drawCircle(Brush.radialGradient(listOf(Glass.Violet.copy(alpha = 0.40f), Color.Transparent), Offset(size.width * (0.95f - 0.1f * drift), size.height * 0.42f), size.width * 0.7f), radius = size.width * 0.7f, center = Offset(size.width * (0.95f - 0.1f * drift), size.height * 0.42f))
        drawCircle(Brush.radialGradient(listOf(Glass.Sky.copy(alpha = 0.28f), Color.Transparent), Offset(size.width * 0.3f, size.height * (0.98f - 0.06f * drift)), size.width * 0.8f), radius = size.width * 0.8f, center = Offset(size.width * 0.3f, size.height * (0.98f - 0.06f * drift)))
      },
    content = content
  )
}

/** Frosted translucent card with a light edge and soft shadow. */
@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  corner: Dp = 24.dp,
  tint: Color? = null,
  onClick: (() -> Unit)? = null,
  contentPadding: Dp = 18.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(corner)
  val fill = Brush.verticalGradient(listOf((tint ?: Color.White).copy(alpha = 0.16f), (tint ?: Color.White).copy(alpha = 0.06f)))
  val edge = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.20f)))
  Column(
    modifier
      .shadow(14.dp, shape, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color.Black.copy(alpha = 0.5f))
      .clip(shape)
      .background(fill)
      .border(BorderStroke(1.dp, edge), shape)
      .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
      .padding(contentPadding),
    content = content
  )
}

enum class GlassButtonStyle { Primary, Soft, Danger }

@Composable
fun GlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  loading: Boolean = false,
  style: GlassButtonStyle = GlassButtonStyle.Primary,
  icon: ImageVector? = null
) {
  val shape = RoundedCornerShape(18.dp)
  val bg: Brush = when (style) {
    GlassButtonStyle.Primary -> Glass.Primary
    GlassButtonStyle.Danger -> Glass.Danger
    GlassButtonStyle.Soft -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.08f)))
  }
  Box(
    modifier
      .height(52.dp)
      .alpha(if (enabled) 1f else 0.5f)
      .clip(shape)
      .background(bg)
      .border(BorderStroke(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f)))), shape)
      .clickable(enabled = enabled && !loading, onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
    else Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
      if (icon != null) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
      Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
  }
}

@Composable
fun GlassTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  leading: ImageVector? = null,
  trailing: @Composable (() -> Unit)? = null,
  password: Boolean = false,
  passwordVisible: Boolean = false,
  keyboardType: KeyboardType = KeyboardType.Text,
  singleLine: Boolean = true,
  minLines: Int = 1,
  placeholder: String? = null,
  supportingText: String? = null
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = placeholder?.let { { Text(it) } },
    leadingIcon = leading?.let { { Icon(it, null) } },
    trailingIcon = trailing,
    supportingText = supportingText?.let { { Text(it) } },
    singleLine = singleLine,
    minLines = minLines,
    visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
    keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedTextColor = Glass.TextHi, unfocusedTextColor = Glass.TextHi,
      focusedContainerColor = Color.White.copy(alpha = 0.10f), unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
      focusedBorderColor = Glass.Sky, unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
      focusedLabelColor = Glass.Sky, unfocusedLabelColor = Glass.TextLo,
      cursorColor = Glass.Sky, focusedLeadingIconColor = Glass.Sky, unfocusedLeadingIconColor = Glass.TextLo,
      focusedPlaceholderColor = Glass.TextLo, unfocusedPlaceholderColor = Glass.TextLo,
      focusedSupportingTextColor = Glass.TextLo, unfocusedSupportingTextColor = Glass.TextLo
    ),
    modifier = modifier.fillMaxWidth()
  )
}

/** A small rounded icon "badge" used in headers and popups. */
@Composable
fun GlassIconBadge(icon: ImageVector, tint: Color = Glass.Sky, size: Dp = 56.dp) {
  Box(
    Modifier.size(size).clip(CircleShape)
      .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.40f), tint.copy(alpha = 0.10f))))
      .border(1.dp, tint.copy(alpha = 0.5f), CircleShape),
    contentAlignment = Alignment.Center
  ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f)) }
}

@Composable
fun GlassMessage(text: String, isError: Boolean = true, modifier: Modifier = Modifier) {
  val c = if (isError) Glass.Rose else Glass.Mint
  Box(modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = 0.14f)).border(1.dp, c.copy(alpha = 0.45f), RoundedCornerShape(14.dp)).padding(12.dp)) {
    Text(text, color = Glass.TextHi, fontSize = 13.sp)
  }
}

/**
 * Popup in the same glass style. Blurs the screen behind it on Android 12+.
 * Used for transaction questions, confirmations and warnings so every popup looks like part of the app.
 */
@Composable
fun GlassDialog(
  onDismiss: () -> Unit,
  title: String,
  icon: ImageVector? = null,
  iconTint: Color = Glass.Sky,
  message: String? = null,
  dismissOnOutside: Boolean = true,
  actions: @Composable ColumnScope.() -> Unit,
  body: @Composable (ColumnScope.() -> Unit)? = null
) {
  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = dismissOnOutside)) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
      val lp = window.attributes
      lp.blurBehindRadius = 38
      lp.dimAmount = 0.35f
      window.attributes = lp
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
      GlassCard(Modifier.fillMaxWidth(), corner = 30.dp, tint = Color(0xFF1B2447), contentPadding = 22.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
          if (icon != null) { GlassIconBadge(icon, iconTint); Spacer(Modifier.height(14.dp)) }
          Text(title, color = Glass.TextHi, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
          if (message != null) { Spacer(Modifier.height(8.dp)); Text(message, color = Glass.TextLo, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp) }
          if (body != null) { Spacer(Modifier.height(12.dp)); body() }
          Spacer(Modifier.height(18.dp))
          Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), content = actions)
        }
      }
    }
  }
}

@Composable
fun glassIsDark(): Boolean = isSystemInDarkTheme()

/**
 * Drop-in glass version of Material's AlertDialog (same parameters), so every remaining popup in the app
 * gets the same frosted look. The content is shown on a dark glass card regardless of the app theme.
 */
@Composable
fun GlassAlertDialog(
  onDismissRequest: () -> Unit,
  confirmButton: @Composable () -> Unit,
  modifier: Modifier = Modifier,
  dismissButton: (@Composable () -> Unit)? = null,
  icon: (@Composable () -> Unit)? = null,
  title: (@Composable () -> Unit)? = null,
  text: (@Composable () -> Unit)? = null,
  shape: androidx.compose.ui.graphics.Shape? = null,
  containerColor: Color? = null,
  properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
  Dialog(onDismissRequest = onDismissRequest, properties = properties) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
      val lp = window.attributes
      lp.blurBehindRadius = 38
      lp.dimAmount = 0.35f
      window.attributes = lp
    }
    androidx.compose.material3.MaterialTheme(
      colorScheme = androidx.compose.material3.darkColorScheme(
        primary = Glass.Sky, onPrimary = Color.White, surface = Color(0xFF1B2447), onSurface = Glass.TextHi,
        onSurfaceVariant = Glass.TextLo, background = Color(0xFF1B2447), onBackground = Glass.TextHi,
        secondary = Glass.Violet, error = Glass.Rose, outline = Color.White.copy(alpha = 0.3f)
      )
    ) {
      Box(Modifier.fillMaxWidth().padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
        GlassCard(modifier.fillMaxWidth(), corner = 28.dp, tint = Color(0xFF1B2447), contentPadding = 22.dp) {
          androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides Glass.TextHi) {
            if (icon != null) { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { icon() }; Spacer(Modifier.height(10.dp)) }
            if (title != null) {
              androidx.compose.material3.ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)) { title() }
              Spacer(Modifier.height(10.dp))
            }
            if (text != null) {
              androidx.compose.material3.ProvideTextStyle(androidx.compose.ui.text.TextStyle(fontSize = 14.sp, lineHeight = 20.sp)) {
                Box(Modifier.fillMaxWidth().heightIn(max = 460.dp).verticalScroll(androidx.compose.foundation.rememberScrollState())) { text() }
              }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
              if (dismissButton != null) { dismissButton(); Spacer(Modifier.width(8.dp)) }
              confirmButton()
            }
          }
        }
      }
    }
  }
}
