package com.example.ui.screens.sbi

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

object SbiFmt {
  fun rupees(v: Double?): String =
    if (v == null) "—" else "₹" + NumberFormat.getNumberInstance(Locale("en", "IN")).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(v)

  fun signedRupees(v: Double?): String =
    if (v == null) "—" else (if (v > 0) "+" else if (v < 0) "−" else "") + rupees(kotlin.math.abs(v))

  /** ISO-8601 ("2026-11-05T04:51:00Z") -> device-local "05 Nov, 10:21 AM". */
  fun time(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
      SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date.from(Instant.parse(iso)))
    } catch (_: Exception) { iso }
  }

  fun date(isoDate: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoDate)
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(d!!)
  } catch (_: Exception) { isoDate }

  fun statusLabel(s: String): String = when (s) {
    "SUCCESS" -> "Last fetch succeeded"
    "RUNNING" -> "Fetching now…"
    "IDLE" -> "Waiting for next run"
    "STOPPED" -> "Automation stopped"
    "NO_RESPONSE" -> "SBI did not respond (will retry)"
    "PARSE_ERROR" -> "Statement format not understood"
    "PAUSED_DISCONNECTED" -> "Paused: WhatsApp disconnected"
    "DB_ERROR" -> "Saved on server queue, retrying"
    "ERROR" -> "Error (will retry)"
    else -> s
  }
}
