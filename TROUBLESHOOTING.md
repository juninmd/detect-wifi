# Troubleshooting Guide - Presence Detector

## 🔧 Problemas e Soluções

### Compilação

#### "Cannot resolve symbol 'R'"
```
❌ Problema: Classe R não encontrada
✅ Solução:
   1. File → Invalidate Caches / Restart
   2. Build → Clean Project
   3. Build → Rebuild Project
   4. Sincronizar com Gradle
```

#### "Unresolved reference: 'kotlin'"
```
❌ Problema: Kotlin plugin não encontrado
✅ Solução:
   1. File → Project Structure → Plugins
   2. Instalar "Kotlin" plugin
   3. Reiniciar Android Studio
```

#### "Duplicate class com.example.presencedetector"
```
❌ Problema: Arquivos .java e .kt duplicados
✅ Solução:
   1. Deletar arquivo .java antigo
   2. Manter apenas arquivo .kt
   3. Build → Clean Project
```

#### Erro com Firebase
```
❌ Problema: "Could not find com.google.gms:google-services"
✅ Solução:
   1. Verificar build.gradle raiz:
      classpath 'com.google.gms:google-services:4.4.0'
   2. Verificar app/build.gradle:
      id 'com.google.gms.google-services'
   3. Limpar .gradle:
      rm -rf ~/.gradle
   4. Rebuild
```

#### "Missing google-services.json"
```
❌ Problema: Aviso sobre google-services.json
✅ Solução (ESCOLHA UM):

   Opção A - Com Firebase:
   1. Download google-services.json do Firebase Console
   2. Copiar para app/google-services.json
   3. Rebuild

   Opção B - Sem Firebase:
   1. OK ignorar o aviso
   2. App funciona com notificações locais
   3. Criar arquivo dummy:
      {}  // app/google-services.json
```

---

### Execução

#### App não inicia / Crash na abertura
```
❌ Problema: App fecha imediatamente
✅ Solução:
   1. Verificar Logcat (Ctrl+6):
      - Ver mensagem de erro
      - Procurar "Exception" ou "Error"

   2. Permissões negadas:
      - Ir a Configurações → Apps → Presence Detector
      - Verificar todas as permissões
      - Ativar tudo

   3. Dispositivo incompatível:
      - Verificar minSdk em app/build.gradle
      - Android 5.0 (API 21) ou superior necessário

   4. Cache corrupto:
      - File → Invalidate Caches / Restart
      - ./gradlew clean build
```

#### "Permission denied" durante execução
```
❌ Problema: Runtime permissions não concedidas
✅ Solução:
   1. Na primeira abertura, aceitar todas as permissões
   2. Se negar:
      - Settings → Apps → Presence Detector
      - Permissions → Ativar tudo

   Permissões necessárias:
   ☑ WiFi
   ☑ Bluetooth
   ☑ Location (ACCESS_FINE_LOCATION)
   ☑ Notifications (Android 13+)
```

#### App não instala em dispositivo real
```
❌ Problema: Erro ao instalar via "Run"
✅ Solução:
   1. Conectar via USB:
      - Ativar USB Debugging (Configurações → Developer Options)
      - Permitir acesso em pop-up do dispositivo

   2. Verificar via ADB:
      adb devices
      # Deve mostrar seu dispositivo

   3. Desinstalar versão anterior:
      adb uninstall com.example.presencedetector

   4. Instalar novamente:
      ./gradlew installDebug
```

---

### Detecção

#### WiFi não detecta presença
```
❌ Problema: WiFi sempre mostra "No networks found"
✅ Solução:

1️⃣ Verificar WiFi ativo:
   - Settings → WiFi → ON

2️⃣ Verificar localização:
   - Settings → Location → ON
   - Permission: ACCESS_FINE_LOCATION
   - (WiFi/BLE scan requer localização)

3️⃣ Verificar timeout:
   - Precisa de redes WiFi por perto
   - Aproximar com outro celular
   - Ou em local com muitas redes

4️⃣ Aumentar sensibilidade:
   - Editar WiFiDetectionService.kt
   - Mudar: SIGNAL_THRESHOLD = -70
   - Para: SIGNAL_THRESHOLD = -75 (mais sensível)
```

#### Bluetooth não funciona
```
❌ Problema: Bluetooth mostra "Not available"
✅ Solução:

1️⃣ Bluetooth ativo:
   - Settings → Bluetooth → ON

2️⃣ Permissões:
   - Android 12+: BLUETOOTH_SCAN + BLUETOOTH_CONNECT
   - Settings → Apps → Presence Detector → Permissions

3️⃣ Dispositivo próximo:
   - Aproximar smartphone/tablet com BLE
   - Watchs, earbuds, etc também funcionam

4️⃣ Aumentar sensibilidade:
   - Editar BluetoothDetectionService.kt
   - Mudar: SIGNAL_THRESHOLD = -70
   - Para: SIGNAL_THRESHOLD = -75
```

