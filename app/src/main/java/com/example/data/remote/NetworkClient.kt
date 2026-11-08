package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {
  private var cachedToken: String? = null
  private var cachedRefreshToken: String? = null
  private var cachedDeviceId: String? = null
  private var currentBaseUrl: String = "https://radhewins.buzz/"
  private var _apiService: FinPulseApiService? = null

  fun setAuthToken(token: String?) {
    cachedToken = token
  }

  fun setRefreshToken(refreshToken: String?) {
    cachedRefreshToken = refreshToken
  }

  fun setDeviceId(deviceId: String?) {
    cachedDeviceId = deviceId
  }

  fun currentToken(): String? = cachedToken
  fun currentDeviceId(): String? = cachedDeviceId
  fun currentRefreshToken(): String? = cachedRefreshToken

  fun setBaseUrl(url: String) {
    val formatted = if (url.endsWith("/")) url else "$url/"
    if (formatted != currentBaseUrl) {
      currentBaseUrl = formatted
      _apiService = null
    }
  }

  private val authInterceptor = Interceptor { chain ->
    val original = chain.request()
    val builder = original.newBuilder()

    cachedToken?.let { token ->
      builder.header("Authorization", "Bearer $token")
    }

    cachedDeviceId?.let { devId ->
      builder.header("X-Device-Id", devId)
    }

    builder.header("Accept", "application/json")
    chain.proceed(builder.build())
  }

  /** Detects "signed in on another phone" / "account disabled" and tells the whole app once. */
  private val sessionInterceptor = Interceptor { chain ->
    val response = chain.proceed(chain.request())
    if ((response.code == 401 || response.code == 403) && !chain.request().url.encodedPath.contains("auth/login")) {
      val snippet = try { response.peekBody(4096).string() } catch (_: Exception) { "" }
      when {
        snippet.contains("SESSION_REPLACED") -> SessionEvents.emit("You were signed out because your account was opened on another phone.")
        snippet.contains("ACCOUNT_BLOCKED") -> SessionEvents.emit("This account has been disabled. Please contact support.")
      }
    }
    response
  }

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
  }

  private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(authInterceptor)
    .addInterceptor(sessionInterceptor)
    .addInterceptor(loggingInterceptor)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private val moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  val apiService: FinPulseApiService
    get() {
      if (_apiService == null) {
        _apiService = Retrofit.Builder()
          .baseUrl(currentBaseUrl)
          .client(okHttpClient)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()
          .create(FinPulseApiService::class.java)
      }
      return _apiService!!
    }
}
