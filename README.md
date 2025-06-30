![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF?style=for-the-badge&logo=kotlin)

# DJI Drone Control App

![License](https://img.shields.io/badge/license-MIT-blue.svg?style=for-the-badge)
![Status](https://img.shields.io/badge/status-Em%20Desenvolvimento-yellow.svg?style=for-the-badge)

Um aplicativo Android robusto para controle de drones DJI, com streaming de vídeo em tempo real, detecção de rostos via ML Kit e modos de voo manual e automático.

## 📖 Índice

- [Visão Geral](#-visão-geral)
- [Arquitetura e Tecnologias](#-arquitetura-e-tecnologias)
- [Pré-requisitos](#-pré-requisitos)
- [Como Instalar e Buildar](#-como-instalar-e-buildar)
- [Como Usar](#-como-usar)
- [Como Contribuir](#-como-contribuir)
- [Licença](#-licença)

## 🌟 Visão Geral

Este projeto é um aplicativo Android construído em Kotlin que utiliza o SDK da DJI para criar uma solução completa de controle de drones. O aplicativo é dividido em três telas principais para uma experiência de usuário clara e funcional.

### Telas Principais

1.  **Tela Principal / Conexão (`MainActivity`)**
    * Gerencia a conexão com o drone.
    * Exibe o status da conexão (Conectado, Procurando, Desconectado).
    * Mostra telemetria básica: bateria do drone, sinal de rádio e GPS.
    * Navegação para as telas de Voo em Tempo Real e Painel de Controle.

2.  **Voo em Tempo Real / FPV (`VideoFeedActivity`)**
    * Exibe o feed de vídeo ao vivo da câmera do drone.
    * Utiliza o ML Kit do Google para detectar rostos em tempo real.
    * Desenha retângulos sobre os rostos detectados em uma camada de sobreposição (`OverlayView`).
    * Permite ativar/desativar a detecção e salvar automaticamente imagens dos rostos encontrados.
    * Exibe um indicador visual ("Rosto salvo!") ao capturar uma imagem.

3.  **Painel de Controle (`ControlActivity`)**
    * Oferece controle total sobre o drone através de duas abas:
        * **Manual:** Inclui joysticks virtuais para controle de movimento, além de botões para decolar, pousar e retornar para casa (RTH).
        * **Automático:** Permite executar missões pré-programadas, como orbitar um ponto ou seguir um alvo.

## 🏗️ Arquitetura e Tecnologias

O projeto é estruturado em pacotes por funcionalidade para garantir clareza, independência e reutilização de código.

### Tecnologias Utilizadas

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **Plataforma:** Android
* **SDKs:**
    * [DJI Mobile SDK](https://developer.dji.com/mobile-sdk/) - Para comunicação e controle do drone.
    * [Google ML Kit (Vision)](https://developers.google.com/ml-kit/vision/face-detection) - Para detecção de rostos.
* **Arquitetura:** Model-View-ViewModel (MVVM) implícito com separação de responsabilidades.

### Estrutura dos Pacotes

```
com.seuprojeto.droneapp/
│
├── activities/       # (UI) Telas e lógica de interface do usuário
│   ├── MainActivity.kt
│   ├── VideoFeedActivity.kt
│   └── ControlActivity.kt
│
├── dji/              # Lógica de conexão e comunicação com o drone
│   ├── DJIConnectionHelper.kt
│   └── DroneTelemetryManager.kt
│
├── vision/           # Módulos de visão computacional
│   ├── FaceDetectionProcessor.kt
│   ├── OverlayView.kt
│   └── ImageSaver.kt
│
├── controls/         # Componentes e lógica de controle de voo
│   ├── VirtualJoystickView.kt
│   └── MissionManager.kt
│
└── utils/            # Classes utilitárias
└── PermissionHelper.kt
```

## 📦 Pré-requisitos

Antes de começar, certifique-se de que você tem o seguinte:

* [Android Studio](https://developer.android.com/studio) (versão mais recente recomendada).
* Um drone DJI compatível com o Mobile SDK (ex: Mavic 3).
* Uma conta de desenvolvedor DJI e uma **App Key** gerada no [DJI Developer Center](https://developer.dji.com/).
* Um dispositivo Android físico para testar o aplicativo.

## 🚀 Como Instalar e Buildar

Siga os passos abaixo para configurar e executar o projeto:

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/seu-usuario/seu-repositorio.git](https://github.com/seu-usuario/seu-repositorio.git)
    ```

2.  **Abra no Android Studio:**
    * Abra o Android Studio.
    * Selecione `Open an existing project` e navegue até a pasta do projeto clonado.

3.  **Adicione sua Chave da API DJI:**
    * Abra o arquivo `app/src/main/AndroidManifest.xml`.
    * Encontre a linha que requer a chave da DJI e substitua `YOUR_DJI_APP_KEY` pela sua chave:
      ```xml
      <meta-data
          android:name="com.dji.sdk.API_KEY"
          android:value="YOUR_DJI_APP_KEY" />
      ```

4.  **Sincronize e builde o projeto:**
    * O Android Studio deve sincronizar o Gradle automaticamente. Se não, clique em `File -> Sync Project with Gradle Files`.
    * Clique em `Build -> Make Project` para compilar o código.

5.  **Execute o aplicativo:**
    * Conecte seu dispositivo Android.
    * Selecione o dispositivo e clique no botão `Run 'app'` (▶️).

## 🎮 Como Usar

1.  **Conexão:** Inicie o aplicativo e siga as instruções na tela para se conectar ao seu drone DJI. O status da conexão será exibido na tela principal.
2.  **Voo em Tempo Real:** Toque no botão **"VOO EM TEMPO REAL"**. Você verá o feed de vídeo do drone. Use o botão de alternância para ativar/desativar a detecção de rostos. As imagens salvas serão armazenadas no seu dispositivo.
3.  **Painel de Controle:** Toque no botão **"PAINEL DE CONTROLE"**.
    * Na aba **"MANUAL"**, use os joysticks virtuais para pilotar o drone.
    * Na aba **"AUTOMÁTICO"**, selecione e inicie uma das missões disponíveis.

## 🤝 Como Contribuir

Contribuições são sempre bem-vindas! Se você deseja contribuir com este projeto, siga estes passos:

1.  **Faça um Fork** do projeto.
2.  **Crie uma Branch** para sua nova feature (`git checkout -b feature/nova-feature`).
3.  **Faça o Commit** de suas alterações (`git commit -m 'Adiciona nova feature'`).
4.  **Faça o Push** para a sua branch (`git push origin feature/nova-feature`).
5.  **Abra um Pull Request**.

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.