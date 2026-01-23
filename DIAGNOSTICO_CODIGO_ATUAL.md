# 🔍 DIAGNÓSTICO COMPLETO - DroneMissionManager.kt

**Data:** 23 de janeiro de 2026  
**Arquivo:** `app/src/main/java/com/sloth/registerapp/features/mission/data/drone/DroneMissionManager.kt`  
**Tamanho:** 949 linhas  
**Status:** ⚠️ Requer comparação com projeto funcional

---

## 📋 1. ESTRUTURA GERAL

### Classe Principal
```
DroneMissionManager
├── Dependências:
│   ├── djiConnectionHelper (DJIConnectionHelper)
│   ├── scope (CoroutineScope)
│   └── missionListener (WaypointMissionOperatorListener)
├── Estados: MissionState (11 estados)
└── Constantes de Timeout e Speed
```

### Estados de Missão (enum MissionState)
| Estado | Descrição |
|--------|-----------|
| IDLE | Nada conectado/ativo |
| PREPARING | Validações iniciais |
| DOWNLOADING | Download em andamento |
| DOWNLOAD_FINISHED | Download concluído |
| UPLOADING | Upload em andamento |
| READY_TO_EXECUTE | Missão validada e pronta |
| EXECUTING | Missão em execução |
| EXECUTION_PAUSED | Missão pausada |
| EXECUTION_STOPPED | Interrompida manualmente |
| FINISHED | Missão finalizada com sucesso |
| ERROR | Erro irrecuperável |

---

## ⚙️ 2. CONFIGURAÇÕES E CONSTANTES

### Drones Suportados (10 modelos)
- Mavic Pro, Mavic 2 Pro, Mavic 2 Zoom, Mavic 2 Enterprise
- Phantom 4 Pro, Phantom 4 RTK
- Phantom 3 Professional, Phantom 3 Advanced
- Inspire 1, Inspire 2

### Timeouts
| Operação | Timeout |
|----------|---------|
| Upload | 30.000ms (30s) |
| Start | 10.000ms (10s) |
| Stop | 10.000ms (10s) |
| Pause | 5.000ms (5s) |
| Resume | 5.000ms (5s) |

### Limites de Velocidade
| Parâmetro | Min | Max |
|-----------|-----|-----|
| Auto Flight Speed | 0.5 m/s | 20 m/s |
| Max Flight Speed | 0.5 m/s | 30 m/s |

### Retry
- Máximo de tentativas: 3
- Delay inicial: 1000ms (backoff exponencial)

---

## 🔄 3. FLUXO PRINCIPAL DE OPERAÇÕES

### A. INICIALIZAÇÃO (init)
```
init()
├── initializeConnectionMonitoring()
│   ├── Escuta mudanças do djiConnectionHelper.product
│   ├── Verifica se drone está conectado
│   ├── Valida modelo de drone
│   ├── Atualiza MissionState (IDLE/ERROR)
│   └── Adiciona MissionListener
└── addMissionListener()
    └── Registra listener no WaypointMissionOperator
```

### B. PREPARAR E FAZER UPLOAD (prepareAndUploadMission)
```
prepareAndUploadMission(missionData)
├── Estado: PREPARING
├── validateDroneConnection() ✓
├── diagnosticoDroneState() [LOG]
├── validateAndFilterWaypoints()
│   ├── Valida latitude (-90 a 90)
│   ├── Valida longitude (-180 a 180)
│   ├── Valida altitude (DroneConstants.MIN/MAX)
│   └── Filtra waypoints inválidos
├── validateFlightParameters()
│   ├── Valida auto_flight_speed
│   ├── Valida max_flight_speed
│   └── Valida relação entre velocidades
├── buildWaypointMission()
│   ├── Valida enums (finishedAction, headingMode, flightPathMode)
│   ├── Cria WaypointMission.Builder
│   └── Seta configurações e waypoints
├── operator.loadMission(mission)
│   ├── Se erro: MissionState = ERROR + throw
│   └── Se ok: continua
├── Retry Upload (MAX_RETRY_ATTEMPTS = 3)
│   ├── withTimeout(UPLOAD_TIMEOUT_MS)
│   ├── operator.uploadMission()
│   ├── Backoff exponencial
│   └── MissionState = READY_TO_EXECUTE
└── Exceptions:
    ├── DJIMissionException (drone não conectado)
    ├── DJIMissionException (operador não disponível)
    ├── IllegalArgumentException (waypoints inválidos)
    ├── DJIMissionException (erro ao carregar)
    └── DJIMissionException/TimeoutCancellationException (upload falha)
```

