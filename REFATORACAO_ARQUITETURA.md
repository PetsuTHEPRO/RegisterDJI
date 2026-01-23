# 🏗️ REFATORAÇÃO DA ARQUITETURA - Clean Architecture

## 📋 PROBLEMAS IDENTIFICADOS

### ❌ **1. AUTH - Inconsistências**
```
auth/
├── data/
│   ├── model/          ❌ ERRADO: model deveria estar em domain
│   │   ├── LoginRequest.kt    ❌ DTO de API (deveria estar em data/remote/dto)
│   │   ├── LoginResponse.kt   ❌ DTO de API (deveria estar em data/remote/dto)
│   │   ├── RegisterRequest.kt ❌ DTO de API
│   │   ├── RegisterResponse.kt ❌ DTO de API
│   │   └── User.kt            ❌ Entidade de domínio (deveria estar em domain/model)
│   └── repository/
│       └── AuthRepository.kt   ✅ OK
└── ui/                         ❌ ERRADO: deveria ser "presentation"
    ├── LoginScreen.kt
    └── RegisterScreen.kt
```

**Falta:** domain/, domain/model/, domain/usecase/, data/remote/, presentation/viewmodel/

---

### ❌ **2. FACEDETECTION - Melhor estruturada, mas com problemas**
```
facedetection/
├── data/
│   ├── local/                  ✅ OK
│   ├── repository/             ✅ OK
│   └── vision/                 ❌ CONFUSO: deveria ser "detector" ou "ml"
├── domain/
│   ├── model/                  ✅ OK
│   ├── service/                ❌ CONFUSO: service não é padrão Clean Arch (deveria ser usecase)
│   └── usecase/                ✅ OK
└── ui/                         ❌ ERRADO: deveria ser "presentation"
```

**Falta:** presentation/viewmodel separado de ui/

---

### ❌ **3. MISSION - A PIOR DE TODAS (Maior problema)**
```
mission/
├── data/
│   ├── drone/                  ❌ MISTURADO: Manager + State + Telemetry juntos
│   │   ├── DroneControllerManager.kt   ✅ Manager (OK aqui)
│   │   ├── DroneMissionManager.kt      ✅ Manager (OK aqui)
│   │   ├── DroneState.kt               ❌ ERRADO: deveria estar em domain/model
│   │   └── DroneTelemetry.kt           ❌ ERRADO: deveria estar em domain/model
│   ├── mapper/                 ✅ OK (mas poderia estar em data/remote)
│   ├── model/                  ❌ ERRADO: ServerMission é DTO (data/remote/dto)
│   ├── network/                ✅ OK (mas deveria ser "remote")
│   ├── repository/             ✅ OK
│   └── sdk/                    ✅ OK
├── domain/
│   └── Mission.kt              ❌ ERRADO: arquivo solto sem pacote model/
├── presentation/
│   └── MissionViewModel.kt     ✅ OK (mas tem outro igual em ui/)
└── ui/                         ❌ BAGUNÇA TOTAL
    ├── AboutActivity.kt        ❌❌❌ "About" não é "Mission"!
    ├── AboutScreen.kt          ❌❌❌ Deveria estar em settings/
    ├── MainActivity.kt         ❌❌❌ MainActivity não deveria estar em feature
    ├── MissionViewModel.kt     ❌❌❌ DUPLICADO (já existe em presentation/)
    ├── MissionUiState.kt       ✅ OK (mas deveria estar com ViewModel)
    ├── component/              ✅ OK
    └── theme/                  ❌ ERRADO: Theme não deveria estar em feature
```

**Problemas graves:**
- 2 ViewModels com mesmo nome em lugares diferentes
- MainActivity dentro de feature (deveria estar em root)
- AboutActivity em mission (deveria estar em settings ou app)
- Theme dentro de feature
- Models misturados (DroneState/Telemetry em data em vez de domain)

---

### ❌ **4. SETTINGS - Incompleto**
```
settings/
└── ui/                         ❌ ERRADO: sem data, domain, presentation
    ├── SettingsActivity.kt
    ├── SettingsScreen.kt
    └── SettingsViewModel.kt    ❌ ERRADO: ViewModel em ui/
```

**Falta:** Tudo! data/, domain/, presentation/

---

