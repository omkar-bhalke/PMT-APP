package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChangePasswordRequest
import com.example.data.model.DeviceResetRequest
import com.example.data.model.ProfileDto
import com.example.data.model.SetupRequest
import com.example.data.model.UpdateProfileRequest
import com.example.data.model.ForgotPasswordRequest
import com.example.data.model.LoginRequest
import com.example.data.model.RegisterRequest
import com.example.data.model.ResetPasswordRequest
import com.example.data.model.UserSettings
import com.example.data.remote.NetworkClient
import com.example.data.repository.FinancialRepository
import com.example.data.security.DeviceSecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {
  val repository = FinancialRepository(application)

  val userSettings: StateFlow<UserSettings> = repository.userSettings.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = UserSettings()
  )

  private val _syncMessage = MutableStateFlow<String?>(null)
  val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  /** One-line message shown on the login screen (e.g. "signed out because the account was opened on another phone"). */
  private val _notice = MutableStateFlow<String?>(null)
  val notice: StateFlow<String?> = _notice.asStateFlow()
  fun showNotice(message: String) { _notice.value = message }
  fun clearNotice() { _notice.value = null }

  val deviceId: String by lazy {
    DeviceSecurityManager.getOrCreateDeviceId(application)
  }

  init {
    // Establish hardware/keystore device identity into network client
    NetworkClient.setDeviceId(deviceId)

    viewModelScope.launch {
      userSettings.collect { settings ->
        NetworkClient.setAuthToken(settings.authToken)
        NetworkClient.setRefreshToken(settings.refreshToken)
        NetworkClient.setBaseUrl(settings.backendUrl)
        if (settings.deviceId.isNotBlank()) {
          NetworkClient.setDeviceId(settings.deviceId)
        } else {
          NetworkClient.setDeviceId(deviceId)
        }
      }
    }

    // Trigger initial sync and cloud data restore if online
    viewModelScope.launch {
      val syncedCount = repository.syncManager.syncPendingTransactions()
      if (syncedCount > 0) {
        _syncMessage.value = "Synced $syncedCount pending records"
      }
      try {
        if (!userSettings.value.authToken.isNullOrBlank()) {
          val restored = repository.restoreFromCloud()
          if (restored > 0) {
            _syncMessage.value = "Your data is up to date"
          }
        }
      } catch (_: Exception) {}
    }
  }

  fun clearSyncMessage() {
    _syncMessage.value = null
  }

  fun clearAuthError() {
    _authError.value = null
  }

  fun register(
    name: String,
    email: String,
    pass: String,
    confirmPass: String,
    onSuccess: (isFirstTime: Boolean) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val app = getApplication<Application>()
        val devFingerprint = DeviceSecurityManager.getDeviceFingerprint(app)
        val devModel = DeviceSecurityManager.getDeviceModel()
        val osVer = DeviceSecurityManager.getOsVersion()

        val request = RegisterRequest(
          name = name.trim(),
          email = email.trim(),
          password = pass,
          confirmPassword = confirmPass,
          deviceId = deviceId,
          deviceFingerprint = devFingerprint,
          deviceModel = devModel,
          osVersion = osVer
        )

        val response = NetworkClient.apiService.register(request)
        if (response.isSuccessful && response.body()?.success == true) {
          val body = response.body()!!
          val user = body.user
          repository.preferenceManager.saveAuthProfile(
            id = user?.id ?: "usr_${System.currentTimeMillis()}",
            email = user?.email ?: email.trim(),
            name = user?.name ?: name.trim(),
            photoUrl = user?.profileImage,
            token = body.token,
            refreshToken = body.refreshToken,
            deviceId = deviceId
          )
          NetworkClient.setAuthToken(body.token)
          NetworkClient.setRefreshToken(body.refreshToken)
          NetworkClient.setDeviceId(deviceId)
          repository.preferenceManager.setOnboardingCompleted(false)
          repository.preferenceManager.saveTrackingMode(null)
          onSuccess(body.isFirstTime)
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: response.body()?.message ?: "Registration failed."
          _authError.value = msg
          onError(msg)
        }
      } catch (e: Exception) {
        val msg = "Could not connect. Please check your internet and try again."
        _authError.value = msg
        onError(msg)
      }
    }
  }

  fun loginWithEmailPassword(
    email: String,
    pass: String,
    rememberMe: Boolean,
    onSuccess: (isFirstTime: Boolean) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val app = getApplication<Application>()
        val devFingerprint = DeviceSecurityManager.getDeviceFingerprint(app)

        val request = LoginRequest(
          email = email.trim(),
          password = pass,
          rememberMe = rememberMe,
          deviceId = deviceId,
          deviceFingerprint = devFingerprint
        )

        val response = NetworkClient.apiService.login(request)
        if (response.isSuccessful && response.body()?.success == true) {
          val body = response.body()!!
          val user = body.user
          repository.preferenceManager.saveAuthProfile(
            id = user?.id ?: "usr_${System.currentTimeMillis()}",
            email = user?.email ?: email.trim(),
            name = user?.name ?: "FinPulse User",
            photoUrl = user?.profileImage,
            token = body.token,
            refreshToken = body.refreshToken,
            deviceId = deviceId
          )
          NetworkClient.setAuthToken(body.token)
          NetworkClient.setRefreshToken(body.refreshToken)
          NetworkClient.setDeviceId(deviceId)
          // The account decides whether first-time setup is needed: finished once = never asked again.
          repository.preferenceManager.setOnboardingCompleted(body.setupCompleted)
          repository.preferenceManager.saveTrackingMode(body.trackingMode)

          // Restore previously saved data from cPanel cloud database (Requirement 5)
          try {
            val restoredCount = repository.restoreFromCloud()
            if (restoredCount > 0) {
              _syncMessage.value = "Your data was restored."
            }
          } catch (_: Exception) {}

          onSuccess(body.isFirstTime)
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: response.body()?.message ?: "Login failed. Check your credentials."
          _authError.value = msg
          onError(msg)
        }
      } catch (e: Exception) {
        val msg = "Could not connect. Please check your internet and try again."
        _authError.value = msg
        onError(msg)
      }
    }
  }

  fun forgotPassword(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.forgotPassword(ForgotPasswordRequest(email = email.trim()))
        if (response.isSuccessful && response.body()?.success == true) {
          onSuccess(response.body()?.message ?: "Password reset instructions sent.")
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Failed to process forgot password request."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  fun resetPassword(
    email: String,
    otp: String,
    newPass: String,
    confirmPass: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.resetPassword(
          ResetPasswordRequest(
            email = email.trim(),
            otp = otp.trim(),
            newPassword = newPass,
            confirmPassword = confirmPass
          )
        )
        if (response.isSuccessful && response.body()?.success == true) {
          onSuccess(response.body()?.message ?: "Password reset successfully.")
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Failed to reset password."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  fun resetDevice(
    email: String,
    pass: String,
    reason: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val response = NetworkClient.apiService.requestDeviceReset(
          DeviceResetRequest(
            email = email.trim(),
            password = pass,
            deviceId = deviceId,
            reason = reason
          )
        )
        if (response.isSuccessful && response.body()?.success == true) {
          onSuccess()
        } else {
          val errJson = response.errorBody()?.string()
          val msg = parseErrorMessage(errJson) ?: "Could not complete this. Please try again."
          onError(msg)
        }
      } catch (e: Exception) {
        onError("Network error: ${e.localizedMessage}")
      }
    }
  }

  fun authenticateUser(id: String, email: String, name: String, photoUrl: String?, token: String?) {
    viewModelScope.launch {
      repository.preferenceManager.saveAuthProfile(
        id = id,
        email = email,
        name = name,
        photoUrl = photoUrl,
        token = token,
        refreshToken = null,
        deviceId = deviceId
      )
      NetworkClient.setAuthToken(token)
    }
  }

  /** First-time choice: AUTO = balance comes from the bank statement (no balance asked); MANUAL = user types it. */
  fun completeSetup(
    mode: String, initialBalance: Double?, bankName: String, dailyLimit: Double?,
    onDone: () -> Unit, onError: (String) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val r = NetworkClient.apiService.completeSetup(SetupRequest(mode, initialBalance, bankName.ifBlank { null }, dailyLimit))
        if (r.isSuccessful && r.body()?.success == true) {
          repository.preferenceManager.saveSetupLocal(mode, initialBalance, bankName, dailyLimit)
          onDone()
        } else onError(parseErrorMessage(r.errorBody()?.string()) ?: "Could not save. Please try again.")
      } catch (e: Exception) {
        onError("Could not connect. Please check your internet and try again.")
      }
    }
  }

  /** Switch later from Settings. MANUAL needs the current balance. */
  fun changeTrackingMode(mode: String, balance: Double?, onDone: () -> Unit, onError: (String) -> Unit) {
    viewModelScope.launch {
      try {
        val r = NetworkClient.apiService.changeTrackingMode(SetupRequest(mode, balance))
        if (r.isSuccessful && r.body()?.success == true) {
          repository.preferenceManager.saveTrackingMode(mode)
          if (mode == "MANUAL" && balance != null) repository.preferenceManager.saveSetupLocal(mode, balance, "", null)
          onDone()
        } else onError(parseErrorMessage(r.errorBody()?.string()) ?: "Could not change. Please try again.")
      } catch (e: Exception) { onError("Could not connect. Please check your internet and try again.") }
    }
  }

  fun loadProfile(onResult: (ProfileDto?) -> Unit) {
    viewModelScope.launch {
      try {
        val r = NetworkClient.apiService.getProfile()
        onResult(if (r.isSuccessful) r.body()?.profile else null)
      } catch (_: Exception) { onResult(null) }
    }
  }

  fun saveProfile(name: String, phone: String, onDone: (String?) -> Unit) {
    viewModelScope.launch {
      try {
        val r = NetworkClient.apiService.updateProfile(UpdateProfileRequest(name.trim(), phone.trim()))
        if (r.isSuccessful && r.body()?.success == true) {
          val s = userSettings.value
          repository.preferenceManager.saveAuthProfile(s.userId, s.userEmail, name.trim(), s.userPhotoUrl, s.authToken, s.refreshToken, s.deviceId.ifBlank { null })
          onDone(null)
        } else onDone(parseErrorMessage(r.errorBody()?.string()) ?: "Could not save.")
      } catch (_: Exception) { onDone("Could not connect. Please try again.") }
    }
  }

  fun changePassword(current: String, new: String, onDone: (String?) -> Unit) {
    viewModelScope.launch {
      try {
        val r = NetworkClient.apiService.changePassword(ChangePasswordRequest(current, new))
        if (r.isSuccessful && r.body()?.success == true) onDone(null)
        else onDone(parseErrorMessage(r.errorBody()?.string()) ?: "Could not change the password.")
      } catch (_: Exception) { onDone("Could not connect. Please try again.") }
    }
  }

  fun logout() {
    viewModelScope.launch {
      try { NetworkClient.apiService.signOutOnServer() } catch (_: Exception) {}
      repository.preferenceManager.clearUserSession()
      NetworkClient.setAuthToken(null)
      NetworkClient.setRefreshToken(null)
    }
  }

  private fun parseErrorMessage(json: String?): String? {
    if (json.isNullOrBlank()) return null
    return try {
      val obj = JSONObject(json)
      obj.optString("message").takeIf { it.isNotBlank() }
    } catch (_: Exception) {
      null
    }
  }
}