### C. INICIAR MISSÃO (startMission)
```
startMission()
├── validateDroneConnection() ✓
├── Verifica operator.currentState == READY_TO_EXECUTE ✓
├── ensureHomePointRecorded() [CRÍTICO]
│   ├── Obtém FlightController
│   ├── Lê satélites e isHomeLocationSet
│   ├── Se já definido: retorna
│   ├── Tentativa 1: setHomePointAutomatically()
│   ├── Aguarda até 30s por GPS fix
│   ├── Tentativa 2: setHomePointAutomatically()
│   ├── Aguarda 2s
│   ├── Tentativa 3: setHomePointAutomatically()
│   └── Se tudo falhar: throw DJIMissionException
├── withTimeout(START_TIMEOUT_MS)
├── operator.startMission()
└── MissionState = EXECUTING
```

### D. PARAR MISSÃO (stopMission)
```
stopMission()
├── validateDroneConnection() ✓
├── withTimeout(STOP_TIMEOUT_MS)
├── operator.stopMission()
└── MissionState = EXECUTION_STOPPED
```

### E. PAUSAR MISSÃO (pauseMission)
```
pauseMission()
├── validateDroneConnection() ✓
├── withTimeout(PAUSE_TIMEOUT_MS)
├── operator.pauseMission()
└── MissionState segue para EXECUTION_PAUSED (via listener)
```

### F. RETOMAR MISSÃO (resumeMission)
```
resumeMission()
├── validateDroneConnection() ✓
├── withTimeout(RESUME_TIMEOUT_MS)
├── operator.resumeMission()
└── MissionState segue para EXECUTING (via listener)
```

---

## 🏠 4. HOME POINT LOGIC (NOVO)

### setHomePointAutomatically()
```kotlin
private suspend fun setHomePointAutomatically(flightController)
├── Chamada: flightController.setHomeLocationUsingAircraftCurrentLocation()
├── Callback retorna DJIError ou null
├── Se sucesso: resume(Unit)
└── Se erro: resumeWithException(DJIMissionException)
```

### ensureHomePointRecorded() - Novo Fluxo
```
1ª Tentativa: setHomePointAutomatically()
2ª Tentativa: Aguarda 30s por GPS fix automático
3ª Tentativa: setHomePointAutomatically()
4ª Tentativa: Aguarda 2s
5ª Tentativa: setHomePointAutomatically()
Falha: Mensagem de erro detalhada com diagnóstico
```

---

## 📡 5. LISTENER (WaypointMissionOperatorListener)

### Eventos Rastreados
| Evento | Ação |
|--------|------|
| onDownloadUpdate() | MissionState = DOWNLOADING / DOWNLOAD_FINISHED |
| onUploadUpdate() | MissionState = UPLOADING / READY_TO_EXECUTE |
| onExecutionStart() | MissionState = EXECUTING |
| onExecutionUpdate() | MissionState = EXECUTING / EXECUTION_PAUSED |
| onExecutionFinish() | MissionState = FINISHED / ERROR |

---

## ✅ 6. VALIDAÇÕES IMPLEMENTADAS

### Drone Connection
- ✓ isDroneConnected() - verifica se product != null
- ✓ validateDroneConnection() - throw se desconectado
- ✓ diagnosticoDroneState() - diagnóstico detalhado em log

### Waypoints
- ✓ Latitude: -90 a 90
- ✓ Longitude: -180 a 180
- ✓ Altitude: DroneConstants.MIN/MAX
- ✓ Suporta múltiplos formatos: ServerWaypoint, Map, reflexão

### Flight Parameters
- ✓ autoSpeed: 0.5 a 20 m/s
- ✓ maxSpeed: 0.5 a 30 m/s
- ✓ maxSpeed >= autoSpeed

