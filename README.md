# Presence Detector & Segurança Anti-Furto

Um aplicativo Android abrangente que combina detecção de presença residencial (WiFi/Bluetooth/Câmera) com medidas ativas de proteção anti-furto. Desenvolvido com Kotlin moderno e otimizado para Android 15.

## 🌟 Principais Recursos

### 🏠 Monitor de Presença Residencial
*   **Radar WiFi:** Detecta presença analisando flutuações de sinal WiFi.
*   **Scanner Bluetooth:** Detecção secundária (fallback) usando dispositivos BLE.
*   **Monitoramento por Câmera:** Detecção visual de presença usando a câmera do dispositivo.
*   **Alertas Remotos:** Envia notificações via Telegram quando presença é detectada.

### 🛡️ Segurança Móvel (Anti-Furto)
*   **Alarme de Movimento:** Dispara se o dispositivo for movido.
*   **Modo Bolso:** Alarma se o dispositivo for removido do bolso/bolsa enquanto armado.
*   **Guarda de Carregador:** Alarma se o carregador for desconectado.
*   **Selfie de Intruso:** Captura uma foto do intruso após tentativas erradas de desbloqueio (requer configuração específica).
*   **Desarme Biométrico:** Requer Impressão Digital/FaceID para parar o alarme.

### 🔌 Integrações
*   **Bot Telegram:** Receba fotos e alertas diretamente no Telegram.

## 📱 Requisitos

*   **Android Mínimo:** Android 8.0 (Oreo) recomendado para melhor performance em segundo plano.
*   **Android Alvo:** Android 15 (Vanilla Ice Cream).
*   **Hardware:** WiFi, Bluetooth, Câmera, Acelerômetro, Sensor de Proximidade.

## 🚀 Instalação e Configuração

### 1. Pré-requisitos
*   Android Studio Koala ou mais recente.
*   JDK 17.
*   Android SDK Platform 35.

### 2. Instruções de Build

**Nota:** Este projeto requer uma configuração válida do Android SDK.

1.  **Configurar SDK:**
    Crie um arquivo `local.properties` na raiz do projeto se ele não existir:
    ```properties
    sdk.dir=/caminho/para/seu/android/sdk
    ```
    *(No Windows: `C:\\Users\\<Usuario>\\AppData\\Local\\Android\\Sdk`)*
    *(No Mac/Linux: `/Users/<Usuario>/Library/Android/sdk` ou `/usr/lib/android-sdk`)*

2.  **Compilar o APK:**
    ```bash
    ./gradlew assembleDebug
    ```

3.  **Instalar:**
    ```bash
    ./gradlew installDebug
    ```

### 3. Configuração do App

**Permissões:**
Ao iniciar pela primeira vez, você deve conceder:
*   **Localização:** "Permitir o tempo todo" é necessário para varredura WiFi/BLE em background.
*   **Notificações:** Necessário para status persistente do serviço e alertas.
*   **Câmera:** Para monitoramento e selfies de intruso.
*   **Sobreposição:** (Opcional) Para certos recursos visuais do alarme.

**Integrações:**
*   **Telegram:** Vá em Configurações -> Integrações -> Telegram. Insira seu Bot Token e Chat ID.

## 🛠️ Detalhes Técnicos

*   **Linguagem:** Kotlin 1.9.22
*   **Arquitetura:** Baseada em Serviços com Managers Singleton.
*   **Processamento em Background:** Usa Foreground Services com tipos `location` e `specialUse` para cumprir as restrições do Android 14+.
*   **Dependências:**
    *   `LibVLC` para processamento de vídeo.
    *   `AndroidX Biometric` para autenticação.
    *   `Coroutines` para tarefas assíncronas.

## 🤝 Contribuição

Por favor, leia `gemini.md` para padrões de código e boas práticas antes de submeter um Pull Request.

## 📄 Licença

Este projeto está licenciado sob a MIT License.
