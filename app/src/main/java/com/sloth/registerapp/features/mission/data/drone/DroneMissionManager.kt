package com.sloth.registerapp.features.mission.data.drone

import android.util.Log
import com.sloth.registerapp.core.constants.DroneConstants
import com.sloth.registerapp.features.mission.data.model.ServerMission
import com.sloth.registerapp.features.mission.data.model.Waypoint as ServerWaypoint
import dji.common.error.DJIError
import dji.common.mission.waypoint.*
import dji.common.product.Model
import dji.sdk.mission.MissionControl
import dji.sdk.mission.waypoint.WaypointMissionOperator
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// O enum de estado interno permanece o mesmo
enum class MissionState {
    IDLE,                   // nada conectado / nada ativo
    PREPARING,              // validações iniciais
    DOWNLOADING,            // download em andamento
    DOWNLOAD_FINISHED,      // download concluído
    UPLOADING,              // upload em andamento
    READY_TO_EXECUTE,       // missão validada e pronta
    EXECUTING,              // missão em execução
    EXECUTION_PAUSED,       // missão pausada
    EXECUTION_STOPPED,      // interrompida manualmente
    FINISHED,               // missão finalizada com sucesso
    ERROR                   // erro irrecuperável
}

class DroneMissionManager(
    private val djiConnectionHelper: com.sloth.registerapp.features.mission.data.sdk.DJIConnectionHelper,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val SUPPORTED_DRONE_MODELS = listOf(
        "Mavic Pro",
        "Mavic 2 Pro",
        "Mavic 2 Zoom",
        "Mavic 2 Enterprise",
        "Phantom 4 Pro",
        "Phantom 4 RTK",
        "Phantom 3 Professional",
        "Phantom 3 Advanced",
        "Inspire 1",
        "Inspire 2"
    )

    companion object {
        private const val TAG = "DroneMissionManager"
        private const val UPLOAD_TIMEOUT_MS = 30000L  // 30 segundos
        private const val START_TIMEOUT_MS = 10000L   // 10 segundos
        private const val STOP_TIMEOUT_MS = 10000L    // 10 segundos
        private const val PAUSE_TIMEOUT_MS = 5000L    // 5 segundos
        private const val RESUME_TIMEOUT_MS = 5000L   // 5 segundos
        private const val MIN_AUTO_FLIGHT_SPEED = 0.5f
        private const val MAX_AUTO_FLIGHT_SPEED = 20f
        private const val MIN_MAX_FLIGHT_SPEED = 0.5f
        private const val MAX_FLIGHT_SPEED_LIMIT = 30f
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    private fun getWaypointMissionOperator(): WaypointMissionOperator? =
        MissionControl.getInstance()?.waypointMissionOperator

    private val _missionState = MutableStateFlow(MissionState.IDLE)
    val missionState = _missionState.asStateFlow()

    // Listener para rastrear eventos do operador
    private val missionListener = MissionListenerImpl()
    private var listenerAdded = false

    init {
        initializeConnectionMonitoring()
        addMissionListener()
    }

    private fun initializeConnectionMonitoring() {
        scope.launch {
            djiConnectionHelper.product.collect { product ->
                when {
                    product == null -> {
                        Log.d(TAG, "✈️ Drone desconectado")
                        _missionState.value = MissionState.IDLE
                        // Remover listener se estiver adicionado
                        if (listenerAdded) {
                            getWaypointMissionOperator()?.removeListener(missionListener)
                            listenerAdded = false
                            Log.d(TAG, "ℹ️ Mission Listener removido (drone desconectado)")
                        }
                    }

                    product.model == null -> {
                        Log.e(TAG, "❌ Produto conectado sem modelo definido")
                        _missionState.value = MissionState.ERROR
                    }

                    !isSupported(product.model) -> {
                        Log.e(
                            TAG,
                            "❌ Drone não suportado: ${product.model.displayName}"
                        )
                        _missionState.value = MissionState.ERROR
                    }

                    else -> {
                        Log.d(TAG, "✅ Drone conectado: ${product.model.displayName}")
                        if (_missionState.value == MissionState.ERROR) {
                            _missionState.value = MissionState.IDLE
                        }
                        // Tentar adicionar listener quando o produto conectar
                        addMissionListener()
                    }
                }
            }
        }
    }

    private fun addMissionListener() {
        val operator = getWaypointMissionOperator()
        if (operator == null) {
            Log.w(TAG, "⚠️ WaypointMissionOperator ainda não disponível (SDK inicializando?)")
            return
        }

        if (!listenerAdded) {
            operator.addListener(missionListener)
            listenerAdded = true
            Log.d(TAG, "✅ Mission Listener adicionado")
        }
    }

    /**
     * Verifica se o drone está realmente conectado.
     * @return true se conectado, false caso contrário
     */
    private fun isDroneConnected(): Boolean {
        val product = djiConnectionHelper.getProductInstance()
        return product != null
    }

    /**
     * Valida se o drone está conectado antes de executar operações.
     * @throws DJIMissionException se o drone não estiver conectado
     */
    private fun validateDroneConnection() {
        if (!isDroneConnected()) {
            val product = djiConnectionHelper.getProductInstance()
            Log.e(TAG, "❌ DRONE NÃO CONECTADO!")
            Log.e(TAG, "  📱 Product: ${product?.model?.displayName ?: "NULL"}")
            Log.e(TAG, "  ⚠️ Não é possível executar operações sem o drone conectado")
            throw DJIMissionException(
                "Drone não está conectado. Conecte o drone e tente novamente."
            )
        }
    }

    /**
     * DIAGNÓSTICO: Verifica se o método setHomeLocationUsingAircraftCurrentLocation existe
     * e o estado atual do Home Point no drone.
     * USE ISSO PARA DEBUG!
     */
    suspend fun diagnosticHomePoint() {
        Log.d(TAG, "🔍 === DIAGNÓSTICO DE HOME POINT ===")
        
        try {
            val product = djiConnectionHelper.getProductInstance() as? dji.sdk.products.Aircraft
            
            if (product == null) {
                Log.e(TAG, "❌ Drone não conectado (product == null)")
                return
            }
            
            val flightController = product.flightController
            if (flightController == null) {
                Log.e(TAG, "❌ FlightController não disponível")
                return
            }
            
            Log.d(TAG, "✅ Drone conectado: ${product.model?.displayName ?: "Desconhecido"}")
            
            // 1. Verificar se o método existe via reflexão
            val hasMethod = try {
                val callbackClass = Class.forName("dji.common.util.CommonCallbacks\$CompletionCallback")
                val method = flightController.javaClass.getMethod(
                    "setHomeLocationUsingAircraftCurrentLocation",
                    callbackClass
                )
                Log.d(TAG, "✅ MÉTODO EXISTE: setHomeLocationUsingAircraftCurrentLocation")
                true
            } catch (e: NoSuchMethodException) {
                Log.e(TAG, "❌ MÉTODO NÃO EXISTE: setHomeLocationUsingAircraftCurrentLocation")
                Log.e(TAG, "   Métodos disponíveis com 'Home' no nome:")
                flightController.javaClass.methods
                    .filter { it.name.contains("Home", ignoreCase = true) }
                    .forEach { Log.e(TAG, "   - ${it.name}") }
                false
            } catch (e: ClassNotFoundException) {
                Log.e(TAG, "❌ Classe de callback não encontrada")
                false
            }
            
            // 2. Verificar estado do Home Point
            val state = try { flightController.state } catch (e: Exception) { null }
            val isHomeSet = try { state?.isHomeLocationSet ?: false } catch (e: Exception) { false }
            val satellites = try { state?.satelliteCount ?: 0 } catch (e: Exception) { 0 }
            
            Log.d(TAG, "📍 Estado Home Point: ${if (isHomeSet) "✅ SETADO" else "❌ NÃO SETADO"}")
            Log.d(TAG, "📡 Satélites: $satellites (mínimo recomendado: 10)")
            
            // 3. Verificar se drone está no ar
            val isFlying = try { state?.isFlying ?: false } catch (e: Exception) { false }
            val altitude = try {
                val altitudeField = state?.javaClass?.getDeclaredField("altitude")
                altitudeField?.isAccessible = true
                (altitudeField?.get(state) as? Float)?.toDouble() ?: 0.0
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível obter altitude: ${e.message}")
                0.0
            }
            
            Log.d(TAG, "🚁 Drone no ar: ${if (isFlying) "SIM ❌ (deve estar no chão)" else "NÃO ✅ (correto)"}")
            Log.d(TAG, "📏 Altitude: ${String.format("%.2f", altitude)}m")
            
            // 4. Status da bateria
            val batteryPercent = try {
                val battery = product.battery
                if (battery != null) {
                    val percentField = battery.javaClass.getDeclaredField("chargeRemainingInPercent")
                    percentField.isAccessible = true
                    percentField.getInt(battery)
                } else {
                    -1
                }
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível obter bateria: ${e.message}")
                -1
            }
            Log.d(TAG, "🔋 Bateria: ${if (batteryPercent >= 0) "$batteryPercent%" else "N/A"}")
            
            Log.d(TAG, "🔍 === FIM DIAGNÓSTICO ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao fazer diagnóstico: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Faz diagnóstico do estado atual do drone antes de carregar missão.
     * Ajuda a identificar por que uma missão não pode ser carregada.
     */
    private fun diagnosticoDroneState() {
        try {
            val product = djiConnectionHelper.getProductInstance()
            
            Log.d(TAG, "🔍 === DIAGNÓSTICO DO DRONE ===")
            
            // Verificar conexão REAL do drone
            if (product != null) {
                Log.d(TAG, "  ✅ Drone: CONECTADO")
                Log.d(TAG, "  📱 Modelo: ${product.model?.displayName ?: "Desconhecido"}")
                Log.d(TAG, "  🆔 Firmware: ${product.firmwarePackageVersion ?: "N/A"}")
            } else {
                Log.e(TAG, "  ❌ Drone: NÃO CONECTADO")
                Log.e(TAG, "  💡 CAUSA: Product é NULL")
            }
            
            // Verificar operador (sempre está disponível se SDK foi inicializado)
            if (getWaypointMissionOperator() == null) {
                Log.w(TAG, "  ⚠️ WaypointMissionOperator: NÃO disponível (SDK não inicializado)")
            } else {
                Log.d(TAG, "  ℹ️ WaypointMissionOperator: Disponível (SDK inicializado)")
            }
            
            // Verificar estado da missão
            Log.d(TAG, "  🎯 Estado da missão: ${_missionState.value}")
            Log.d(TAG, "  📡 Listener adicionado: $listenerAdded")
            
            Log.d(TAG, "🔍 === FIM DIAGNÓSTICO ===")
            
            if (product == null) {
                Log.e(TAG, "")
                Log.e(TAG, "❌ AÇÃO NECESSÁRIA:")
                Log.e(TAG, "   1. Ligue o DRONE")
                Log.e(TAG, "   2. Ligue o CONTROLE REMOTO")
                Log.e(TAG, "   3. Conecte o cabo USB ao dispositivo")
                Log.e(TAG, "   4. Aguarde a conexão ser estabelecida")
                Log.e(TAG, "")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao fazer diagnóstico: ${e.message}")
        }
    }

    /**
     * Prepara, valida, carrega e faz upload de uma missão para o drone.
     * Inclui retry automático em caso de falha de upload.
     * @throws IllegalArgumentException se os parâmetros forem inválidos
     * @throws DJIMissionException se houver erro no upload ou se drone não estiver conectado
     */
    suspend fun prepareAndUploadMission(missionData: ServerMission) {
        _missionState.value = MissionState.PREPARING

        try {
            Log.i(TAG, "🚀 Iniciando preparação de missão: ${missionData.name}")
            
            // VALIDAÇÃO CRÍTICA: Verificar se o drone está conectado ANTES de tudo
            validateDroneConnection()
            
            // Verificar operador após validação de conexão
            val operator = getWaypointMissionOperator() ?: throw DJIMissionException(
                "WaypointMissionOperator não está disponível. Reinicie o app."
            )
            
            // Fazer diagnóstico do drone
            diagnosticoDroneState()

            // 1. Validar e filtrar waypoints
            Log.d(TAG, "📍 Validando ${missionData.waypoints.size} waypoints...")
            val waypointList = validateAndFilterWaypoints(missionData.waypoints)
            Log.d(TAG, "✅ ${waypointList.size} waypoints válidos após filtragem")

            // 2. Validar parâmetros de voo
            Log.d(TAG, "⚙️ Validando parâmetros de voo...")
            validateFlightParameters(
                missionData.auto_flight_speed.toFloat(),
                missionData.max_flight_speed.toFloat()
            )
            Log.d(TAG, "✅ Parâmetros de voo validados")

            // 3. Construir missão
            Log.d(TAG, "🔧 Construindo missão DJI...")
            val mission = buildWaypointMission(missionData, waypointList)
            Log.d(TAG, "✅ Missão construída: ${mission.waypointCount} waypoints")

            // 4. Carregar missão
            Log.d(TAG, "📤 Carregando missão no operador...")
            val loadError = operator.loadMission(mission)
            if (loadError != null) {
                _missionState.value = MissionState.ERROR
                val errorMessage = buildString {
                    append("Erro ao carregar missão no drone: ")
                    append(loadError.description)
                    append(" (Código: ${loadError.errorCode})")
                }
                Log.e(TAG, "❌ $errorMessage")
                
                // Fazer diagnóstico novamente quando falha
                Log.e(TAG, "⚠️ Diagnóstico após falha de carregamento:")
                diagnosticoDroneState()
                
                throw DJIMissionException(errorMessage)
            }

            Log.i(TAG, "✅ Missão carregada com sucesso no drone (${waypointList.size} waypoints, ${mission.waypointCount} confirmados)")

            // 5. Fazer upload com retry e timeout
            Log.d(TAG, "☁️ Iniciando upload da missão com retry...")
            try {
                retryOperation(MAX_RETRY_ATTEMPTS, RETRY_DELAY_MS) {
                    withTimeout(UPLOAD_TIMEOUT_MS) {
                        uploadMissionSuspend(operator)
                    }
                }
                Log.i(TAG, "✅ Upload da missão concluído com sucesso!")
                _missionState.value = MissionState.READY_TO_EXECUTE
            } catch (e: TimeoutCancellationException) {
                _missionState.value = MissionState.ERROR
                Log.e(TAG, "❌ Upload timeout após ${UPLOAD_TIMEOUT_MS}ms")
                throw DJIMissionException("Upload timeout (${UPLOAD_TIMEOUT_MS}ms)", e)
            } catch (e: Exception) {
                _missionState.value = MissionState.ERROR
                Log.e(TAG, "❌ Erro durante upload: ${e.message}")
                throw e
            }

        } catch (e: Exception) {
            _missionState.value = MissionState.ERROR
            Log.e(TAG, "❌ ERRO CRÍTICO ao preparar/upload missão: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    suspend fun startMission() {
        // Validar conexão do drone
        validateDroneConnection()
        
        val operator = getWaypointMissionOperator() ?: throw DJIMissionException(
            "WaypointMissionOperator não está disponível"
        )

        if (operator.currentState != WaypointMissionState.READY_TO_EXECUTE) {
            throw DJIMissionException(
                "Estado incorreto para iniciar. Estado atual: ${operator.currentState}"
            )
        }

        try {
            withTimeout(START_TIMEOUT_MS) {
                startMissionSuspend(operator)
            }
            Log.i(TAG, "✅ Missão iniciada com sucesso!")
        } catch (e: TimeoutCancellationException) {
            _missionState.value = MissionState.ERROR
            throw DJIMissionException("Start mission timeout (${START_TIMEOUT_MS}ms)", e)
        }
    }

    suspend fun stopMission() {
        // Validar conexão do drone
        validateDroneConnection()
        
        val operator = getWaypointMissionOperator() ?: throw DJIMissionException(
            "WaypointMissionOperator não está disponível"
        )

        try {
            withTimeout(STOP_TIMEOUT_MS) {
                stopMissionSuspend(operator)
            }
            _missionState.value = MissionState.EXECUTION_STOPPED
            Log.i(TAG, "✅ Missão parada com sucesso!")
        } catch (e: TimeoutCancellationException) {
            _missionState.value = MissionState.ERROR
            throw DJIMissionException("Stop mission timeout (${STOP_TIMEOUT_MS}ms)", e)
        }
    }

    suspend fun pauseMission() {
        // Validar conexão do drone
        validateDroneConnection()
        
        val operator = getWaypointMissionOperator() ?: throw DJIMissionException(
            "WaypointMissionOperator não está disponível"
        )

        try {
            withTimeout(PAUSE_TIMEOUT_MS) {
                pauseMissionSuspend(operator)
            }
            Log.i(TAG, "✅ Missão pausada com sucesso!")
        } catch (e: TimeoutCancellationException) {
            _missionState.value = MissionState.ERROR
            Log.e(TAG, "❌ Timeout ao pausar missão (${PAUSE_TIMEOUT_MS}ms)")
            throw DJIMissionException("Timeout ao pausar missão", e)
        } catch (e: Exception) {
            _missionState.value = MissionState.ERROR
            Log.e(TAG, "❌ Erro ao pausar missão: ${e.message}")
            throw DJIMissionException("Erro ao pausar missão", e)
        }
    }

    suspend fun resumeMission() {
        // Validar conexão do drone
        validateDroneConnection()
        
        val operator = getWaypointMissionOperator() ?: throw DJIMissionException(
            "WaypointMissionOperator não está disponível"
        )

        try {
            withTimeout(RESUME_TIMEOUT_MS) {
                resumeMissionSuspend(operator)
            }
            Log.i(TAG, "✅ Missão retomada com sucesso!")
        } catch (e: TimeoutCancellationException) {
            _missionState.value = MissionState.ERROR
            Log.e(TAG, "❌ Timeout ao retomar missão (${RESUME_TIMEOUT_MS}ms)")
            throw DJIMissionException("Timeout ao retomar missão", e)
        } catch (e: Exception) {
            _missionState.value = MissionState.ERROR
            Log.e(TAG, "❌ Erro ao retomar missão: ${e.message}")
            throw DJIMissionException("Erro ao retomar missão", e)
        }
    }

    // ========== RETRY LOGIC ==========

    /**
     * Executa uma operação com retry automático e backoff exponencial.
     * @param maxAttempts número máximo de tentativas (padrão 3)
     * @param initialDelayMs delay inicial em ms (padrão 100)
     * @param block a operação a ser executada
     * @throws Exception quando todas as tentativas falham
     */
    private suspend inline fun <T> retryOperation(
        maxAttempts: Int = MAX_RETRY_ATTEMPTS,
        initialDelayMs: Long = RETRY_DELAY_MS,
        crossinline block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var delayMs = initialDelayMs

        for (attempt in 1..maxAttempts) {
            try {
                Log.d(TAG, "🔄 Tentativa $attempt/$maxAttempts...")
                return block()
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "⚠️ Tentativa $attempt falhou: ${e.message}")

                if (attempt < maxAttempts) {
                    delay(delayMs)
                    delayMs *= 2 // Backoff exponencial: 100ms, 200ms, 400ms, ...
                }
            }
        }

        throw lastException ?: Exception("Operação falhou após $maxAttempts tentativas")
    }

    // ========== SUSPEND FUNCTIONS PARA CALLBACKS ==========

    private suspend fun uploadMissionSuspend(operator: WaypointMissionOperator) =
        suspendCancellableCoroutine<Unit> { continuation ->
            operator.uploadMission { error: dji.common.error.DJIError? ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        DJIMissionException("Falha no upload: ${error.description}")
                    )
                }
            }
        }

    private suspend fun startMissionSuspend(operator: WaypointMissionOperator) =
        suspendCancellableCoroutine<Unit> { continuation ->
            operator.startMission { error: dji.common.error.DJIError? ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        DJIMissionException("Falha ao iniciar: ${error.description}")
                    )
                }
            }
        }

    private suspend fun stopMissionSuspend(operator: WaypointMissionOperator) =
        suspendCancellableCoroutine<Unit> { continuation ->
            operator.stopMission { error: dji.common.error.DJIError? ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        DJIMissionException("Falha ao parar: ${error.description}")
                    )
                }
            }
        }

    private suspend fun pauseMissionSuspend(operator: WaypointMissionOperator) =
        suspendCancellableCoroutine<Unit> { continuation ->
            operator.pauseMission { error: dji.common.error.DJIError? ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        DJIMissionException("Falha ao pausar: ${error.description}")
                    )
                }
            }
        }

    private suspend fun resumeMissionSuspend(operator: WaypointMissionOperator) =
        suspendCancellableCoroutine<Unit> { continuation ->
            operator.resumeMission { error: dji.common.error.DJIError? ->
                if (error == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(
                        DJIMissionException("Falha ao retomar: ${error.description}")
                    )
                }
            }
        }

    // ========== VALIDAÇÕES ==========

    /**
     * Garante que o Home Point do drone esteja registrado antes da execução da missão.
     * Tenta registrar automaticamente usando a posição atual da aeronave.
     * Lança DJIMissionException com instruções acionáveis se não for possível.
     */
    private suspend fun ensureHomePointRecorded() {
        val product = djiConnectionHelper.getProductInstance() as? dji.sdk.products.Aircraft
            ?: throw DJIMissionException("Aeronave não disponível (produto não é Aircraft)")

        val flightController = product.flightController
            ?: throw DJIMissionException("FlightController não disponível")

        // Ler estado atual e satélites (quando disponível)
        val state = try { flightController.state } catch (e: Exception) { null }
        val satellites = try { state?.satelliteCount ?: 0 } catch (e: Exception) { 0 }
        var isHomeSet = try { state?.isHomeLocationSet ?: false } catch (e: Exception) { false }

        Log.d(TAG, "🔎 Pré-checagem: satélites=$satellites, homeSet=$isHomeSet")

        if (isHomeSet) {
            Log.i(TAG, "✅ Home Point já registrado")
            return
        }

        // Tentar registrar Home Point automaticamente (sem esperar GPS fix)
        try {
            Log.d(TAG, "📍 Tentando registrar Home Point automaticamente (Tentativa 1/3)...")
            setHomePointAutomatically(flightController)
            Log.i(TAG, "✅ Home Point registrado automaticamente!")
            return
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Tentativa 1 falhou: ${e.message}")
        }

        // Aguarda brevemente pela gravação automática do Home Point (GPS fix)
        try {
            Log.d(TAG, "⏳ Aguardando fix automático de Home Point (até 30s)...")
            waitForHomePointSet(flightController, timeoutMs = 30_000L)
            Log.i(TAG, "✅ Home Point registrado via GPS fix")
            return
        } catch (_: Exception) {
            Log.w(TAG, "⚠️ Timeout aguardando GPS fix")
        }

        // Se poucos satélites, avisar
        if (satellites in 0..5) {
            Log.w(TAG, "⚠️ Sinal GPS baixo (satélites=$satellites)")
        }

        // Segunda tentativa de registrar Home Point automaticamente
        try {
            Log.d(TAG, "📍 Tentando registrar Home Point novamente (Tentativa 2/3)...")
            setHomePointAutomatically(flightController)
            Log.i(TAG, "✅ Home Point registrado na segunda tentativa!")
            return
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Tentativa 2 falhou: ${e.message}")
        }

        // Aguardar mais um pouco e revalidar
        delay(2000L)
        val postState = try { flightController.state } catch (e: Exception) { null }
        val postHomeSet = try { postState?.isHomeLocationSet ?: false } catch (e: Exception) { false }
        
        if (postHomeSet) {
            Log.i(TAG, "✅ Home Point registrado após aguardar")
            return
        }

        // Terceira e última tentativa
        try {
            Log.d(TAG, "📍 Tentando registrar Home Point (Tentativa 3/3)...")
            setHomePointAutomatically(flightController)
            Log.i(TAG, "✅ Home Point registrado na terceira tentativa!")
            return
        } catch (e: Exception) {
            Log.e(TAG, "❌ Falha final ao registrar Home Point: ${e.message}")
        }

        // Se tudo falhou, lançar exceção com instruções claras
        Log.e(TAG, "❌ Não foi possível registrar automaticamente")
        throw DJIMissionException(
            "Home Point não foi registrado. Causas possíveis:\n" +
            "1. Sinal GPS insuficiente (satélites=$satellites, mínimo 10)\n" +
            "2. Drone acelerou rápido demais\n\n" +
            "Solução:\n" +
            "• Mantenha o drone parado em área aberta\n" +
            "• Aguarde 30-60 segundos para GPS fazer fix\n" +
            "• Verifique se tem pelo menos 10+ satélites\n" +
            "• Tente novamente"
        )
    }

    /**
     * Tenta registrar o Home Point da aeronave automaticamente.
     * Usa a posição GPS atual como referência.
     */
    private suspend fun setHomePointAutomatically(flightController: dji.sdk.flightcontroller.FlightController) {
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                flightController.setHomeLocationUsingAircraftCurrentLocation { error: dji.common.error.DJIError? ->
                    if (error == null) {
                        Log.d(TAG, "✅ setHomeLocationUsingAircraftCurrentLocation bem-sucedido")
                        continuation.resume(Unit)
                    } else {
                        Log.w(TAG, "⚠️ setHomeLocationUsingAircraftCurrentLocation falhou: ${error.description}")
                        continuation.resumeWithException(
                            DJIMissionException("Erro ao registrar Home Point: ${error.description}")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Exceção ao chamar setHomeLocationUsingAircraftCurrentLocation: ${e.message}")
            throw DJIMissionException("Não foi possível registrar Home Point: ${e.message}", e)
        }
    }

    /**
     * Aguarda até que `isHomeLocationSet` torne-se verdadeiro no `FlightController.state`.
     * Remove o callback no retorno ou cancelamento.
     */
    private suspend fun waitForHomePointSet(
        flightController: dji.sdk.flightcontroller.FlightController,
        timeoutMs: Long
    ) {
        kotlinx.coroutines.withTimeout(timeoutMs) {
            kotlinx.coroutines.suspendCancellableCoroutine<Unit> { cont ->
                // Registra callback de estado para observar `isHomeLocationSet`
                val callback: (dji.common.flightcontroller.FlightControllerState) -> Unit = { st ->
                    val homeSet = try { st.isHomeLocationSet } catch (_: Exception) { false }
                    if (homeSet) {
                        try { flightController.setStateCallback(null) } catch (_: Exception) {}
                        cont.resume(Unit)
                    }
                }
                try {
                    flightController.setStateCallback(callback)
                } catch (e: Exception) {
                    cont.resumeWithException(DJIMissionException("Não foi possível observar estado do FlightController: ${e.message}"))
                    return@suspendCancellableCoroutine
                }
                cont.invokeOnCancellation {
                    try { flightController.setStateCallback(null) } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Valida e extrai dados de coordenadas de um waypoint.
     * @return Triple(latitude, longitude, altitude) se válido, ou null se inválido
     */
    private fun extractAndValidateCoordinates(wp: Any): Triple<Double, Double, Double>? {
        return try {
            val (lat, lng, alt) = when (wp) {
                is ServerWaypoint -> {
                    // Nosso modelo de servidor
                    Triple(wp.latitude, wp.longitude, wp.altitude)
                }
                is Map<*, *> -> {
                    // Map genérico
                    @Suppress("UNCHECKED_CAST")
                    val map = wp as Map<String, Any>
                    Triple(
                        map["latitude"] as Double,
                        map["longitude"] as Double,
                        (map["altitude"] as Number).toDouble()
                    )
                }
                else -> {
                    // Qualquer outro tipo (incluindo DJI Waypoint): extrair via reflexão
                    Triple(
                        wp.javaClass.getMethod("getLatitude").invoke(wp) as Double,
                        wp.javaClass.getMethod("getLongitude").invoke(wp) as Double,
                        (wp.javaClass.getMethod("getAltitude").invoke(wp) as Number).toDouble()
                    )
                }
            }

            // Validar latitude
            if (lat !in -90.0..90.0) {
                Log.w(TAG, "⚠️ Latitude inválida: $lat (permitido: -90 a 90)")
                return null
            }

            // Validar longitude
            if (lng !in -180.0..180.0) {
                Log.w(TAG, "⚠️ Longitude inválida: $lng (permitido: -180 a 180)")
                return null
            }

            // Validar altitude
            if (alt.toFloat() !in DroneConstants.MIN_ALTITUDE..DroneConstants.MAX_ALTITUDE) {
                Log.w(TAG, "⚠️ Altitude inválida: $alt m (permitido: ${DroneConstants.MIN_ALTITUDE}-${DroneConstants.MAX_ALTITUDE}m)")
                return null
            }

            Triple(lat, lng, alt)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Erro ao processar waypoint: ${e.message}")
            null
        }
    }

    private fun validateAndFilterWaypoints(waypoints: List<Any>): List<Waypoint> {
        if (waypoints.isEmpty()) {
            throw IllegalArgumentException("Nenhum waypoint fornecido")
        }

        Log.d(TAG, "📍 Processando $waypoints.size} waypoints para validação...")

        val validWaypoints = waypoints.mapIndexed { index, wp ->
            val (latitude, longitude, altitude) = extractAndValidateCoordinates(wp) ?: return@mapIndexed null
            Log.d(TAG, "  ✓ Waypoint #${index + 1}: lat=$latitude, lng=$longitude, alt=${altitude.toFloat()}m")
            Waypoint(latitude, longitude, altitude.toFloat())
        }.filterNotNull()

        if (validWaypoints.isEmpty()) {
            throw IllegalArgumentException(
                "Nenhum waypoint válido após filtragem (altitude: ${DroneConstants.MIN_ALTITUDE}-${DroneConstants.MAX_ALTITUDE}m, lat: -90 a 90, lng: -180 a 180)"
            )
        }

        Log.i(TAG, "✅ ${validWaypoints.size}/${waypoints.size} waypoints válidos")
        return validWaypoints
    }

    private fun validateFlightParameters(autoSpeed: Float, maxSpeed: Float) {
        // Validar velocidade automática
        if (autoSpeed !in MIN_AUTO_FLIGHT_SPEED..MAX_AUTO_FLIGHT_SPEED) {
            throw IllegalArgumentException(
                "Auto flight speed inválida: $autoSpeed (permitido: $MIN_AUTO_FLIGHT_SPEED-$MAX_AUTO_FLIGHT_SPEED m/s)"
            )
        }

        // Validar velocidade máxima
        if (maxSpeed !in MIN_MAX_FLIGHT_SPEED..MAX_FLIGHT_SPEED_LIMIT) {
            throw IllegalArgumentException(
                "Max flight speed inválida: $maxSpeed (permitido: $MIN_MAX_FLIGHT_SPEED-$MAX_FLIGHT_SPEED_LIMIT m/s)"
            )
        }

        // Validar relação entre velocidades
        if (maxSpeed < autoSpeed) {
            throw IllegalArgumentException(
                "Max flight speed ($maxSpeed) não pode ser menor que auto flight speed ($autoSpeed)"
            )
        }

        Log.d(TAG, "✅ Parâmetros de voo validados: auto=$autoSpeed m/s, max=$maxSpeed m/s")
    }

    @Suppress("DEPRECATION")
    private fun buildWaypointMission(
        missionData: ServerMission,
        waypointList: List<Waypoint>
    ): WaypointMission {
        return try {
            // Validar enums antes de usar
            val finishedAction = try {
                WaypointMissionFinishedAction.valueOf(missionData.finished_action)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "⚠️ Ação final inválida: ${missionData.finished_action}, usando padrão")
                WaypointMissionFinishedAction.NO_ACTION
            }

            val headingMode = try {
                WaypointMissionHeadingMode.valueOf(missionData.heading_mode)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "⚠️ Modo heading inválido: ${missionData.heading_mode}, usando padrão")
                WaypointMissionHeadingMode.AUTO
            }

            val flightPathMode = try {
                WaypointMissionFlightPathMode.valueOf(missionData.flight_path_mode)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "⚠️ Modo flight path inválido: ${missionData.flight_path_mode}, usando padrão")
                WaypointMissionFlightPathMode.NORMAL
            }

            Log.d(TAG, "🔧 Configurando missão: finishedAction=$finishedAction, heading=$headingMode, flightPath=$flightPathMode")

            WaypointMission.Builder().apply {
                finishedAction(finishedAction)
                headingMode(headingMode)
                autoFlightSpeed(missionData.auto_flight_speed.toFloat())
                maxFlightSpeed(missionData.max_flight_speed.toFloat())
                flightPathMode(flightPathMode)
                waypointList(waypointList)
                waypointCount(waypointList.size)
            }.build()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao construir missão: ${e.message}")
            e.printStackTrace()
            throw IllegalArgumentException("Erro ao construir missão: ${e.message}", e)
        }
    }

    private fun isSupported(model: Model?): Boolean {
        val name = model?.displayName ?: return false
        return SUPPORTED_DRONE_MODELS.any { it.equals(name, ignoreCase = true) }
    }

    // ========== CLEANUP ==========

    /**
     * Libera recursos e remove listeners.
     * DEVE ser chamado quando a Activity/Fragment é destruída.
     */
    /**
     * Libera recursos e remove listeners de forma síncrona.
     * DEVE ser chamado quando a Activity/Fragment é destruída.
     * 
     * IMPORTANTE: Este método é SÍNCRONO e bloqueia a thread até que
     * a limpeza seja concluída, garantindo que todos os recursos sejam
     * liberados antes que a Activity seja destruída.
     */
    fun destroy() {
        try {
            Log.d(TAG, "🛑 Iniciando limpeza de recursos...")

            // 1. Parar missão em execução de forma não bloqueante (best-effort)
            if (_missionState.value == MissionState.EXECUTING ||
                _missionState.value == MissionState.EXECUTION_PAUSED
            ) {
                try {
                    Log.d(TAG, "⏹️ Solicitando parada da missão (assíncrono)...")
                    getWaypointMissionOperator()?.stopMission { error: dji.common.error.DJIError? ->
                        if (error == null) {
                            Log.d(TAG, "✅ Missão parada durante cleanup")
                        } else {
                            Log.w(TAG, "⚠️ Falha ao parar missão no cleanup: ${error.description}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erro ao solicitar parada no cleanup: ${e.message}")
                }
            }

            // 2. Remover listener
            if (listenerAdded) {
                getWaypointMissionOperator()?.removeListener(missionListener)
                listenerAdded = false
                Log.d(TAG, "✅ Mission Listener removido")
            }

            // 3. Cancelar coroutine scope
            scope.cancel()
            Log.d(TAG, "✅ Coroutine Scope cancelado")

            Log.d(TAG, "✅ DroneMissionManager destruído com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao destruir DroneMissionManager: ${e.message}")
        }
    }

    // ========== LISTENER INTERNO ==========

    private inner class MissionListenerImpl : WaypointMissionOperatorListener {
        override fun onDownloadUpdate(event: WaypointMissionDownloadEvent) {
            val error = event.error
            val progress = event.progress

            if (error != null) {
                Log.e(TAG, "❌ Erro no download: ${error.description}")
                _missionState.value = MissionState.ERROR
                return
            }

            if (progress != null) {
                _missionState.value = MissionState.DOWNLOADING
                Log.d(
                    TAG,
                    "⬇️ Download: ${progress.downloadedWaypointIndex}/${progress.totalWaypointCount}"
                )

                if (progress.downloadedWaypointIndex == progress.totalWaypointCount) {
                    _missionState.value = MissionState.DOWNLOAD_FINISHED
                    Log.d(TAG, "✅ Download concluído")
                }
            }
        }

        override fun onUploadUpdate(event: WaypointMissionUploadEvent) {
            val currentState = event.currentState
            when (currentState) {
                WaypointMissionState.UPLOADING -> {
                    _missionState.value = MissionState.UPLOADING
                    Log.d(TAG, "⬆️ Upload em progresso...")
                }
                WaypointMissionState.READY_TO_EXECUTE -> {
                    _missionState.value = MissionState.READY_TO_EXECUTE
                    Log.d(TAG, "✅ Pronto para executar")
                }
                else -> {}
            }
        }

        override fun onExecutionStart() {
            _missionState.value = MissionState.EXECUTING
            Log.i(TAG, "▶️ Missão iniciada")
        }

        override fun onExecutionUpdate(event: WaypointMissionExecutionEvent) {
            val currentState = event.currentState
            when (currentState) {
                WaypointMissionState.EXECUTING -> {
                    _missionState.value = MissionState.EXECUTING
                }
                WaypointMissionState.EXECUTION_PAUSED -> {
                    _missionState.value = MissionState.EXECUTION_PAUSED
                    Log.i(TAG, "⏸️ Missão pausada")
                }
                else -> {}
            }
        }

        override fun onExecutionFinish(error: DJIError?) {
            if (error == null) {
                _missionState.value = MissionState.FINISHED
                Log.i(TAG, "✅ Missão concluída com sucesso!")
            } else {
                _missionState.value = MissionState.ERROR
                Log.e(TAG, "❌ Erro na conclusão: ${error.description}")
            }
        }
    }
}

/**
 * Exception customizada para erros de missão DJI
 */
class DJIMissionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
