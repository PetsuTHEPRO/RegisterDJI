package com.sloth.registerapp.ui.mission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.data.network.RetrofitClient
import com.sloth.registerapp.data.repository.MissionRepository
import com.sloth.registerapp.data.repository.TokenRepository
import com.sloth.registerapp.model.Mission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class MissionViewModel(application: Application) : AndroidViewModel(application) {

    private val missionRepository = MissionRepository(
        RetrofitClient.getInstance(application),
        TokenRepository.getInstance(application)
    )

    private val _uiState = MutableStateFlow<MissionUiState>(MissionUiState.Idle)
    val uiState: StateFlow<MissionUiState> = _uiState

    fun fetchMissions() {
        viewModelScope.launch {
            _uiState.value = MissionUiState.Loading
            val result = missionRepository.getMissions()
            result.onSuccess { missions ->
                _uiState.value = MissionUiState.Success(missions)
                println("Missions fetched successfully: $missions")
            }.onFailure { error ->
                if (error is HttpException && error.code() == 401) {
                    _uiState.value = MissionUiState.Unauthorized
                } else {
                    _uiState.value = MissionUiState.Error(error.message ?: "Unknown error")
                }
                println("Error fetching missions: ${error.message}")
            }
        }
    }
}
