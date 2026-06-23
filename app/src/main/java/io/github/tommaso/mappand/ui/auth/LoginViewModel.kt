package io.github.tommaso.mappand.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.data.auth.AuthDataStore
import io.github.tommaso.mappand.data.pcloud.TwoFactorRequired
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class NeedTotp(val tfaToken: String) : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(private val app: MappandApp) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    var selectedHost = AuthDataStore.EU_HOST

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                app.authDataStore.setHost(selectedHost)
                val token = app.pCloudClient.loginWithPassword(email, password)
                app.authDataStore.saveToken(token)
                _state.value = LoginState.Success
            } catch (e: TwoFactorRequired) {
                _state.value = LoginState.NeedTotp(e.tfaToken)
            } catch (e: Exception) {
                _state.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun submitTotp(tfaToken: String, code: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val token = app.pCloudClient.loginWithTfa(tfaToken, code)
                app.authDataStore.saveToken(token)
                _state.value = LoginState.Success
            } catch (e: Exception) {
                _state.value = LoginState.Error(e.message ?: "TOTP failed")
            }
        }
    }
}
