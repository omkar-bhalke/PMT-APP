package com.example.data.model

data class UserSettings(
  val userId: String = "",
  val userEmail: String = "",
  val userName: String = "",
  val userPhotoUrl: String? = null,
  val currency: String = "INR",
  val currencySymbol: String = "₹",
  val initialBalance: Double = 0.0,
  val dailySpendingLimit: Double = 500.0,
  val selectedBank: String = "State Bank of India (SBI)",
  val onboardingCompleted: Boolean = false,
  val trackingMode: String = "", // AUTO (from bank statement), MANUAL, or empty until chosen
  val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
  val aiProvider: String = "GEMINI", // GEMINI, OPENAI
  val isAiConfigured: Boolean = false,
  val backendUrl: String = "https://radhewins.buzz",
  val authToken: String? = null,
  val refreshToken: String? = null,
  val deviceId: String = "",
  val isWhatsAppConnected: Boolean = false,
  val whatsAppPhoneNumber: String? = null,
  val accessibilityServiceEnabled: Boolean = false
)
