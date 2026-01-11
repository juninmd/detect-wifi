# Presence Detector - WiFi & Bluetooth Home Presence Detection

Um aplicativo Android moderno que detecta a presença de pessoas em casa através de análise de ondas WiFi, com fallback em Bluetooth. Desenvolvido em **Kotlin** com **Push Notifications** e **Serviço em Background**.

## 🎯 Características

- **✓ Detecção WiFi Primária**: Monitora redes WiFi e intensidade de sinais
- **✓ Fallback Bluetooth**: Sistema de detecção Bluetooth como backup
- **✓ Kotlin Moderno**: Código 100% em Kotlin com coroutines
- **✓ Serviço de Background**: Detecção contínua mesmo com app fechado
- **✓ Push Notifications**: Alertas em tempo real com Firebase Cloud Messaging
- **✓ Interface em Tempo Real**: Exibição ao vivo do status de detecção
- **✓ Log Detalhado**: Registro completo de eventos de detecção
- **✓ Suporte Multi-Android**: Compatível com Android 5.0+

## 📋 Requisitos

- Android 5.0 (API 21) ou superior
- Permissões WiFi ativadas
- Bluetooth habilitado (para detecção secundária)
- Localização ativada (necessária para varredura WiFi/BLE)

## 🚀 Como Usar

### 1. Instalação

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/detect-wifi.git
cd detect-wifi

# Abra no Android Studio
open .
```

### 2. Configuração Firebase (Opcional)

Para ativar push notifications, configure o Firebase:

1. Acesse [Firebase Console](https://console.firebase.google.com/)
2. Crie um projeto novo
3. Adicione um app Android
4. Baixe o arquivo `google-services.json`
5. Coloque na pasta `app/`

Sem o Firebase, o app funciona normalmente com notificações locais.

### 3. Compilação

```bash
# Compile o projeto
./gradlew build

# Instale em um dispositivo/emulador
./gradlew installDebug
```

### 4. Execução

1. Abra o aplicativo "Presence Detector"
2. Conceda as permissões solicitadas
3. Clique em "Start Detection"
4. O app iniciará o serviço em background
5. Receberá push notifications quando houver mudança de presença
6. Verifique o log para detalhes completos
7. Clique em "Stop Detection" para parar

## 🔄 Fluxo de Funcionamento

```
┌─────────────────────────────────────┐
│      MainActivity (UI)               │
│  ✓ Controles Start/Stop             │
│  ✓ Exibição de status               │
│  ✓ Log em tempo real                │
└─────────────────┬───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────┐
│  DetectionBackgroundService         │
│  ✓ Roda mesmo com app fechado       │
│  ✓ Notificação de foreground        │
│  ✓ Gerencia ciclo de vida           │
└─────────────────┬───────────────────┘
                  │
         ┌────────┴────────┐
         ▼                  ▼
    ┌─────────┐      ┌──────────┐
    │  WiFi   │      │ Bluetooth│
    │Service  │      │ Service  │
    │(Primary)│      │(Fallback)│
    └────┬────┘      └────┬─────┘
         │                │
         └────────┬───────┘
                  ▼
    ┌─────────────────────────────┐
    │ PresenceDetectionManager    │
    │ ✓ Combina resultados        │
    │ ✓ Gerencia timeouts         │
    │ ✓ Dispara notificações      │
    └─────────────────────────────┘
                  │
         ┌────────┴────────┐
         ▼                  ▼
  ┌────────────┐     ┌──────────────┐
  │   Push     │     │   Local      │
  │Notification│     │Notification  │
  └────────────┘     └──────────────┘
```

## 🏗️ Arquitetura

### Serviços Principais

#### WiFiDetectionService.kt
- Realiza varredura periódica de redes WiFi
- Análise de intensidade de sinais para detectar presença
- Usa Kotlin Coroutines para operações assíncronas
- Threshold: -70 dBm (ajustável)

#### BluetoothDetectionService.kt
- Varredura BLE (Bluetooth Low Energy)
- Detecta dispositivos Bluetooth próximos
- Modo secundário/fallback

#### PresenceDetectionManager.kt
- Coordena WiFi e Bluetooth
- Avalia presença combinada com timeout de 30s
- Dispara notificações ao detectar mudanças

#### DetectionBackgroundService.kt
- **Serviço de Foreground**: Executa continuamente
- Mantém a detecção mesmo com app fechado
- Notificação persistente durante operação
- Gerencia ciclo de vida da detecção

### UI

- **MainActivity.kt**: Interface principal em Kotlin
- Layout responsivo com status visual
- Log em tempo real com auto-scroll
- Integração com Firebase Cloud Messaging

### Notificações

- **NotificationUtil.kt**: Gerencia todos os tipos de notificação
- Canais separados para alertas e status
- Vibração e luzes para presença detectada
- Compatibilidade com Android 8+

## 📝 Permissões Necessárias

```xml
<!-- WiFi -->
android.permission.ACCESS_WIFI_STATE
android.permission.CHANGE_WIFI_STATE
android.permission.ACCESS_FINE_LOCATION
android.permission.ACCESS_COARSE_LOCATION

<!-- Bluetooth -->
android.permission.BLUETOOTH
android.permission.BLUETOOTH_ADMIN
android.permission.BLUETOOTH_SCAN (Android 12+)
android.permission.BLUETOOTH_CONNECT (Android 12+)

<!-- Notificações -->
android.permission.POST_NOTIFICATIONS (Android 13+)

