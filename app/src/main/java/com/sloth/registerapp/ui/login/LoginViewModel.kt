package com.sloth.registerapp.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.data.repository.TokenRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenRepository = TokenRepository(application)

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onSaveTokenClicked(token: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            if (token.isNotBlank()) {
                tokenRepository.saveToken(token)
                _uiState.value = LoginUiState.Success
            } else {
                _uiState.value = LoginUiState.Error("Token cannot be empty")
            }
        }
    }
}