### ❌ **5. VISION - Arquivo Solto**
```
vision/
└── FaceAnalyzer.kt             ❌ ERRADO: deveria estar em facedetection/
```

---

## ✅ ESTRUTURA PROPOSTA (Clean Architecture)

```
features/
├── auth/
│   ├── data/
│   │   ├── local/              (se tiver cache/db)
│   │   ├── remote/
│   │   │   ├── api/
│   │   │   │   └── AuthApi.kt
│   │   │   └── dto/
│   │   │       ├── LoginRequestDto.kt
│   │   │       ├── LoginResponseDto.kt
│   │   │       ├── RegisterRequestDto.kt
│   │   │       └── RegisterResponseDto.kt
│   │   ├── mapper/
│   │   │   └── UserMapper.kt
│   │   └── repository/
│   │       └── AuthRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   └── User.kt
│   │   ├── repository/
│   │   │   └── AuthRepository.kt (interface)
│   │   └── usecase/
│   │       ├── LoginUseCase.kt
│   │       └── RegisterUseCase.kt
│   └── presentation/
│       ├── login/
│       │   ├── LoginScreen.kt
│       │   ├── LoginViewModel.kt
│       │   └── LoginUiState.kt
│       └── register/
│           ├── RegisterScreen.kt
│           ├── RegisterViewModel.kt
│           └── RegisterUiState.kt
│
├── facedetection/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/
│   │   │   │   └── FaceDao.kt
│   │   │   ├── database/
│   │   │   │   └── FaceDatabase.kt
│   │   │   └── entity/
│   │   │       └── FaceEntity.kt
│   │   ├── detector/           (renomear vision/ para detector/)
│   │   │   ├── FaceRecognitionManager.kt
│   │   │   └── FaceAnalyzer.kt (mover de vision/)
│   │   ├── mapper/
│   │   │   ├── Converters.kt
│   │   │   └── FaceMapper.kt
│   │   └── repository/
│   │       └── FaceRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   └── FaceResult.kt
│   │   ├── repository/
│   │   │   └── FaceRepository.kt (interface)
│   │   └── usecase/
│   │       ├── CaptureFaceUseCase.kt
│   │       ├── SaveFaceUseCase.kt
│   │       └── RegisterFaceUseCase.kt (renomear service/)
│   └── presentation/
│       ├── recognition/
│       │   ├── FaceRecognitionScreen.kt
│       │   └── FaceRecognitionViewModel.kt
│       ├── registration/
│       │   └── FaceRegistrationActivity.kt (migrar para Compose)
│       └── registered/
│           ├── RegisteredFacesActivity.kt (migrar para Compose)
│           └── RegisteredFacesScreen.kt
│
├── mission/
│   ├── data/
│   │   ├── drone/              (managers OK aqui)
│   │   │   ├── manager/
│   │   │   │   ├── DroneControllerManager.kt
│   │   │   │   └── DroneMissionManager.kt
│   │   │   └── sdk/
│   │   │       └── DJIConnectionHelper.kt (mover de sdk/)
│   │   ├── remote/
│   │   │   ├── dto/
│   │   │   │   ├── ServerMissionDto.kt (renomear ServerMission)
│   │   │   │   └── ServerMissionCommandDto.kt
│   │   │   └── websocket/
│   │   │       └── MissionWebSocketListener.kt
│   │   ├── mapper/
│   │   │   └── ServerMissionMapper.kt
│   │   └── repository/
│   │       └── MissionRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Mission.kt (mover do domain raiz)
│   │   │   ├── DroneState.kt (mover de data/drone/)
│   │   │   └── DroneTelemetry.kt (mover de data/drone/)
│   │   ├── repository/
│   │   │   └── MissionRepository.kt (interface)
│   │   └── usecase/
│   │       ├── CreateMissionUseCase.kt
│   │       ├── ExecuteMissionUseCase.kt
│   │       ├── GetMissionsUseCase.kt
│   │       └── SyncMissionUseCase.kt
│   └── presentation/
│       ├── control/
│       │   ├── MissionControlActivity.kt
│       │   ├── MissionControlScreen.kt
│       │   ├── MissionControlViewModel.kt
│       │   └── MissionControlUiState.kt
│       ├── create/
│       │   ├── MissionCreateScreen.kt
│       │   └── MissionCreateViewModel.kt
│       ├── dashboard/
│       │   ├── DashboardScreen.kt
│       │   └── DashboardViewModel.kt
│       ├── drone/
│       │   ├── camera/
│       │   │   ├── DroneCameraScreen.kt
│       │   │   └── DroneCameraViewModel.kt
│       │   └── control/
│       │       ├── DroneControlScreen.kt
│       │       └── DroneControlViewModel.kt
│       ├── list/
│       │   ├── MissionsTableScreen.kt
│       │   └── MissionsViewModel.kt (renomear MissionViewModel)
│       ├── video/
│       │   └── VideoFeedActivity.kt (migrar para Compose)
│       ├── welcome/
│       │   └── WelcomeScreen.kt
│       ├── component/
│       │   ├── FaceOverlayView.kt
│       │   ├── MapboxMapView.kt
│       │   ├── SyncStatusBar.kt
│       │   └── VideoFeedView.kt
│       └── shared/
│           └── MissionUiState.kt
│
├── settings/
│   ├── data/
│   │   ├── local/
│   │   │   └── SettingsDataStore.kt
│   │   └── repository/
│   │       └── SettingsRepositoryImpl.kt
│   ├── domain/
│   │   ├── model/
│   │   │   └── AppSettings.kt
│   │   ├── repository/
│   │   │   └── SettingsRepository.kt (interface)
│   │   └── usecase/
│   │       ├── GetSettingsUseCase.kt
│   │       └── UpdateSettingsUseCase.kt
│   └── presentation/
│       ├── about/              (MOVER AboutActivity para aqui)
│       │   ├── AboutScreen.kt
│       │   └── AboutViewModel.kt
│       └── settings/
│           ├── SettingsActivity.kt
│           ├── SettingsScreen.kt
│           └── SettingsViewModel.kt
```

