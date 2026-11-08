package com.example.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Raised when the account was opened on another phone (or disabled): the app signs out and tells the user. */
object SessionEvents {
  private val _signedOut = MutableSharedFlow<String>(extraBufferCapacity = 4)
  val signedOut: SharedFlow<String> = _signedOut.asSharedFlow()
  fun emit(message: String) { _signedOut.tryEmit(message) }
}