<!-- Serviço em Background -->
android.permission.FOREGROUND_SERVICE
```

## 🔔 Notificações Push

### Tipos de Notificações

**1. Notificação de Foreground (Contínua)**
- Mostra status atual de detecção
- Ativa enquanto o serviço está rodando
- Baixa prioridade (não atrapalha usuário)

**2. Notificação de Alerta (Push)**
- Dispara quando presença é detectada/removida
- Alta prioridade com vibração e luz
- Clicável para abrir o app

### Configuração de Canais

```kotlin
// Canal para status (contínuo)
NotificationChannel(
    "presence_detection_channel",
    "Presence Detection",
    IMPORTANCE_LOW
)

// Canal para alertas
NotificationChannel(
    "presence_alerts_channel",
    "Presence Alerts",
    IMPORTANCE_HIGH  // Vibração + luz
)
```

## 🚀 Serviço em Background

### Como Funciona

1. **Inicialização**: Clique em "Start Detection"
2. **Foreground Service**: App inicia `DetectionBackgroundService`
3. **Notificação Persistente**: Mostra status contínuo
4. **Operação Contínua**: Detecta presença mesmo com app fechado
5. **Parada**: Clique em "Stop Detection" para finalizar

### Diferencial

- ✅ **Funciona mesmo com app fechado**
- ✅ **Notificações em tempo real**
- ✅ **Não consome muita bateria** (WiFi scan a cada 5s)
- ✅ **Service sticky** (reinicia se morto pelo sistema)

### Comportamento do Serviço

```
User abre app
    ↓
Clica "Start Detection"
    ↓
MainActivity inicia DetectionBackgroundService
    ↓
Service inicia PresenceDetectionManager
    ↓
WiFi + Bluetooth scanning começam
    ↓
Notificação de foreground aparece
    ↓
User pode fechar o app
    ↓
Detecção CONTINUA rodando
    ↓
Ao detectar presença, envia notificação push
    ↓
User clica em "Stop Detection"
    ↓
Service para e notificação desaparece
```

## 🔧 Configuração Avançada

### Ajustar Threshold de Sinal WiFi

Em `WiFiDetectionService.java`:
```java
private static final int SIGNAL_THRESHOLD = -70; // dBm
```

Valores recomendados:
- `-60 dBm`: Sinal muito forte (próximo)
- `-70 dBm`: Sinal forte (padrão)
- `-80 dBm`: Sinal moderado (mais sensível)

### Intervalo de Varredura

```java
private static final int SCAN_INTERVAL = 5000; // 5 segundos
```

## 📊 Estrutura de Diretórios

```
detect-wifi/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/presencedetector/
│   │       │   ├── MainActivity.java
│   │       │   ├── services/
│   │       │   │   ├── WiFiDetectionService.java
│   │       │   │   ├── BluetoothDetectionService.java
│   │       │   │   └── PresenceDetectionManager.java
│   │       │   └── utils/
│   │       │       ├── LoggerUtil.java
│   │       │       └── PreferencesUtil.java
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml
│   │       │   ├── drawable/
│   │       │   └── values/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

## 🔍 Como Funciona

### Fluxo de Detecção WiFi

1. **Varredura Periódica**: A cada 5 segundos
2. **Análise de Sinais**: Verifica intensidade e quantidade de redes
3. **Limiar de Detecção**: Se sinais fortes detectados → Presença
4. **Notificação**: Atualiza UI e listeners

### Fluxo de Detecção Bluetooth

1. **BLE Scan**: Busca por dispositivos BLE
2. **RSSI Analysis**: Verifica força do sinal
3. **Device Detection**: Se dispositivo com sinal forte encontrado
4. **Fallback**: Ativa se WiFi não detectar presença

### Decisão Combinada

```
isPeoplePresent = (WiFi detected dentro de 30s) OR
                  (Bluetooth detected dentro de 30s)
```

## 🧪 Testes

### Teste Manual

1. **Teste WiFi**:
   - Ative o detector
   - Aproxime-se com outro celular/tablet
   - Verifique se detecta presença

2. **Teste Bluetooth**:
   - Desative WiFi, mantenha Bluetooth ativo
   - Aproxime dispositivo Bluetooth compatível
   - Confirme detecção secundária

3. **Teste de Timeout**:
   - Detecte presença
   - Afaste-se gradualmente
   - Verifique se status muda após 30s

## 📱 Compatibilidade de Dispositivos

| Versão Android | Status | Notas |
|---|---|---|
| Android 5.0-8.x | ✅ Completo | Funciona perfeitamente |
| Android 9-11 | ✅ Completo | WiFi + Bluetooth OK |
| Android 12+ | ✅ Completo | Requer permissões extras |
| Android 13+ | ✅ Completo | Sem alterações |

## 🐛 Troubleshooting

### WiFi não detecta presença
- Verifique se WiFi está ativado
- Confirme permissões de localização
- Aumente sensibilidade reduzindo threshold

### Bluetooth sem resposta
- Ative Bluetooth no dispositivo
- Verifique permissões (Android 12+)
- Confirme que dispositivos BLE estão próximos

### Crash ao iniciar
- Verifique permissões solicitadas
- Limpe build: `./gradlew clean`
- Reconstrua: `./gradlew build`

## 📚 Referências GitHub

Projeto baseado em:
- [WiFi Scanning Android Examples](https://github.com/android/samples)
- [BLE Scanner Projects](https://github.com/search?q=bluetooth+le+scanner+android)
- [Home Presence Detection](https://github.com/topics/presence-detection)

## 🤝 Contribuições

Contribuições são bem-vindas! Abra uma issue ou PR.

## 📄 Licença

MIT License - veja LICENSE.md para detalhes

## 👨‍💻 Autor

Desenvolvido como solução de automação residencial.

## 📞 Suporte

Para dúvidas ou problemas:
1. Verifique este README
2. Consulte o arquivo de troubleshooting
3. Abra uma issue no GitHub

---

**Última Atualização**: Janeiro 2026
**Versão**: 1.0
