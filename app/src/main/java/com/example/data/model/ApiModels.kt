package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ==========================================
// AUTHENTICATION & DEVICE BINDING MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class RegisterRequest(
  val name: String,
  val email: String,
  val password: String,
  val confirmPassword: String,
  val deviceId: String,
  val deviceFingerprint: String? = null,
  val deviceModel: String? = null,
  val osVersion: String? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
  val email: String,
  val password: String,
  val rememberMe: Boolean = true,
  val deviceId: String? = null,
  val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
  val success: Boolean,
  val token: String? = null,
  val refreshToken: String? = null,
  val isFirstTime: Boolean = false,
  val message: String? = null,
  val code: String? = null,
  val user: UserDto? = null
)

@JsonClass(generateAdapter = true)
data class UserDto(
  val id: String,
  val email: String,
  val name: String,
  val profileImage: String? = null
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
  val email: String
)

@JsonClass(generateAdapter = true)
data class ResetPasswordRequest(
  val email: String,
  val otp: String,
  val newPassword: String,
  val confirmPassword: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
  val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class DeviceStatusResponse(
  val success: Boolean,
  val isBound: Boolean = false,
  val status: String? = null,
  val boundToMaskedEmail: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class DeviceResetRequest(
  val email: String,
  val password: String,
  val deviceId: String,
  val reason: String? = null
)

// ==========================================
// TRANSACTION & SYNC MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class TransactionDto(
  val id: String? = null,
  val amount: Double,
  val type: String,
  val description: String,
  val categoryName: String,
  val categoryId: String? = null,
  val transactionDate: String,
  val transactionTime: String,
  val timestamp: Long? = null,
  val notes: String? = null,
  val idempotencyKey: String? = null,
  val clientLocalId: String? = null
)

@JsonClass(generateAdapter = true)
data class TransactionResponse(
  val success: Boolean,
  val transaction: TransactionDto? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncBatchRequest(
  val pendingTransactions: List<TransactionDto>
)

@JsonClass(generateAdapter = true)
data class SyncBatchResult(
  val clientLocalId: String?,
  val serverId: String?,
  val status: String
)

@JsonClass(generateAdapter = true)
data class SyncBatchResponse(
  val success: Boolean,
  val results: List<SyncBatchResult>? = null
)

// ==========================================
// DASHBOARD & SUMMARY MODELS
// ==========================================

@JsonClass(generateAdapter = true)
data class DashboardSummaryResponse(
  val success: Boolean,
  val data: DashboardSummaryData? = null
)

@JsonClass(generateAdapter = true)
data class DashboardSummaryData(
  val balance: BalanceData? = null,
  val today: TodayData? = null,
  val monthly: MonthlyData? = null,
  val recentTransactions: List<TransactionDto>? = null
)

@JsonClass(generateAdapter = true)
data class BalanceData(
  val currentBalance: Double = 0.0,
  val initialBalance: Double = 0.0,
  val totalIncome: Double = 0.0,
  val totalExpenses: Double = 0.0,
  val bankName: String = "SBI"
)

@JsonClass(generateAdapter = true)
data class TodayData(
  val income: Double = 0.0,
  val expense: Double = 0.0,
  val dailyLimit: Double = 500.0,
  val remainingAllowance: Double = 500.0,
  val utilizationPercentage: Double = 0.0,
  val isExceeded: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MonthlyData(
  val income: Double = 0.0,
  val expense: Double = 0.0,
  val savings: Double = 0.0,
  val spendingPercentage: Double = 0.0
)

// ==========================================
// REAL AI MODELS (TASK 4)
// ==========================================

@JsonClass(generateAdapter = true)
data class ValidateAiKeyRequest(
  val provider: String, // GEMINI, OPENAI
  val apiKey: String
)

@JsonClass(generateAdapter = true)
data class AiConfigResponse(
  val success: Boolean,
  val config: AiConfigDto? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class AiConfigDto(
  val provider: String = "GEMINI",
  val isConfigured: Boolean = false,
  val keyStatus: String = "UNCHECKED",
  val lastValidatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisRequest(
  val customQuery: String? = null
)

@JsonClass(generateAdapter = true)
data class AiAnalysisResponse(
  val success: Boolean,
  val provider: String? = null,
  val analysis: String,
  val requiresConfiguration: Boolean = false,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class EmergencyReviewRequest(
  val requestedAmount: Double,
  val reason: String,
  val screenshotUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class EmergencyReviewResponse(
  val success: Boolean,
  val recommendation: String,
  val confidenceScore: Double = 0.0,
  val suggestedApproval: Boolean = false,
  val requiresUserConfirmation: Boolean = true
)

// ==========================================
// REAL WHATSAPP MODELS (TASK 3)
// ==========================================

@JsonClass(generateAdapter = true)
data class MetaWhatsAppConnectRequest(
  val phoneNumber: String,
  val accessToken: String? = null,
  val phoneNumberId: String? = null,
  val wabaId: String? = null
)

@JsonClass(generateAdapter = true)
data class WhatsAppStatusResponse(
  val success: Boolean,
  val status: String = "NOT_CONNECTED", // NOT_CONNECTED, CONNECTING, CONNECTED, CONNECTION_FAILED, DISCONNECTED
  val phoneNumber: String? = null,
  val verifiedName: String? = null,
  val lastSyncedAt: String? = null,
  val errorMessage: String? = null,
  val message: String? = null
) {
  val isConnected: Boolean get() = status == "CONNECTED"
  val connected: Boolean get() = status == "CONNECTED"
}

// ==========================================
// REAL APP RESTRICTIONS MODELS (TASK 5)
// ==========================================

@JsonClass(generateAdapter = true)
data class AppRestrictionItem(
  val packageName: String,
  val appName: String,
  val isEnabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AppRestrictionsResponse(
  val success: Boolean,
  val restrictions: List<AppRestrictionItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class UpdateRestrictionsRequest(
  val restrictions: List<AppRestrictionItem>
)

@JsonClass(generateAdapter = true)
data class LogRestrictionEventRequest(
  val packageName: String,
  val appName: String,
  val dailyLimit: Double,
  val amountSpent: Double,
  val amountExceeded: Double,
  val actionTaken: String = "BLOCKED"
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
  val success: Boolean,
  val message: String? = null,
  val code: String? = null
)

// ==========================================
// CLOUD RESTORE & SYNC ALL MODELS (REQUIREMENT 5)
// ==========================================

@JsonClass(generateAdapter = true)
data class CloudSyncAllResponse(
  val success: Boolean,
  val message: String? = null,
  val transactions: List<TransactionDto>? = null,
  val categories: List<CategoryDto>? = null,
  val settings: CloudSettingsDto? = null,
  val account: CloudAccountDto? = null,
  val whatsApp: CloudWhatsAppDto? = null
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
  val id: String,
  val name: String,
  val type: String,
  val iconName: String? = "category",
  val colorHex: String? = "#1E3E62",
  val isDefault: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CloudSettingsDto(
  val dailySpendingLimit: Double? = 500.0,
  val selectedBank: String? = "State Bank of India (SBI)",
  val currency: String? = "INR",
  val currencySymbol: String? = "₹",
  val themeMode: String? = "SYSTEM",
  val aiProvider: String? = "GEMINI",
  val onboardingCompleted: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class CloudWhatsAppDto(
  val connected: Boolean = false,
  val phoneNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class CloudAccountDto(
  val initialBalance: Double = 0.0,
  val currentBalance: Double = 0.0,
  val bankName: String = "State Bank of India (SBI)"
)

// ==========================================
// SBI AUTOMATION, PENDING QUEUE, BALANCE  (cPanel + Railway)
// No fake defaults: values the server did not send stay null.
// ==========================================

@JsonClass(generateAdapter = true)
data class PendingTransactionDto(
  val id: String,
  val source: String = "SBI_WHATSAPP_STATEMENT",
  val accountNumber: String? = null,
  val transactionDate: String,
  val amount: Double,
  val type: String, // DEBIT or CREDIT
  val description: String = "",
  val suggestedCategory: String? = null,
  val status: String = "PENDING",
  val snoozedUntil: String? = null,
  val isSnoozed: Boolean = false,
  val createdAt: String? = null
) {
  val isCredit: Boolean get() = type.equals("CREDIT", ignoreCase = true)
}

@JsonClass(generateAdapter = true)
data class PendingTransactionsResponse(
  val success: Boolean,
  val count: Int = 0,
  val items: List<PendingTransactionDto> = emptyList(),
  val serverTime: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingOneResponse(val success: Boolean, val item: PendingTransactionDto? = null, val message: String? = null)

@JsonClass(generateAdapter = true)
data class ConfirmationDto(
  val pendingTransactionId: String,
  val categoryName: String,
  val purpose: String
)

@JsonClass(generateAdapter = true)
data class PendingSyncRequest(val confirmations: List<ConfirmationDto> = emptyList())

@JsonClass(generateAdapter = true)
data class SyncResultDto(
  val pendingTransactionId: String,
  val status: String, // CONFIRMED | ALREADY_CONFIRMED | NOT_FOUND | INVALID | ERROR
  val transactionId: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingSyncResponse(
  val success: Boolean,
  val results: List<SyncResultDto> = emptyList(),
  val count: Int = 0,
  val items: List<PendingTransactionDto> = emptyList(),
  val balance: Double? = null,
  val balanceDifference: Double? = null,
  val serverTime: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class SnoozeRequest(val pendingTransactionId: String, val minutes: Int)

@JsonClass(generateAdapter = true)
data class SnoozeResponse(val success: Boolean, val updated: Boolean = false, val message: String? = null)

@JsonClass(generateAdapter = true)
data class CategoryDto2(
  val id: String,
  val name: String,
  val type: String, // INCOME | EXPENSE
  val iconName: String? = null,
  val colorHex: String? = null,
  val isDefault: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CategoriesResponse(val success: Boolean, val categories: List<CategoryDto2> = emptyList())

@JsonClass(generateAdapter = true)
data class AddCategoryRequest(val name: String, val type: String)

@JsonClass(generateAdapter = true)
data class AddCategoryResponse(val success: Boolean, val category: CategoryDto2? = null, val message: String? = null)

@JsonClass(generateAdapter = true)
data class RealBalanceResponse(
  val success: Boolean,
  val hasData: Boolean = false,
  val currentBalance: Double? = null,
  val previousBalance: Double? = null,
  val balanceDifference: Double? = null,
  val accountNumber: String? = null,
  val bankName: String? = null,
  val lastStatementAt: String? = null,
  val lastSyncedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class BalanceHistoryItem(
  val id: String,
  val accountNumber: String? = null,
  val balance: Double,
  val previousBalance: Double? = null,
  val balanceDifference: Double? = null,
  val newEntries: Int = 0,
  val discrepancy: Double? = null,
  val isBaseline: Boolean = false,
  val statementAt: String? = null,
  val createdAt: String
)

@JsonClass(generateAdapter = true)
data class BalanceHistoryResponse(
  val success: Boolean,
  val count: Int = 0,
  val history: List<BalanceHistoryItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AutomationDto(
  val isEnabled: Boolean = false,
  val intervalMinutes: Int = 30,
  val importInitialEntries: Boolean = false,
  val targetNumber: String? = null,
  val nextRunAt: String? = null,
  val lastRunAt: String? = null,
  val lastSuccessAt: String? = null,
  val lastStatus: String = "IDLE",
  val lastError: String? = null,
  val retryCount: Int = 0,
  val isRunning: Boolean = false,
  val minIntervalMinutes: Int = 5,
  val maxIntervalMinutes: Int = 1440
)

@JsonClass(generateAdapter = true)
data class WhatsAppMetaDto(
  val status: String = "DISCONNECTED",
  val phoneMasked: String? = null,
  val linkedAt: String? = null,
  val lastConnectedAt: String? = null,
  val lastError: String? = null
)

@JsonClass(generateAdapter = true)
data class OverviewResponse(
  val success: Boolean,
  val automation: AutomationDto = AutomationDto(),
  val whatsapp: WhatsAppMetaDto = WhatsAppMetaDto(),
  val balance: RealBalanceResponse = RealBalanceResponse(success = true),
  val pendingCount: Int = 0,
  val serverTime: String? = null
)

@JsonClass(generateAdapter = true)
data class AppConfigResponse(
  val success: Boolean,
  val railwayBaseUrl: String? = null,
  val sbiTargetNumber: String? = null,
  val minIntervalMinutes: Int = 5,
  val maxIntervalMinutes: Int = 1440,
  val whatsappClientNotice: String? = null
)

@JsonClass(generateAdapter = true)
data class AutomationLogDto(
  val id: String,
  val eventType: String,
  val message: String,
  val details: String? = null,
  val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AutomationLogsResponse(val success: Boolean, val count: Int = 0, val logs: List<AutomationLogDto> = emptyList())

// ---------- Railway worker ----------

@JsonClass(generateAdapter = true)
data class RwWhatsAppDto(
  val state: String = "DISCONNECTED", // DISCONNECTED | CONNECTING | QR_READY | PAIRING_CODE_READY | CONNECTED | RECONNECTING
  val connected: Boolean = false,
  val hasSession: Boolean = false,
  val qrDataUrl: String? = null,
  val pairingCode: String? = null,
  val phoneMasked: String? = null,
  val lastError: String? = null
)

@JsonClass(generateAdapter = true)
data class RwStatusResponse(
  val success: Boolean,
  val whatsapp: RwWhatsAppDto = RwWhatsAppDto(),
  val unofficialNotice: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class RwPairRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class RwPairResponse(val success: Boolean, val pairingCode: String? = null, val message: String? = null, val code: String? = null)

@JsonClass(generateAdapter = true)
data class RwStartRequest(val intervalMinutes: Int? = null, val importInitialEntries: Boolean? = null)

@JsonClass(generateAdapter = true)
data class RwIntervalRequest(val intervalMinutes: Int)

@JsonClass(generateAdapter = true)
data class RwSettingsRequest(val importInitialEntries: Boolean)

@JsonClass(generateAdapter = true)
data class RwAutomationResponse(
  val success: Boolean,
  val automation: AutomationDto? = null,
  val message: String? = null,
  val code: String? = null
)

@JsonClass(generateAdapter = true)
data class RwAutomationStatusResponse(
  val success: Boolean,
  val automation: AutomationDto? = null,
  val whatsapp: RwWhatsAppDto = RwWhatsAppDto(),
  val runningNow: Boolean = false,
  val queuedForRetry: Int = 0,
  val serverTime: String? = null,
  val message: String? = null
)

@JsonClass(generateAdapter = true)
data class RwGenericResponse(
  val success: Boolean,
  val message: String? = null,
  val code: String? = null,
  val retryAfterSeconds: Int? = null
)