### Home Point
- ✓ Verifica isHomeLocationSet
- ✓ Tenta registrar automaticamente 3x
- ✓ Aguarda GPS fix até 30s
- ✓ Mensagem de erro com diagnóstico

### Enums
- ✓ WaypointMissionFinishedAction (fallback: NO_ACTION)
- ✓ WaypointMissionHeadingMode (fallback: AUTO)
- ✓ WaypointMissionFlightPathMode (fallback: NORMAL)

---

## ⚠️ 7. TRATAMENTO DE ERROS

### Exceptions Customizadas
```kotlin
class DJIMissionException(message, cause) : Exception
```

### Tipos de Erro Capturados
| Cenário | Tratamento | Estado |
|---------|-----------|--------|
| Drone desconectado | throw DJIMissionException | ERROR |
| Operador indisponível | throw DJIMissionException | ERROR |
| Waypoints inválidos | throw IllegalArgumentException | ERROR |
| Falha ao carregar | throw DJIMissionException | ERROR |
| Upload timeout | throw DJIMissionException | ERROR |
| Home Point não registrado | throw DJIMissionException | ERROR |
| Start timeout | throw DJIMissionException | ERROR |
| Stop timeout | throw DJIMissionException | ERROR |
| Pause timeout | throw DJIMissionException | ERROR |
| Resume timeout | throw DJIMissionException | ERROR |

### Try-Catch Estratégicos
- ✓ initializeConnectionMonitoring: try-catch no estado
- ✓ diagnosticoDroneState: try-catch em leituras
- ✓ extractAndValidateCoordinates: try-catch com reflexão
- ✓ buildWaypointMission: try-catch em validação de enums
- ✓ waitForHomePointSet: try-catch em callbacks
- ✓ setHomePointAutomatically: try-catch em suspensão
- ✓ destroy: try-catch em limpeza

---

## 🔄 8. RETRY E TIMEOUT LOGIC

### Retry Operation
```kotlin
private suspend inline fun retryOperation<T>(
    maxAttempts: 3,
    initialDelayMs: 1000L,
    block: suspend () -> T
)
```

**Behavior:**
- Executa bloco up to 3 vezes
- Backoff exponencial: 1s, 2s, 4s
- Log de tentativas
- Throw na última falha

### Timeouts
- ✓ Upload: 30s com retry
- ✓ Start: 10s (sem retry)
- ✓ Stop: 10s (sem retry)
- ✓ Pause: 5s (sem retry)
- ✓ Resume: 5s (sem retry)
- ✓ Home Point GPS fix: 30s (sem retry)

---

## 📊 9. COROUTINES E THREADING

### Scope Management
```kotlin
CoroutineScope(Dispatchers.Main) // Por padrão
```

### Launch Points
1. `init()`: initializeConnectionMonitoring() - Main
2. `initializeConnectionMonitoring()`: djiConnectionHelper.product.collect() - Flow
3. `prepareAndUploadMission()`: suspend function
4. `startMission()`: suspend function
5. `stopMission()`: suspend function (assíncrono no destroy)

### Suspensão
- ✓ retryOperation: withTimeout + suspendCancellableCoroutine
- ✓ uploadMissionSuspend: suspendCancellableCoroutine
- ✓ startMissionSuspend: suspendCancellableCoroutine
- ✓ stopMissionSuspend: suspendCancellableCoroutine
- ✓ pauseMissionSuspend: suspendCancellableCoroutine
- ✓ resumeMissionSuspend: suspendCancellableCoroutine
- ✓ waitForHomePointSet: withTimeout + suspendCancellableCoroutine
- ✓ setHomePointAutomatically: suspendCancellableCoroutine

---

## 🧹 10. CLEANUP (destroy)

```
destroy()
├── Parar missão em execução (assíncrono, best-effort)
├── Remover listener
├── Cancelar coroutine scope
└── Log de sucesso/erro
```

**Importante:** O método é SÍNCRONO mas chama stopMission de forma assíncrona

---

## 🚨 11. POSSÍVEIS PROBLEMAS / GAPS

