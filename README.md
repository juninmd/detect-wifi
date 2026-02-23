# Presence Detector & Segurança Anti-Furto

[![CI/CD Pipeline](https://github.com/juninmd/detect-wifi/actions/workflows/ci.yml/badge.svg)](https://github.com/juninmd/detect-wifi/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/juninmd/detect-wifi/graph/badge.svg)](https://codecov.io/gh/juninmd/detect-wifi)

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
*   JDK 21.
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

2.  **Rodar Testes:**
    ```bash
    ./gradlew testDebugUnitTest
    ```

3.  **Verificar Cobertura:**
    ```bash
    ./gradlew jacocoTestReport jacocoTestCoverageVerification
    ```

4.  **Compilar o APK:**
    ```bash
    ./gradlew assembleDebug
    ```

5.  **Instalar:**
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

## 🔄 Pipeline CI/CD e Processo de Release

O projeto utiliza um pipeline robusto no GitHub Actions para garantir a qualidade do código e automatizar entregas.

### Estágios do Pipeline
1.  **Lint & Format:** Verifica a qualidade do código com Android Lint e formatação com Spotless.
2.  **Testes:** Executa testes unitários e valida a cobertura de código (mínimo 80%) via JaCoCo.
3.  **Testes Android:** Executa testes de interface (E2E) em um emulador Android.
4.  **Segurança:** Escaneia o código em busca de segredos expostos (Gitleaks) e vulnerabilidades em dependências (Trivy).
5.  **Build:** Compila os artefatos (APKs) para Debug e Release.
6.  **Deploy:** Automatiza a publicação no GitHub Releases.

### Tipos de Release

1.  **Release Automática (Staging/Nightly):**
    *   Gerada automaticamente a cada push na branch `main`.
    *   A versão segue o número da build do GitHub Actions (ex: `v1.1.42`).
    *   Inclui APKs de debug e release (não assinado por padrão).

2.  **Release de Produção (Stable):**
    *   Gerada ao criar uma tag Git (ex: `v1.2.0`).
    *   Inclui APK assinado (se as chaves estiverem configuradas).
    *   Para criar:
        ```bash
        git tag v1.2.0
        git push origin v1.2.0
        ```

### Configuração de Assinatura (Opcional)

Para gerar APKs assinados automaticamente, configure os seguintes **Secrets** no repositório GitHub (Settings -> Secrets and variables -> Actions):

*   `RELEASE_KEYSTORE_BASE64`: Conteúdo do arquivo keystore (.jks) codificado em Base64 (`base64 -w 0 seu-keystore.jks`).
*   `KEYSTORE_PASSWORD`: Senha do keystore.
*   `KEY_ALIAS`: Alias da chave.
*   `KEY_PASSWORD`: Senha da chave.

Se esses segredos não estiverem configurados, a release conterá um APK não assinado (ou assinado com debug key, dependendo da configuração local).

## 🤝 Contribuição

Por favor, leia `CONTRIBUTING.md` para padrões de código e diretrizes de CI/CD antes de submeter um Pull Request.

## 📄 Licença

Este projeto está licenciado sob a MIT License.
