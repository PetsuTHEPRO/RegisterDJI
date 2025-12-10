package com.sloth.registerapp.presentation.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sloth.registerapp.data.repository.TokenRepository
import kotlinx.coroutines.flow.map

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenRepository = TokenRepository(application)

    val isLoggedIn = tokenRepository.token.map { !it.isNullOrBlank() }
}
