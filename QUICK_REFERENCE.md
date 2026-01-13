# 📚 Quick Reference - DroneMissionManager

## 🎯 Uso Rápido

### Criar Instance
```kotlin
val missionManager = DroneMissionManager(djiConnectionHelper)
```

### Upload Missão (Suspend)
```kotlin
viewModelScope.launch {
    try {
        missionManager.prepareAndUploadMission(missionData)
        // ✅ Pronto para executar
    } catch (e: Exception) {
        // ❌ Erro
    }
}
```

### Iniciar/Pausar/Retomar/Parar
```kotlin
missionManager.startMission()      // ▶️ Inicia
missionManager.pauseMission()      // ⏸️ Pausa
missionManager.resumeMission()     // ▶️ Retoma
missionManager.stopMission()       // ⏹️ Para
```

### Cleanup (IMPORTANTE!)
```kotlin
override fun onDestroy() {
    super.onDestroy()
    missionManager.destroy()  // 🧹 Libera recursos
}
```

### Observar Estado
```kotlin
lifecycleScope.launch {
    missionManager.missionState.collect { state ->
        when (state) {
            MissionState.UPLOADING -> showProgressBar()
            MissionState.READY_TO_EXECUTE -> enableStartButton()
            MissionState.EXECUTING -> disableButtons()
            MissionState.FINISHED -> showSuccess()
            MissionState.ERROR -> showError()
            else -> {}
        }
    }
}
```

---

## 📊 Estados da Missão

```
IDLE → PREPARING → UPLOADING → READY_TO_EXECUTE → EXECUTING → FINISHED
                ↓                                      ↓
              ERROR ←──────────────────────────────────┘
```

| Estado | Descrição | Pode fazer |
|--------|-----------|-----------|
| **IDLE** | Inativo/conectado | Upload |
| **PREPARING** | Validando | - |
| **UPLOADING** | Upload em progresso | - |
| **READY_TO_EXECUTE** | Pronto para voar | Start |
| **EXECUTING** | Voando | Pause, Stop |
| **EXECUTION_PAUSED** | Pausado | Resume, Stop |
| **EXECUTION_STOPPED** | Parado manualmente | - |
| **FINISHED** | Concluído com sucesso | - |
| **ERROR** | Erro irrecuperável | - |

---

## 🚨 Exceptions

### DJIMissionException
```kotlin
// SDK DJI error ou timeout
try {
    missionManager.startMission()
} catch (e: DJIMissionException) {
    Log.e(TAG, "DJI Error: ${e.message}")
}
```

### IllegalArgumentException
```kotlin
// Parâmetros inválidos
try {
    missionManager.prepareAndUploadMission(data)
} catch (e: IllegalArgumentException) {
    Log.e(TAG, "Validation failed: ${e.message}")
}
```

---

## ⚙️ Constantes Configuráveis

```kotlin
UPLOAD_TIMEOUT_MS = 30000L      // 30 segundos
START_TIMEOUT_MS = 10000L       // 10 segundos
STOP_TIMEOUT_MS = 10000L        // 10 segundos

MIN_AUTO_FLIGHT_SPEED = 0.5f    // m/s
MAX_AUTO_FLIGHT_SPEED = 20f     // m/s
MIN_MAX_FLIGHT_SPEED = 0.5f     // m/s
MAX_FLIGHT_SPEED_LIMIT = 30f    // m/s
```

---

## 🔍 Debugging

### Logcat Filter
```bash
adb logcat | grep "DroneMissionManager"
```

### Verbose Logging
```kotlin
if (BuildConfig.DEBUG) {
    System.setProperty("dji.debug.verbose", "true")
}
```

### Expected Logs
```
✅ Missão carregada com sucesso (5 waypoints)
✅ Upload da missão concluído!
✅ Missão iniciada com sucesso!
⬇️ Download: 0/5
⬇️ Download: 5/5
✅ Missão concluída com sucesso!
```

---

## 🧪 Testes Mínimos Necessários

- [ ] Upload missão com sucesso
- [ ] Validação rejeita waypoints inválidos
- [ ] Validação rejeita velocidades inválidas
- [ ] Start/Pause/Resume/Stop sequência
- [ ] Timeout protege operações
- [ ] Cleanup remove listeners
- [ ] Destroy pode ser chamado durante operação
- [ ] Estado é sincronizado com listener

---

## 📱 Android Integration Checklist

- [ ] AndroidX ViewModel adicionado
- [ ] Coroutines adicionadas
- [ ] Activity/Fragment implementada
- [ ] Layout XML criado
- [ ] Listeners de botão configurados
- [ ] `destroy()` em `onDestroy()`
- [ ] Observação de State/Flow
- [ ] Error handling implementado
- [ ] Testes unitários

---

## 🔐 Production Checklist

- [ ] Sem memory leaks (Android Studio Memory Profiler)
- [ ] Timeout adequado para conectividade
- [ ] Tratamento de desconexão durante operação
- [ ] Logs sem informações sensíveis
- [ ] Crash testing completado
- [ ] Battery impact testado
- [ ] Permisos DJI/Android configurados
- [ ] Documentação atualizada
- [ ] Versão stável

---

## 🎓 Recursos

| Recurso | Link |
|---------|------|
| **DJI SDK Docs** | https://developer.dji.com |
| **Kotlin Coroutines** | https://kotlinlang.org/docs/coroutines-overview.html |
| **Android ViewModel** | https://developer.android.com/topic/libraries/architecture/viewmodel |
| **Refactoring Guide** | [REFACTORING_GUIDE.md](REFACTORING_GUIDE.md) |
| **Operation Flows** | [OPERATION_FLOWS.md](OPERATION_FLOWS.md) |
| **Integration Guide** | [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) |

---

## 📞 FAQ Rápido

**P: Preciso chamar `destroy()`?**  
R: SIM, em `onDestroy()` de Activity/Fragment.

**P: Posso chamar métodos em sequence sem await?**  
R: Não, use `suspend` functions com `viewModelScope.launch`.

**P: Como adiciono retry logic?**  
R: Wrap `prepareAndUploadMission()` com retry decorator.

**P: Funciona offline?**  
R: Não, precisa conexão com drone via DJI API.

**P: Posso cancelar upload?**  
R: Sim, coroutine cancellation automática via `Job.cancel()`.

**P: Qual Android mínimo?**  
R: API 24+ (Coroutines + DJI SDK).

---

## 🚀 Um-Minuto Setup

```kotlin
// 1. Create
val missionMgr = DroneMissionManager(djiConnectionHelper)

// 2. Setup UI observers
lifecycleScope.launch {
    missionMgr.missionState.collect { state ->
        updateUI(state)
    }
}

// 3. Upload
lifecycleScope.launch {
    missionMgr.prepareAndUploadMission(mission)
}

// 4. Start
lifecycleScope.launch {
    missionMgr.startMission()
}

// 5. Cleanup
override fun onDestroy() {
    super.onDestroy()
    missionMgr.destroy()
}
```

---

## 🎯 Next Steps

1. Ler [REFACTORING_GUIDE.md](REFACTORING_GUIDE.md)
2. Ver exemplos em [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)
3. Estudar fluxos em [OPERATION_FLOWS.md](OPERATION_FLOWS.md)
4. Implementar em seu Activity/Fragment
5. Testar com simulador DJI
6. Testar com drone real