#### Sempre detecta presença (falso positivo)
```
❌ Problema: Status sempre "PEOPLE DETECTED"
✅ Solução:

1️⃣ Reduzir sensibilidade:
   WiFiDetectionService.kt
   SIGNAL_THRESHOLD = -60 (menos sensível)

2️⃣ Aumentar timeout:
   PresenceDetectionManager.kt
   DETECTION_TIMEOUT = 60000L (60 segundos)

3️⃣ Verificar proximidade:
   - Afastar-se de redes WiFi conhecidas
   - Desligar Bluetooth de outros dispositivos
```

#### Nunca detecta presença (falso negativo)
```
❌ Problema: Status sempre "No one home"
✅ Solução:

1️⃣ Aumentar sensibilidade:
   WiFiDetectionService.kt
   SIGNAL_THRESHOLD = -80 (mais sensível)

2️⃣ Reduzir timeout:
   PresenceDetectionManager.kt
   DETECTION_TIMEOUT = 15000L (15 segundos)

3️⃣ Verificar intervalo de scan:
   WiFiDetectionService.kt
   SCAN_INTERVAL = 3000L (3 segundos)

4️⃣ Trazer dispositivo muito perto (< 5m)
```

---

### Notificações

#### Notificações não aparecem
```
❌ Problema: Push notifications não chegam
✅ Solução:

1️⃣ Verificar canais:
   - Settings → Apps → Presence Detector → Notifications
   - "Presence Detection" → Ativado
   - "Presence Alerts" → Ativado

2️⃣ Modo silencioso:
   - Desativar silencioso
   - Volume do sistema >= 1

3️⃣ Verificar Firebase (se configurado):
   - Firebase Console → Cloud Messaging
   - Server Key disponível
   - Token do dispositivo sendo registrado

4️⃣ Verificar permissão:
   - Android 13+: POST_NOTIFICATIONS permission
   - Settings → Permissions → Notifications → Allow

5️⃣ Ver logs:
   adb logcat | grep "Notification"
   adb logcat | grep "FCM"
```

#### Notificação de foreground não aparece
```
❌ Problema: Notificação persistente do serviço não visível
✅ Solução:

1️⃣ Verificar canal:
   - Settings → Apps → Presence Detector → Notifications
   - Channel "Presence Detection" existe
   - Não silenciado

2️⃣ Verificar serviço:
   - Adb logcat | grep "Background"
   - Deve mostrar "startForeground"

3️⃣ Reimiciar detecção:
   - Stop Detection
   - Start Detection novamente

4️⃣ Verificar Android version:
   - Android 8+: obrigatório foreground
   - Android 7-: pode não aparecer
```

#### Notificações muito frequentes
```
❌ Problema: Spam de notificações
✅ Solução:

✓ Comportamento normal!
  App limita a 1 notificação a cada 30 segundos

Se continuar recebendo muitas:
1️⃣ Aumentar DETECTION_TIMEOUT
   PresenceDetectionManager.kt
   DETECTION_TIMEOUT = 60000L

2️⃣ Aumentar lastNotificationTime
   Editar função sendNotification()
   if (now - lastNotificationTime < 60000)
```

---

### Serviço em Background

#### Detecção para quando app fecha
```
❌ Problema: Presença não detectada com app fechado
✅ Solução:

1️⃣ Verificar se serviço iniciou:
   - Ao clicar "Start", notificação aparece
   - Se não aparece → erro

2️⃣ Verificar permissões:
   - FOREGROUND_SERVICE permission
   - Settings → Apps → Presence Detector

3️⃣ Verificar logs:
   adb logcat | grep "BackgroundService"
   Deve mostrar: "startForeground"

4️⃣ Testar padrão:
   1. Start Detection
   2. Notificação aparece
   3. Pressionar Home (app para background)
   4. Notificação continua visível
   5. Aproximar outro celular
   6. Notificação de alerta chega
```

#### Sistema mata o serviço (Battery Saver)
```
❌ Problema: Serviço para com Battery Saver ativo
✅ Solução:

1️⃣ Desativar Battery Saver:
   - Settings → Battery → Battery Saver → OFF

2️⃣ Whitelist o app:
   - Settings → Battery Saver → Whitelist
   - Adicionar "Presence Detector"

3️⃣ Desativar otimizações:
   - Settings → Apps → Presence Detector
   - Battery → Optimize Battery Usage
   - Trocar para "Don't Optimize"
```

---

### Performance

