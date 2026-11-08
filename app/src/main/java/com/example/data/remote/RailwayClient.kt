package com.example.data.remote

import com.example.data.model.RwAutomationResponse
import com.example.data.model.RwAutomationStatusResponse
import com.example.data.model.RwGenericResponse
import com.example.data.model.RwIntervalRequest
import com.example.data.model.RwPairRequest
import com.example.data.model.RwPairResponse
import com.example.data.model.RwSettingsRequest
import com.example.data.model.RwStartRequest
import com.example.data.model.RwStatusResponse
import com.example.data.model.AutomationLogsResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Railway worker API (WhatsApp linking + automation control). Every call is per-user authenticated. */
interface RailwayApiService {
  @GET("api/whatsapp/status")
  suspend fun whatsappStatus(): Response<RwStatusResponse>

  @POST("api/whatsapp/link/qr")
  suspend fun startQrLinking(): Response<RwStatusResponse>

  @POST("api/whatsapp/link/pair")
  suspend fun requestPairingCode(@Body body: RwPairRequest): Response<RwPairResponse>

  @POST("api/whatsapp/disconnect")
  suspend fun disconnect(): Response<RwGenericResponse>

  @GET("api/automation/status")
  suspend fun automationStatus(): Response<RwAutomationStatusResponse>

  @POST("api/automation/start")
  suspend fun startAutomation(@Body body: RwStartRequest): Response<RwAutomationResponse>

  @POST("api/automation/stop")
  suspend fun stopAutomation(): Response<RwAutomationResponse>

  @PUT("api/automation/interval")
  suspend fun updateInterval(@Body body: RwIntervalRequest): Response<RwAutomationResponse>

  @PUT("api/automation/settings")
  suspend fun updateSettings(@Body body: RwSettingsRequest): Response<RwAutomationResponse>

  @POST("api/automation/run-now")
  suspend fun runNow(): Response<RwGenericResponse>

  @GET("api/automation/logs")
  suspend fun logs(@Query("limit") limit: Int = 50): Response<AutomationLogsResponse>
}

/**
 * Builds the Railway client for the URL announced by cPanel (/api/app-config).
 * The login token is attached ONLY to requests whose host equals the configured Railway host,
 * so it can never leak to another server.
 */
object RailwayClient {
  private var baseUrl: String? = null
  private var service: RailwayApiService? = null

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  fun configure(url: String?) {
    val clean = url?.trim()?.takeIf { it.isNotEmpty() }?.let { if (it.endsWith("/")) it else "$it/" }
    if (clean != baseUrl) {
      baseUrl = clean
      service = null
    }
  }

  fun isConfigured(): Boolean = !baseUrl.isNullOrBlank() && baseUrl!!.toHttpUrlOrNull() != null

  fun api(): RailwayApiService? {
    val url = baseUrl ?: return null
    val parsed = url.toHttpUrlOrNull() ?: return null
    if (service == null) {
      val allowedHost = parsed.host
      val auth = Interceptor { chain ->
        val req = chain.request()
        val b = req.newBuilder().header("Accept", "application/json")
        if (req.url.host == allowedHost) {
          NetworkClient.currentToken()?.let { b.header("Authorization", "Bearer $it") }
          NetworkClient.currentDeviceId()?.let { b.header("X-Device-Id", it) }
        }
        chain.proceed(b.build())
      }
      val client = OkHttpClient.Builder()
        .addInterceptor(auth)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
      service = Retrofit.Builder()
        .baseUrl(url)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(RailwayApiService::class.java)
    }
    return service
  }
}
