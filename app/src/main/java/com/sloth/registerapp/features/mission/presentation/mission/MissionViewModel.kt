package com.sloth.registerapp.ui.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sloth.registerapp.data.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MissionViewModel @Inject constructor(
    private val missionRepository: MissionRepository
) : ViewModel() {

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
