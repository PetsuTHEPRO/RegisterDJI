package com.sloth.registerapp.features.report.data.manager

import com.sloth.registerapp.features.report.domain.model.FlightReport
import com.sloth.registerapp.features.report.domain.model.FlightReportSession
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Gerencia o ciclo de um relatório de voo em memória:
 * - cria sessão ao iniciar missão
 * - mantém cronômetro (elapsedMs)
 * - finaliza sessão gerando relatório completo
 *
 * Estrutura preparada para evolução: campo extraData aceita novos dados sem quebrar contrato.
 */
class FlightReportManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var timerJob: Job? = null

    private val _currentSession = MutableStateFlow<FlightReportSession?>(null)
    val currentSession: StateFlow<FlightReportSession?> = _currentSession.asStateFlow()

    private val _reports = MutableStateFlow<List<FlightReport>>(emptyList())
    val reports: StateFlow<List<FlightReport>> = _reports.asStateFlow()

    /**
     * Inicia uma nova sessão de relatório.
     * Se já existir uma sessão ativa, ela será substituída.
     */
    fun startReport(
        missionName: String,
        aircraftName: String,
        startedAtMs: Long = System.currentTimeMillis(),
        extraData: Map<String, String> = emptyMap()
    ) {
        stopTimer()
        val safeNow = System.currentTimeMillis()
        _currentSession.value = FlightReportSession(
            id = UUID.randomUUID().toString(),
            missionName = missionName,
            aircraftName = aircraftName,
            createdAtMs = safeNow,
            startedAtMs = startedAtMs,
            elapsedMs = max(0L, safeNow - startedAtMs),
            extraData = extraData
        )
        startTimer()
    }

    /**
     * Atualiza dados extras da sessão ativa.
     * Use para anexar novos campos sem alterar modelo principal.
     */
    fun updateExtraData(extraData: Map<String, String>) {
        val session = _currentSession.value ?: return
        _currentSession.value = session.copy(
            extraData = session.extraData + extraData
        )
    }

    /**
     * Finaliza a sessão ativa e salva relatório.
     */
    fun finishReport(
        finalObservation: String? = null,
        endedAtMs: Long = System.currentTimeMillis()
    ): FlightReport? {
        val session = _currentSession.value ?: return null
        stopTimer()

        val duration = max(0L, endedAtMs - session.startedAtMs)
        val report = FlightReport(
            id = session.id,
            missionName = session.missionName,
            aircraftName = session.aircraftName,
            createdAtMs = session.createdAtMs,
            startedAtMs = session.startedAtMs,
            endedAtMs = endedAtMs,
            durationMs = duration,
            finalObservation = finalObservation,
            extraData = session.extraData
        )

        _reports.value = listOf(report) + _reports.value
        _currentSession.value = null
        return report
    }

    /**
     * Cancela a sessão ativa sem gerar relatório.
     */
    fun cancelCurrentReport() {
        stopTimer()
        _currentSession.value = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val session = _currentSession.value ?: break
                val now = System.currentTimeMillis()
                _currentSession.value = session.copy(
                    elapsedMs = max(0L, now - session.startedAtMs)
                )
                delay(1000)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
}

