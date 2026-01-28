package com.sloth.registerapp.features.mission.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.RetrofitClient
import com.sloth.registerapp.features.mission.data.repository.MissionRepositoryImpl
import com.sloth.registerapp.features.mission.domain.model.Mission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

private const val TAG = "MissionViewModel"

class MissionViewModel(application: Application) : AndroidViewModel(application) {

    private val missionRepository = MissionRepositoryImpl(
        com.sloth.registerapp.core.network.RetrofitClient.getInstance(application),
        com.sloth.registerapp.core.auth.TokenRepository.getInstance(application)
    )

    private val _uiState = MutableStateFlow<MissionUiState>(MissionUiState.Idle)
    val uiState: StateFlow<MissionUiState> = _uiState

    fun fetchMissions() {
        viewModelScope.launch {
            _uiState.value = MissionUiState.Loading
            val result = missionRepository.getMissions()
            result.onSuccess { missions ->
                _uiState.value = MissionUiState.Success(missions)
                Log.d(TAG, "Missions fetched successfully: ${missions.size} missions")
            }.onFailure { error ->
                if (error is HttpException && error.code() == 401) {
                    _uiState.value = MissionUiState.Unauthorized
                    Log.w(TAG, "Unauthorized access when fetching missions")
                } else {
                    _uiState.value = MissionUiState.Error(error.message ?: "Unknown error")
                    Log.e(TAG, "Error fetching missions", error)
                }
            }
        }
    }
}