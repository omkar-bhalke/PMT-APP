package com.example.data.remote

import com.example.data.model.AiAnalysisRequest
import com.example.data.model.AiAnalysisResponse
import com.example.data.model.AiConfigResponse
import com.example.data.model.AppRestrictionsResponse
import com.example.data.model.AuthResponse
import com.example.data.model.DashboardSummaryResponse
import com.example.data.model.DeviceResetRequest
import com.example.data.model.DeviceStatusResponse
import com.example.data.model.EmergencyReviewRequest
import com.example.data.model.EmergencyReviewResponse
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.GenericApiResponse
import com.example.data.model.LogRestrictionEventRequest
import com.example.data.model.LoginRequest
import com.example.data.model.MetaWhatsAppConnectRequest
import com.example.data.model.RefreshTokenRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.ResetPasswordRequest
import com.example.data.model.SyncBatchRequest
import com.example.data.model.SyncBatchResponse
import com.example.data.model.TransactionDto
import com.example.data.model.TransactionResponse
import com.example.data.model.UpdateRestrictionsRequest
import com.example.data.model.ValidateAiKeyRequest
import com.example.data.model.WhatsAppStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FinPulseApiService {
  // 1. Authentication & Device Binding (Tasks 1 & 2)
  @POST("api/auth/register")
  suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

  @POST("api/auth/login")
  suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

  @POST("api/auth/forgot-password")
  suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<GenericApiResponse>

  @POST("api/auth/reset-password")
  suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericApiResponse>

  @POST("api/auth/refresh-token")
  suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

  @GET("api/auth/device-status")
  suspend fun checkDeviceStatus(@Query("deviceId") deviceId: String): Response<DeviceStatusResponse>

  @POST("api/auth/device-reset")
  suspend fun requestDeviceReset(@Body request: DeviceResetRequest): Response<GenericApiResponse>

  // 2. Dashboard & Summary
  @GET("api/dashboard/summary")
  suspend fun getDashboardSummary(): Response<DashboardSummaryResponse>

  // 3. Transactions & Sync
  @POST("api/transactions")
  suspend fun createTransaction(@Body transaction: TransactionDto): Response<TransactionResponse>

  @PUT("api/transactions/{id}")
  suspend fun updateTransaction(
    @Path("id") id: String,
    @Body transaction: TransactionDto
  ): Response<TransactionResponse>

  @DELETE("api/transactions/{id}")
  suspend fun deleteTransaction(@Path("id") id: String): Response<GenericApiResponse>

  @POST("api/transactions/sync-batch")
  suspend fun syncBatchTransactions(@Body request: SyncBatchRequest): Response<SyncBatchResponse>

  // 4. Real AI Provider Management (Task 4)
  @GET("api/ai/config")
  suspend fun getAiConfig(): Response<AiConfigResponse>

  @POST("api/ai/validate-and-save")
  suspend fun validateAndSaveAiConfig(@Body request: ValidateAiKeyRequest): Response<AiConfigResponse>

  @DELETE("api/ai/config")
  suspend fun deleteAiConfig(): Response<GenericApiResponse>

  @POST("api/ai/analyze")
  suspend fun analyzeSpending(@Body request: AiAnalysisRequest): Response<AiAnalysisResponse>

  @POST("api/ai/emergency-review")
  suspend fun reviewEmergency(@Body request: EmergencyReviewRequest): Response<EmergencyReviewResponse>

  // 5. Real WhatsApp Integration (Task 3)
  @GET("api/integrations/whatsapp/status")
  suspend fun getWhatsAppStatus(): Response<WhatsAppStatusResponse>

  @POST("api/integrations/whatsapp/connect")
  suspend fun connectWhatsApp(@Body request: MetaWhatsAppConnectRequest): Response<WhatsAppStatusResponse>

  @DELETE("api/integrations/whatsapp/disconnect")
  suspend fun disconnectWhatsApp(): Response<GenericApiResponse>

  // 6. Real App Restrictions & Accessibility Logging (Task 5)
  @GET("api/restrictions")
  suspend fun getRestrictions(): Response<AppRestrictionsResponse>

  @POST("api/restrictions")
  suspend fun updateRestrictions(@Body request: UpdateRestrictionsRequest): Response<GenericApiResponse>

  @POST("api/restrictions/log-event")
  suspend fun logRestrictionEvent(@Body request: LogRestrictionEventRequest): Response<GenericApiResponse>

  // 7. Full Cloud User Dataset Restore (Requirement 5)
  @GET("api/user/sync-all")
  suspend fun pullCloudData(): Response<com.example.data.model.CloudSyncAllResponse>

  // 8. SBI mini statement: pending queue, balance, automation read-model (cPanel)
  @GET("api/app-config")
  suspend fun getAppConfig(): Response<com.example.data.model.AppConfigResponse>

  @GET("api/automation/overview")
  suspend fun getAutomationOverview(): Response<com.example.data.model.OverviewResponse>

  @GET("api/automation/logs")
  suspend fun getAutomationLogs(@Query("limit") limit: Int = 50): Response<com.example.data.model.AutomationLogsResponse>

  @GET("api/pending-transactions")
  suspend fun getPendingTransactions(): Response<com.example.data.model.PendingTransactionsResponse>

  @GET("api/pending-transactions/{id}")
  suspend fun getPendingTransaction(@Path("id") id: String): Response<com.example.data.model.PendingOneResponse>

  @POST("api/pending-transactions/sync")
  suspend fun syncPendingTransactions(
    @Body request: com.example.data.model.PendingSyncRequest
  ): Response<com.example.data.model.PendingSyncResponse>

  @POST("api/pending-transactions/snooze")
  suspend fun snoozePendingTransaction(
    @Body request: com.example.data.model.SnoozeRequest
  ): Response<com.example.data.model.SnoozeResponse>

  @GET("api/balance/current")
  suspend fun getRealBalance(): Response<com.example.data.model.RealBalanceResponse>

  @GET("api/balance/history")
  suspend fun getBalanceHistory(
    @Query("limit") limit: Int = 30
  ): Response<com.example.data.model.BalanceHistoryResponse>

  @GET("api/categories")
  suspend fun getCategories(): Response<com.example.data.model.CategoriesResponse>

  @POST("api/categories")
  suspend fun addCategory(
    @Body request: com.example.data.model.AddCategoryRequest
  ): Response<com.example.data.model.AddCategoryResponse>

  // 9. Profile, first-time setup, sign-out, AI providers
  @GET("api/profile")
  suspend fun getProfile(): Response<com.example.data.model.ProfileResponse>

  @retrofit2.http.PUT("api/profile")
  suspend fun updateProfile(@Body request: com.example.data.model.UpdateProfileRequest): Response<com.example.data.model.ProfileResponse>

  @retrofit2.http.PUT("api/profile/password")
  suspend fun changePassword(@Body request: com.example.data.model.ChangePasswordRequest): Response<com.example.data.model.GenericApiResponse>

  @retrofit2.http.PUT("api/settings/setup")
  suspend fun completeSetup(@Body request: com.example.data.model.SetupRequest): Response<com.example.data.model.SetupResponse>

  @retrofit2.http.PUT("api/settings/tracking-mode")
  suspend fun changeTrackingMode(@Body request: com.example.data.model.SetupRequest): Response<com.example.data.model.SetupResponse>

  @POST("api/auth/logout")
  suspend fun signOutOnServer(): Response<com.example.data.model.GenericApiResponse>

  @GET("api/ai/providers")
  suspend fun getAiProviders(): Response<com.example.data.model.AiProvidersResponse>

  @POST("api/ai/suggest-category")
  suspend fun suggestCategory(@Body request: com.example.data.model.SuggestCategoryRequest): Response<com.example.data.model.SuggestCategoryResponse>
}