#### App consome muita bateria
```
❌ Problema: Bateria drena rapidamente
✅ Solução:

1️⃣ Aumentar intervalo de scan:
   WiFiDetectionService.kt
   SCAN_INTERVAL = 10000L (em vez de 5000)

2️⃣ Reduzir scan duration:
   BluetoothDetectionService.kt
   SCAN_DURATION = 3000L (em vez de 5000)

3️⃣ Aumentar timeout:
   PresenceDetectionManager.kt
   DETECTION_TIMEOUT = 60000L

4️⃣ Monitorar uso:
   - Settings → Battery → Battery Usage
   - Presence Detector não deve estar top
```

#### App trava ou fica lento
```
❌ Problema: Interface congelada
✅ Solução:

1️⃣ Verificar threads:
   Logcat procurar por "ANR" ou "timeout"

2️⃣ Reduzir atualização de log:
   MainActivity.kt → addLog() é muito frequente?

3️⃣ Monitorar memória:
   Android Studio → Profiler
   Memory não deve crescer > 100MB

4️⃣ Limpar cache:
   Settings → Apps → Presence Detector → Storage → Clear Cache
```

---

### Database/Storage

#### Erro ao salvar logs
```
❌ Problema: Logs não salvam em arquivo
✅ Solução:

1️⃣ Verificar permissão:
   - WRITE_EXTERNAL_STORAGE (Android < 10)
   - READ_EXTERNAL_STORAGE (Android < 10)

2️⃣ Verificar espaço:
   - Settings → Storage → Espaço disponível
   - Precisa de pelo menos 10 MB

3️⃣ Verificar diretório:
   /sdcard/presence_detector_logs/

4️⃣ Debug:
   adb shell
   ls -la /sdcard/presence_detector_logs/
```

---

### Firebase/Cloud

#### Firebase não conecta
```
❌ Problema: Erro ao inicializar Firebase
✅ Solução:

1️⃣ Verificar google-services.json:
   - Arquivo em app/google-services.json
   - Conteúdo válido (não vazio)

2️⃣ Verificar conectividade:
   - Celular conectado ao WiFi
   - Internet funcionando

3️⃣ Verificar credenciais:
   - Projeto Firebase ativo
   - App Android registrado
   - SHA-1 correto

4️⃣ Verificar versão:
   - firebase-bom: 32.7.0
   - google-services plugin: 4.4.0
```

#### Token FCM não gerado
```
❌ Problema: Push notifications não chegam via Firebase
✅ Solução:

1️⃣ Verificar Internet:
   - Conectado ao WiFi/dados
   - Google services disponível

2️⃣ Verificar app:
   - Aberto pela primeira vez
   - Permissões concedidas

3️⃣ Debug token:
   Adicionar em PresenceDetectorApp.kt:
   ```kotlin
   FirebaseMessaging.getInstance().token
       .addOnCompleteListener { task ->
           Log.d("FCM", "Token: ${task.result}")
       }
   ```

4️⃣ Ver em Logcat:
   adb logcat | grep "FCM"
```

---

## 📞 Quando Tudo Falha

### Opção 1: Limpeza Completa
```bash
# Remover tudo Android
rm -rf ~/.android ~/.gradle
rm -rf build
./gradlew clean

# Reconstruir
./gradlew build
./gradlew installDebug
```

### Opção 2: Reset do Emulador
```bash
# Listar emuladores
emulator -list-avds

# Deletar emulador
rm -rf ~/.android/avd/[NOME_EMULADOR]

# Criar novo
android create avd --name test --target android-34
```

### Opção 3: Verificar Compatibilidade
```
✓ Android Studio: Bumblebee ou superior
✓ JDK: 11 ou superior
✓ Android SDK: API 21+ (Android 5.0)
✓ Gradle: 8.0+
✓ Kotlin: 1.9+
```

### Opção 4: Abrir Issue no GitHub
```
Descrever:
1. Problema exato
2. Stack trace completo
3. Versão Android
4. Versão Android Studio
5. Passos para reproduzir
```

---

## ✅ Checklist de Verificação

Antes de reportar um bug, verificar:

```
Compilação:
☐ ./gradlew clean build → Sucesso
☐ Sem erros de Kotlin
☐ Sem erros de Firebase (ou ignorado)

Instalação:
☐ ./gradlew installDebug → Sucesso
☐ App aparece no launcher
☐ Abre sem crash

Permissões:
☐ WiFi: Concedido
☐ Bluetooth: Concedido
☐ Location: Concedido
☐ Notifications: Concedido (Android 13+)

Detecção:
☐ WiFi ativo (Settings)
☐ Bluetooth ativo
☐ Pelo menos 1 rede WiFi próxima
☐ Dispositivo BLE próximo (opcional)

Background:
☐ Notificação de foreground aparece
☐ App continua com screen off
☐ Não é morto por Battery Saver

Logs:
☐ adb logcat | grep "PresenceDetector"
☐ Procurar por "Error" ou "Exception"
☐ Verificar eventos de detecção
```

---

**Versão**: 2.0
**Data**: Janeiro 2026
**Atualizado**: Sempre que bugs são encontrados
