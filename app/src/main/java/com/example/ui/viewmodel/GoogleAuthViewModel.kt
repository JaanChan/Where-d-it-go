package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.util.findActivity
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class GoogleAuthUiState(
    val currentUser: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val isSignedIn: Boolean
        get() = currentUser != null
}

class GoogleAuthViewModel(
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GoogleAuthUiState(currentUser = auth.currentUser)
    )
    val uiState: StateFlow<GoogleAuthUiState> = _uiState.asStateFlow()

    private var autoSignInJob: Job? = null
    private var hasAttemptedAutoSignIn: Boolean = false

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _uiState.update { state ->
            state.copy(
                currentUser = user,
                isLoading = if (user != null) false else state.isLoading,
                errorMessage = if (user != null) null else state.errorMessage
            )
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager = CredentialManager.create(context),
        onAuthSuccess: () -> Unit = {},
        onUnauthenticated: () -> Unit = {}
    ) {
        if (auth.currentUser != null) {
            _uiState.update { it.copy(currentUser = auth.currentUser, isLoading = false, errorMessage = null) }
            onAuthSuccess()
            return
        }
        if (hasAttemptedAutoSignIn) {
            return
        }
        hasAttemptedAutoSignIn = true

        val clientId = resolveWebClientId(context)
        if (clientId == null) {
            onUnauthenticated()
            return
        }

        val activityContext = context.findActivity() ?: context
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        // Single option per GetCredentialRequest for API 36 bottom-sheet responsiveness
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        autoSignInJob?.cancel()
        autoSignInJob = viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activityContext, request)
                val signedIn = handleCredentialResult(result.credential)
                if (signedIn) {
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Silent auto sign-in not available: ${e.message}")
                onUnauthenticated()
            }
        }
    }

    fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager = CredentialManager.create(context),
        onAuthSuccess: () -> Unit = {},
        onAuthError: (String) -> Unit = {},
        onAuthCancelled: () -> Unit = {}
    ) {
        // Cancel any in-flight silent auto-sign-in job before interactive sign-in
        autoSignInJob?.cancel()

        val clientId = resolveWebClientId(context)
        if (clientId == null) {
            val msg = "Google Sign-In configuration missing: default_web_client_id not found"
            _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            onAuthError(msg)
            return
        }

        val activityContext = context.findActivity()
        if (activityContext == null) {
            val msg = "Unable to launch Google Sign-In: active Activity context is required."
            _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            onAuthError(msg)
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Interactive Sign-In: strictly one GetSignInWithGoogleOption per request
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        viewModelScope.launch {
            try {
                val result = credentialManager.getCredential(activityContext, request)
                val signedIn = handleCredentialResult(result.credential)
                if (signedIn) {
                    _uiState.update {
                        it.copy(
                            currentUser = auth.currentUser,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                    onAuthSuccess()
                } else {
                    val msg = "Unexpected credential type returned from Credential Manager."
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                    onAuthError(msg)
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In cancelled or dismissed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credentials available on device: ${e.message}", e)
                val msg = "No Google account found on this device. Please add a Google account in Settings and try again."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                onAuthError(msg)
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                val msg = e.localizedMessage ?: "Sign in failed. Please try again."
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                onAuthError(msg)
            }
        }
    }

    private suspend fun handleCredentialResult(credential: Credential): Boolean {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(firebaseCredential).await()
            return true
        }
        return false
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager = CredentialManager.create(context),
        onSignOutComplete: () -> Unit = {}
    ) {
        autoSignInJob?.cancel()
        hasAttemptedAutoSignIn = false
        auth.signOut()
        _uiState.update {
            it.copy(
                currentUser = null,
                isLoading = false,
                errorMessage = null
            )
        }
        viewModelScope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun resolveWebClientId(context: Context): String? {
        return try {
            context.getString(R.string.default_web_client_id).trim().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read default_web_client_id", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSignInJob?.cancel()
        auth.removeAuthStateListener(authStateListener)
    }

    companion object {
        private const val TAG = "Auth"

        fun factory(auth: FirebaseAuth = Firebase.auth): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GoogleAuthViewModel(auth = auth)
            }
        }
    }
}