### Detectados no Código Atual
1. **Home Point Logic** (NOVO - POSSÍVEL PROBLEMA)
   - Método `setHomeLocationUsingAircraftCurrentLocation()` pode não existir em todas versões do SDK
   - Sem documentação clara de qual versão do SDK está sendo usada
   - Sem tratamento de exceção específica se o método não existir

2. **FlightController Access**
   - Não há verificação se `flightController` está pronto/inicializado
   - Acesso direto a `flightController.state` pode retornar null

3. **Listener Lifecycle**
   - Listener adicionado em `init()`, mas pode haver race condition
   - `listenerAdded` flag pode ficar inconsistente se erros ocorrerem

4. **Product Casting**
   - `as? dji.sdk.products.Aircraft` pode falhar silenciosamente
   - Sem verificação adicional de type safety

5. **Callback Cleanup**
   - Em `waitForHomePointSet()`, callback pode não ser removido se timeout ocorrer
   - `invokeOnCancellation` deveria ser mais robusto

6. **Drone Model Validation**
   - Lista de modelos suportados é hardcoded (10 modelos)
   - Novos drones não são suportados automaticamente

7. **Diagnostic Logging**
   - `diagnosticoDroneState()` imprime muitas linhas de log
   - Pode ser verboso demais

8. **Error Messages**
   - Home Point error message é bilíngue (português/inglês), pode confundir usuário

9. **GPS Fix Wait**
   - `waitForHomePointSet()` aguarda até 30s, mas pode bloquear UI se chamado na Main thread
   - `setStateCallback()` pode não ser suportado em todos os drones

10. **Enum Fallbacks**
    - Fallbacks silenciosos para enums (NO_ACTION, AUTO, NORMAL)
    - Sem log clara indicando que foi usado fallback

---

## 📈 12. FLUXO DE EXECUÇÃO ESPERADO

```
┌─────────────────────────────────────────────────────┐
│ 1. DroneMissionManager criado (init)                │
│    ├─ initializeConnectionMonitoring() [Flow]       │
│    └─ addMissionListener() [Operador]               │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ 2. Drone conectado                                  │
│    ├─ product != null                               │
│    ├─ validar modelo                                │
│    └─ estado = IDLE                                 │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ 3. prepareAndUploadMission(missionData)            │
│    ├─ PREPARING                                     │
│    ├─ validar drone conectado                       │
│    ├─ validar/filtrar waypoints                     │
│    ├─ validar parâmetros de voo                     │
│    ├─ construir missão DJI                          │
│    ├─ carregar no operador                          │
│    ├─ upload com retry (3x)                         │
│    └─ READY_TO_EXECUTE                              │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ 4. startMission()                                   │
│    ├─ validar drone conectado                       │
│    ├─ ensureHomePointRecorded()                     │
│    │  ├─ verificar se já está registrado            │
│    │  ├─ 3x tentar setHomePointAutomatically()      │
│    │  ├─ aguardar 30s GPS fix                       │
│    │  └─ throw se falhar                            │
│    ├─ operator.startMission()                       │
│    └─ EXECUTING                                     │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ 5. Durante execução                                 │
│    ├─ pauseMission() → EXECUTION_PAUSED             │
│    ├─ resumeMission() → EXECUTING                   │
│    └─ listener.onExecutionUpdate() [background]    │
└────────────────┬────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────┐
│ 6. Missão finaliza                                  │
│    └─ listener.onExecutionFinish()                  │
│       ├─ FINISHED (sucesso)                         │
│       └─ ERROR (falha)                              │
└─────────────────────────────────────────────────────┘
```

---

## 🔍 13. COMPARAÇÃO COM PROJETO FUNCIONAL

**Por favor, analise estas áreas no projeto funcional:**

1. ✓ **Home Point Registration**
   - Como é feito? Via API ou manual?
   - Qual método é chamado?
   - Qual é o timeout esperado?
   - Há retry?

2. ✓ **Download/Upload**
   - Há listeners para download?
   - Como é tratado o progresso?
   - Há timeout diferente?

3. ✓ **FlightController Access**
   - Como é obtido de forma segura?
   - Há initialization check?
   - Há callback setup?

