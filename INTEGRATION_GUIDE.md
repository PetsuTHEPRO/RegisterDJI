# 🔌 Guia de Integração - DroneMissionManager Refatorado

## 📋 Pré-requisitos

- ✅ Android SDK 24+
- ✅ Kotlin 1.5+
- ✅ Coroutines 1.6+
- ✅ DJI SDK v4
- ✅ `DJIConnectionHelper` configurado

---

## 🚀 Passo 1: Adicionar Dependencies

Certifique-se que tem no `build.gradle.kts`:

```kotlin
dependencies {
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    
    // DJI SDK (já deve existir)
    // implementation("com.dji:dji-sdk:X.X.X")
}
```

---

## 📄 Passo 2: Atualizar o ViewModel

### Opção A: Usar o MissionViewModel Fornecido

```kotlin
// Copiar MissionViewModel.kt para seu projeto
// Depois:

class YourMissionActivity : AppCompatActivity() {
    
    private val viewModel: MissionViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mission)
        
        // Observar estado
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
            }
        }
        
        // Observar eventos
        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is UiEvent.ShowMessage -> {
                        Toast.makeText(this@YourMissionActivity, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is UiEvent.ShowError -> {
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun updateUI(state: MissionUiState) {
        when (state) {
            is MissionUiState.Uploading -> {
                progressBar.visibility = View.VISIBLE
                startButton.isEnabled = false
            }
            is MissionUiState.ReadyToExecute -> {
                progressBar.visibility = View.GONE
                startButton.isEnabled = true
            }
            is MissionUiState.Executing -> {
                startButton.isEnabled = false
                pauseButton.isEnabled = true
                stopButton.isEnabled = true
            }
            // ... outros estados
        }
    }
}
```

### Opção B: Usar a Classe Diretamente

```kotlin
class YourMissionActivity : AppCompatActivity() {
    
    private lateinit var missionManager: DroneMissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val djiConnectionHelper = DJIConnectionHelper.getInstance()
        missionManager = DroneMissionManager(djiConnectionHelper)
        
        lifecycleScope.launch {
            missionManager.missionState.collect { state ->
                handleStateChange(state)
            }
        }
    }
    
    private fun uploadMission(missionData: ServerMission) {
        lifecycleScope.launch {
            try {
                missionManager.prepareAndUploadMission(missionData)
                Toast.makeText(this@YourMissionActivity, "Missão pronta!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@YourMissionActivity, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        missionManager.destroy()  // ⭐ IMPORTANTE
    }
}
```

---

## 🎯 Passo 3: Implementar UI

### Exemplo de Layout

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Status Text -->
    <TextView
        android:id="@+id/statusText"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Status: IDLE"
        android:textSize="16sp" />

    <!-- Progress Bar -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:visibility="gone" />

    <!-- Upload Button -->
    <Button
        android:id="@+id/uploadButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="📤 Upload Missão" />

    <!-- Start Button -->
    <Button
        android:id="@+id/startButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="▶️ Iniciar"
        android:enabled="false" />

    <!-- Pause Button -->
    <Button
        android:id="@+id/pauseButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="⏸️ Pausar"
        android:enabled="false" />

    <!-- Resume Button -->
    <Button
        android:id="@+id/resumeButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="▶️ Retomar"
        android:enabled="false" />

    <!-- Stop Button -->
    <Button
        android:id="@+id/stopButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="⏹️ Parar"
        android:enabled="false" />