---

## 📦 ARQUIVOS A MOVER/CRIAR

### 🔴 MOVER URGENTE

1. **auth/**
   - ✅ `User.kt` → `domain/model/User.kt`
   - ✅ `Login/RegisterRequest/Response.kt` → `data/remote/dto/`
   - ✅ `ui/` → `presentation/`

2. **facedetection/**
   - ✅ `data/vision/` → `data/detector/`
   - ✅ `domain/service/` → `domain/usecase/`
   - ✅ `ui/` → `presentation/`

3. **mission/** (CRÍTICO)
   - ✅ `DroneState.kt` → `domain/model/DroneState.kt`
   - ✅ `DroneTelemetry.kt` → `domain/model/DroneTelemetry.kt`
   - ✅ `domain/Mission.kt` → `domain/model/Mission.kt`
   - ✅ `data/model/ServerMission.kt` → `data/remote/dto/ServerMissionDto.kt`
   - ✅ `data/network/` → `data/remote/websocket/`
   - ✅ `data/sdk/` → `data/drone/sdk/`
   - ✅ `presentation/MissionViewModel.kt` REMOVER (duplicado)
   - ✅ `ui/AboutActivity.kt` → `settings/presentation/about/`
   - ✅ `ui/MainActivity.kt` → MOVER PARA ROOT (não feature)
   - ✅ `ui/theme/` → MOVER PARA ROOT `ui/theme/`
   - ✅ Organizar todos os ViewModels em pastas específicas

4. **settings/**
   - ✅ Criar estrutura completa: data/, domain/, presentation/

5. **vision/**
   - ✅ `FaceAnalyzer.kt` → `facedetection/data/detector/`

---

## 🎯 BENEFÍCIOS DA REORGANIZAÇÃO

1. ✅ **Separação clara de responsabilidades** (Clean Architecture)
2. ✅ **Navegação intuitiva** no código
3. ✅ **Escalabilidade** - fácil adicionar novas features
4. ✅ **Testabilidade** - camadas independentes
5. ✅ **Manutenibilidade** - cada arquivo no lugar certo
6. ✅ **Consistência** - todas features seguem mesmo padrão

---

## 📝 PRÓXIMOS PASSOS

1. ✅ Criar estrutura de pastas nova
2. ✅ Mover arquivos (com git mv para preservar histórico)
3. ✅ Atualizar imports
4. ✅ Testar compilação
5. ✅ Commitar mudanças

---

**Quer que eu implemente essa reorganização agora?**