4. ✓ **Listener Lifecycle**
   - Quando é adicionado exatamente?
   - Quando é removido?
   - Há sincronização?

5. ✓ **Error Handling**
   - Quais exceções são esperadas?
   - Como são tratadas?
   - Há recovery logic?

6. ✓ **State Management**
   - Estados são diferentes?
   - Transições diferentes?
   - Há estados intermediários?

7. ✓ **Timeouts**
   - Valores são iguais?
   - Há diferença por operação?
   - Há retry com backoff?

8. ✓ **Validation**
   - Ordem de validações diferente?
   - Há validações extras?
   - Limites numéricos diferentes?

9. ✓ **Threading/Coroutines**
   - Usa CoroutineScope diferente?
   - Há sincronização especial?
   - Main thread vs background?

10. ✓ **Logging**
    - Verbose diferente?
    - Indicadores diferentes?
    - Diagnóstico diferente?

---

## 📋 14. CHECKLIST DE COMPARAÇÃO

Use este checklist ao comparar com o projeto funcional:

```
INICIALIZAÇÃO
☐ Constructor/init idêntico?
☐ Default CoroutineScope igual?
☐ Listener adicionado no mesmo ponto?
☐ Product monitoring igual?

PREPARAÇÃO DE MISSÃO
☐ Validações na mesma ordem?
☐ Limites de velocidade iguais?
☐ Limites de altitude iguais?
☐ Limites de coordenadas iguais?
☐ Retry logic igual?
☐ Timeouts iguais?

HOME POINT
☐ Como é registrado no funcional?
☐ Qual método DJI é chamado?
☐ Há mais tentativas?
☐ Timeout diferente?
☐ Há condition check diferente?
☐ GPS satellites threshold diferente?

EXECUÇÃO
☐ startMission() pré-requisitos iguais?
☐ Ordem de checks igual?
☐ Timeout igual?
☐ pauseMission() funcionamento igual?
☐ resumeMission() funcionamento igual?
☐ stopMission() funcionamento igual?

LISTENER
☐ Eventos rastreados iguais?
☐ State transitions iguais?
☐ Tratamento de erro no listener igual?
☐ Cleanup do listener igual?

CLEANUP
☐ destroy() método existe?
☐ Ordem de limpeza igual?
☐ Listener removido?
☐ Scope cancelado?
```

---

## 🎯 15. RESUMO EXECUTIVO

| Aspecto | Status | Detalhes |
|---------|--------|----------|
| **Estrutura** | ✓ Completa | 11 estados, retry logic, listeners |
| **Validação** | ✓ Robusta | Waypoints, speeds, coords, enums |
| **Home Point** | ⚠️ NOVO | 3 tentativas, 30s GPS fix wait, API setHome |
| **Error Handling** | ✓ Bom | DJIMissionException, try-catch estratégicos |
| **Timeouts** | ✓ Definidos | 30s upload, 10s start, 5s pause/resume |
| **Coroutines** | ✓ Bem usado | Suspend, retry, timeout coordenado |
| **Listener** | ✓ Completo | 5 eventos, state management automático |
| **Cleanup** | ✓ Implementado | destroy() with scope cancel |
| **Logging** | ✓ Detalhado | Diagnóstico, retry, state transitions |
| **Comparação** | ❌ Pendente | Aguardando projeto funcional |

---

## 📝 NOTAS PARA COMPARAÇÃO

Leve este documento ao analisar o projeto funcional e procure por:

1. **Diferenças em method calls** (setHomeLocationUsingAircraftCurrentLocation existe?)
2. **Diferenças em timeout values** (são iguais ou diferentes?)
3. **Diferenças em state transitions** (há estados extras?)
4. **Diferenças em error handling** (há tratamentos diferentes?)
5. **Diferenças em retry logic** (há retry em diferentes operações?)
6. **Diferenças em initialization** (há setup extra?)
7. **Diferenças em listener management** (quando é adicionado/removido?)
8. **Diferenças em home point logic** (como funciona no código que voa?)

---

**Quando tiver o projeto funcional pronto para comparar, relate as diferenças que encontrar e qual é o método de Home Point que realmente funciona.**