</LinearLayout>
```

### Exemplo de Activity

```kotlin
class MissionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMissionBinding
    private val viewModel: MissionViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
        observeViewModel()
    }
    
    private fun setupListeners() {
        binding.uploadButton.setOnClickListener {
            val mission = loadMissionFromServer() // TODO: Implementar
            viewModel.prepareAndUploadMission(mission)
        }
        
        binding.startButton.setOnClickListener {
            viewModel.startMission()
        }
        
        binding.pauseButton.setOnClickListener {
            viewModel.pauseMission()
        }
        
        binding.resumeButton.setOnClickListener {
            viewModel.resumeMission()
        }
        
        binding.stopButton.setOnClickListener {
            viewModel.stopMission()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateUI(state)
                binding.statusText.text = "Status: ${state.javaClass.simpleName}"
            }
        }
        
        lifecycleScope.launch {
            viewModel.uiEvent.collect { event ->
                when (event) {
                    is UiEvent.ShowMessage -> {
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_SHORT).show()
                    }
                    is UiEvent.ShowError -> {
                        Snackbar.make(binding.root, "❌ ${event.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    
    private fun updateUI(state: MissionUiState) {
        val isLoading = state.isLoading()
        val isExecuting = state.isExecuting()
        val isReady = state.isReadyToStart()
        
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.uploadButton.isEnabled = !isLoading && !isExecuting
        binding.startButton.isEnabled = isReady
        binding.pauseButton.isEnabled = isExecuting && state !is MissionUiState.Paused
        binding.resumeButton.isEnabled = state is MissionUiState.Paused
        binding.stopButton.isEnabled = isExecuting
    }
}
```

---

## 🧪 Passo 4: Testes

### Test Setup

```kotlin
class MissionViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var mockMissionManager: DroneMissionManager
    private lateinit var viewModel: MissionViewModel
    
    @Before
    fun setup() {
        mockMissionManager = mockk(relaxed = true)
        viewModel = MissionViewModel(mockMissionManager)
    }
    
    @Test
    fun `uploadMission com sucesso deve mudar estado`() = runTest {
        coEvery {
            mockMissionManager.prepareAndUploadMission(any())
        } answers {
            // Simular sucesso
        }
        
        val mission = createMockMission()
        viewModel.prepareAndUploadMission(mission)
        
        // Verificar estado
        assertEquals(MissionUiState.ReadyToExecute, viewModel.uiState.value)
    }
    
    @Test
    fun `uploadMission com erro deve emitir evento`() = runTest {
        coEvery {
            mockMissionManager.prepareAndUploadMission(any())
        } throws IllegalArgumentException("Waypoint inválido")
        
        val mission = createMockMission()
        viewModel.prepareAndUploadMission(mission)
        
        // Verificar evento
        val event = viewModel.uiEvent.replayCache.first()
        assertTrue(event is UiEvent.ShowError)
    }
}
```

---

## 📊 Passo 5: Logging e Debug

### Ativar Verbose Logging

```kotlin
// Antes de criar o DroneMissionManager
if (BuildConfig.DEBUG) {
    // Enable verbose logging
    System.setProperty("dji.debug.verbose", "true")
}
```

### Monitorar via Logcat

```bash
# Terminal
adb logcat | grep "DroneMissionManager"

# Ou no Android Studio:
# Logcat → Filter → "DroneMissionManager"
```

### Exemplo de Log Output

```
✅ Drone conectado: Mavic 2 Pro
✅ Mission Listener adicionado
📤 Iniciando upload da missão...
✅ Missão carregada com sucesso (5 waypoints)
⬆️ Upload em progresso...
✅ Upload da missão concluído!
✅ Pronto para executar
▶️ Iniciando missão...
▶️ Missão iniciada
⬆️ Download: 0/5
⬆️ Download: 5/5
✅ Download concluído
```

---

## 🔧 Passo 6: Configuração de Constantes

### Ajustar Timeouts para seu Drone

```kotlin
// DroneMissionManager.kt

companion object {
    // Increase para drones mais lentos
    private const val UPLOAD_TIMEOUT_MS = 45000L  // 45s ao invés de 30s
    
    // Decrease se quiser ser mais agressivo
    private const val START_TIMEOUT_MS = 5000L   // 5s ao invés de 10s
}
```

### Ajustar Limites de Velocidade

```kotlin
companion object {
    // Para Mavic 3
    private const val MAX_FLIGHT_SPEED_LIMIT = 28f
    
    // Para Phantom 4
    private const val MAX_FLIGHT_SPEED_LIMIT = 20f
}
```

---

## ⚠️ Passo 7: Tratamento de Erros

### Errors Possíveis e Soluções

```kotlin
try {
    missionManager.prepareAndUploadMission(mission)
} catch (e: IllegalArgumentException) {
    // Validação falhou
    Log.e(TAG, "Parâmetros inválidos: ${e.message}")
    showUserError("Missão inválida: ${e.message}")
    
} catch (e: DJIMissionException) {
    // Erro do SDK
    Log.e(TAG, "Erro DJI: ${e.message}")
    if (e.message?.contains("timeout") == true) {
        showUserError("Timeout - Verifique conexão com o drone")
    } else {
        showUserError("Erro: ${e.message}")
    }
    
} catch (e: TimeoutCancellationException) {
    // Timeout
    Log.e(TAG, "Operação com timeout")
    showUserError("Operação demorou muito - Tente novamente")
    
} catch (e: Exception) {
    // Erro genérico
    Log.e(TAG, "Erro inesperado", e)
    showUserError("Erro inesperado: ${e.message}")
}
```

---

## 🎯 Checklist de Integração

- [ ] Dependencies adicionadas
- [ ] MissionViewModel ou Activity criada
- [ ] UI layout implementada
- [ ] Listeners de botão configurados
- [ ] Observação de estado implementada
- [ ] `destroy()` chamado em `onDestroy()`
- [ ] Testes unitários escritos
- [ ] Testado com simulador DJI
- [ ] Testado com drone real
- [ ] Logs verificados
- [ ] Timeout ajustado para seu drone
- [ ] Error handling implementado
- [ ] Documentação atualizada

---

## 🚀 Próximas Steps

1. **Testes Básicos** - Usar simulador DJI
2. **Testes de Integração** - Com drone real
3. **Otimização** - Ajustar timeouts
4. **Features** - Retry logic, persistence, etc
5. **Release** - Para production

