package com.sloth.registerapp.data.drone

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class MissionManager(
    private val droneController: DroneControllerManager
) {

    private val _missionStatus = MutableStateFlow<MissionStatus>(MissionStatus.Idle)
    val missionStatus: StateFlow<MissionStatus> = _missionStatus

    private val scope = CoroutineScope(Dispatchers.Main)
    private var isMissionRunning = false

    // ========== MISSÕES GEOMÉTRICAS ==========

    /**
     * Executa missão em formato de RETÂNGULO
     * @param width Largura em metros
     * @param height Altura em metros
     * @param speed Velocidade de movimento
     */
    fun executeRectangleMission(
        width: Float = 5f,
        height: Float = 3f,
        speed: Float = 2f
    ) {
        if (isMissionRunning) {
            Log.w(TAG, "⚠️ Missão já está em execução")
            return
        }

        scope.launch {
            isMissionRunning = true
            _missionStatus.value = MissionStatus.Running("Retângulo")
            Log.d(TAG, "🔷 Iniciando missão RETÂNGULO (${width}m x ${height}m)")

            try {
                // Passo 1: Decolar
                _missionStatus.value = MissionStatus.Running("Decolando...")
                droneController.takeOff()
                delay(5000) // Aguarda decolagem

                // Passo 2: Subir para altitude segura
                _missionStatus.value = MissionStatus.Running("Subindo...")
                droneController.moveUp(2f)
                delay(3000)

                // Passo 3: Desenhar retângulo
                _missionStatus.value = MissionStatus.Running("Lado 1/4")
                moveForDuration(droneController::moveForward, (width / speed * 1000).toLong())

                _missionStatus.value = MissionStatus.Running("Lado 2/4")
                rotateAndMove(90f, droneController::moveForward, (height / speed * 1000).toLong())

                _missionStatus.value = MissionStatus.Running("Lado 3/4")
                rotateAndMove(90f, droneController::moveForward, (width / speed * 1000).toLong())

                _missionStatus.value = MissionStatus.Running("Lado 4/4")
                rotateAndMove(90f, droneController::moveForward, (height / speed * 1000).toLong())

                // Finaliza rotação
                droneController.rotateRight(90f)
                delay(1000)
                droneController.stopMovement()

                // Passo 4: Pousar
                _missionStatus.value = MissionStatus.Running("Pousando...")
                droneController.land()
                delay(5000)

                _missionStatus.value = MissionStatus.Completed("Retângulo concluído!")
                Log.d(TAG, "✅ Missão RETÂNGULO concluída")

            } catch (e: Exception) {
                _missionStatus.value = MissionStatus.Failed("Erro: ${e.message}")
                Log.e(TAG, "❌ Erro na missão: ${e.message}")
            } finally {
                isMissionRunning = false
            }
        }
    }

    /**
     * Executa missão em formato de TRIÂNGULO
     * @param sideLength Tamanho do lado em metros
     * @param speed Velocidade de movimento
     */
    fun executeTriangleMission(
        sideLength: Float = 5f,
        speed: Float = 2f
    ) {
        if (isMissionRunning) {
            Log.w(TAG, "⚠️ Missão já está em execução")
            return
        }

        scope.launch {
            isMissionRunning = true
            _missionStatus.value = MissionStatus.Running("Triângulo")
            Log.d(TAG, "🔺 Iniciando missão TRIÂNGULO (${sideLength}m por lado)")

            try {
                // Decolar
                _missionStatus.value = MissionStatus.Running("Decolando...")
                droneController.takeOff()
                delay(5000)

                // Subir
                _missionStatus.value = MissionStatus.Running("Subindo...")
                droneController.moveUp(2f)
                delay(3000)

                // Desenhar triângulo (3 lados, 120° cada)
                for (i in 1..3) {
                    _missionStatus.value = MissionStatus.Running("Lado $i/3")
                    moveForDuration(droneController::moveForward, (sideLength / speed * 1000).toLong())

                    if (i < 3) {
                        rotateAndMove(120f, droneController::stopMovement, 500)
                    }
                }

                // Pousar
                _missionStatus.value = MissionStatus.Running("Pousando...")
                droneController.land()
                delay(5000)

                _missionStatus.value = MissionStatus.Completed("Triângulo concluído!")
                Log.d(TAG, "✅ Missão TRIÂNGULO concluída")

            } catch (e: Exception) {
                _missionStatus.value = MissionStatus.Failed("Erro: ${e.message}")
                Log.e(TAG, "❌ Erro na missão: ${e.message}")
            } finally {
                isMissionRunning = false
            }
        }
    }

    /**
     * Executa missão em formato de CÍRCULO
     * @param radius Raio em metros
     * @param points Número de pontos do círculo (mais = mais suave)
     */
    fun executeCircleMission(
        radius: Float = 3f,
        points: Int = 12,
        speed: Float = 1.5f
    ) {
        if (isMissionRunning) {
            Log.w(TAG, "⚠️ Missão já está em execução")
            return
        }

        scope.launch {
            isMissionRunning = true
            _missionStatus.value = MissionStatus.Running("Círculo")
            Log.d(TAG, "⭕ Iniciando missão CÍRCULO (raio ${radius}m, $points pontos)")

            try {
                // Decolar
                _missionStatus.value = MissionStatus.Running("Decolando...")
                droneController.takeOff()
                delay(5000)

                // Subir
                _missionStatus.value = MissionStatus.Running("Subindo...")
                droneController.moveUp(2f)
                delay(3000)

                // Desenhar círculo
                val angleStep = 360f / points
                val segmentLength = (2 * Math.PI * radius / points).toFloat()

                for (i in 1..points) {
                    _missionStatus.value = MissionStatus.Running("Segmento $i/$points")

                    // Move para frente
                    moveForDuration(droneController::moveForward, (segmentLength / speed * 1000).toLong())

                    // Rotaciona
                    droneController.rotateRight(angleStep)
                    delay(800)
                }

                droneController.stopMovement()

                // Pousar
                _missionStatus.value = MissionStatus.Running("Pousando...")
                droneController.land()
                delay(5000)

                _missionStatus.value = MissionStatus.Completed("Círculo concluído!")
                Log.d(TAG, "✅ Missão CÍRCULO concluída")

            } catch (e: Exception) {
                _missionStatus.value = MissionStatus.Failed("Erro: ${e.message}")
                Log.e(TAG, "❌ Erro na missão: ${e.message}")
            } finally {
                isMissionRunning = false
            }
        }
    }

    /**
     * Executa missão customizada com waypoints
     */
    fun executeCustomMission(waypoints: List<Pair<Float, Float>>) {
        if (isMissionRunning) {
            Log.w(TAG, "⚠️ Missão já está em execução")
            return
        }

        scope.launch {
            isMissionRunning = true
            _missionStatus.value = MissionStatus.Running("Missão Customizada")
            Log.d(TAG, "📍 Iniciando missão CUSTOMIZADA (${waypoints.size} waypoints)")

            try {
                // Decolar
                droneController.takeOff()
                delay(5000)

                // Subir
                droneController.moveUp(2f)
                delay(3000)

                // Percorrer waypoints
                waypoints.forEachIndexed { index, (x, y) ->
                    _missionStatus.value = MissionStatus.Running("Waypoint ${index + 1}/${waypoints.size}")

                    // Move X
                    if (x > 0) {
                        moveForDuration(droneController::moveRight, (x * 500).toLong())
                    } else if (x < 0) {
                        moveForDuration(droneController::moveLeft, (-x * 500).toLong())
                    }

                    // Move Y
                    if (y > 0) {
                        moveForDuration(droneController::moveForward, (y * 500).toLong())
                    } else if (y < 0) {
                        moveForDuration(droneController::moveBackward, (-y * 500).toLong())
                    }

                    delay(1000)
                }

                // Pousar
                droneController.land()
                delay(5000)

                _missionStatus.value = MissionStatus.Completed("Missão customizada concluída!")
                Log.d(TAG, "✅ Missão CUSTOMIZADA concluída")

            } catch (e: Exception) {
                _missionStatus.value = MissionStatus.Failed("Erro: ${e.message}")
            } finally {
                isMissionRunning = false
            }
        }
    }

    // ========== CONTROLE DE MISSÃO ==========

    fun pauseMission() {
        if (isMissionRunning) {
            droneController.stopMovement()
            _missionStatus.value = MissionStatus.Paused
            Log.d(TAG, "⏸️ Missão pausada")
        }
    }

    fun cancelMission() {
        if (isMissionRunning) {
            isMissionRunning = false
            droneController.stopMovement()
            droneController.land()
            _missionStatus.value = MissionStatus.Cancelled
            Log.d(TAG, "❌ Missão cancelada")
        }
    }

    // ========== HELPERS ==========

    private suspend fun moveForDuration(action: (Float) -> Unit, durationMs: Long) {
        action(2f) // Inicia movimento
        delay(durationMs)
        droneController.stopMovement()
        delay(500)
    }

    private suspend fun rotateAndMove(angle: Float, action: () -> Unit, durationMs: Long) {
        droneController.rotateRight(angle)
        delay(1500) // Tempo para rotação
        action()
        delay(durationMs)
    }

    companion object {
        private const val TAG = "MissionManager"
    }
}

// ========== STATUS DA MISSÃO ==========

sealed class MissionStatus {
    object Idle : MissionStatus()
    data class Running(val step: String) : MissionStatus()
    object Paused : MissionStatus()
    data class Completed(val message: String) : MissionStatus()
    data class Failed(val error: String) : MissionStatus()
    object Cancelled : MissionStatus()
}
