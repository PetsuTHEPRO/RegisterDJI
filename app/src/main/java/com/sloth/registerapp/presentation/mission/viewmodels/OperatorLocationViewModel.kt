package com.sloth.registerapp.presentation.mission.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.geojson.Point
import com.sloth.registerapp.features.mission.data.location.OperatorLocationRepositoryImpl
import com.sloth.registerapp.features.mission.domain.repository.OperatorLocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperatorLocationUiState(
    val location: Point? = null,
    val errorMessage: String? = null,
    val hasPermission: Boolean = false
)

class OperatorLocationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: OperatorLocationRepository = OperatorLocationRepositoryImpl(application)

    private val _uiState = MutableStateFlow(OperatorLocationUiState())
    val uiState: StateFlow<OperatorLocationUiState> = _uiState.asStateFlow()

    private var updatesJob: Job? = null

    fun setPermissionGranted(granted: Boolean) {
        _uiState.update { it.copy(hasPermission = granted) }

        if (!granted) {
            updatesJob?.cancel()
            updatesJob = null
            _uiState.update { it.copy(location = null, errorMessage = "Permissão de GPS não concedida") }
            return
        }

        if (updatesJob == null) {
            startUpdates()
        }
    }

    private fun startUpdates() {
        updatesJob?.cancel()
        updatesJob = viewModelScope.launch {
            try {
                val last = repository.getLastKnownLocation()
                if (last != null) {
                    _uiState.update { it.copy(location = last, errorMessage = null) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Falha ao obter GPS") }
            }

            repository.locationUpdates()
                .catch {
                    _uiState.update { it.copy(errorMessage = "Falha ao obter GPS") }
                }
                .collect { point ->
                    _uiState.update { it.copy(location = point, errorMessage = null) }
                }
        }
    }
}
