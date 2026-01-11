# Quick Start Guide - Presence Detector

## ⚡ Início Rápido em 5 Minutos

### Requisitos
- Android Studio (Bumblebee ou superior)
- Android SDK 21+ instalado
- Dispositivo Android com WiFi e Bluetooth
- (Opcional) Conta Firebase

---

## 🚀 Passo 1: Abrir o Projeto

```bash
# Clonar ou abrir a pasta
cd detect-wifi
open . # ou File → Open em Android Studio
```

Aguarde a sincronização do Gradle (~1-2 minutos).

---

## 🔧 Passo 2: (Opcional) Configurar Firebase

**Pule para o Passo 3 se quiser usar apenas notificações locais.**

### Com Firebase:

1. Acesse [Firebase Console](https://console.firebase.google.com)
2. Criar → Novo Projeto → "PresenceDetector"
3. Adicionar App Android
   - Package: `com.example.presencedetector`
4. Baixar `google-services.json`
5. Copiar para `app/google-services.json`

Pronto! Firebase configurado ✅

### Sem Firebase:

Prosseguir sem alterações. O app funciona com notificações locais.

---

## 🛠️ Passo 3: Compilar

### Via Android Studio:
1. Build → Clean Project
2. Build → Rebuild Project

### Via Terminal:
```bash
./gradlew clean build
```

Aguarde a conclusão (~2-3 minutos).

---

## 📱 Passo 4: Instalar no Dispositivo

### Emulador (Recomendado para teste):
```bash
./gradlew installDebug
```

### Dispositivo Real:
1. Conectar ao computador via USB
2. Ativar "USB Debugging" nas Configurações
3. Executar em Android Studio ou:
```bash
./gradlew installDebug
```

---

## ▶️ Passo 5: Iniciar Execução

1. **Abrir o app** "Presence Detector"

2. **Conceder Permissões** (quando solicitado)
   - WiFi
   - Bluetooth
   - Localização
   - Notificações (Android 13+)

3. **Clicar "Start Detection"**

4. **Observar Status**
   - Notificação de foreground aparece
   - Indicador muda de cor
   - Log exibe eventos

5. **Testar Detecção**
   - Aproximar outro celular/tablet
   - Deverá detectar em segundos
   - Recebará notificação push/local

---

## 🧪 Verificação Rápida

### ✅ WiFi Funcionando
```
Log mostra: "Found X networks"
Presença: SIM ou NÃO
```

### ✅ Bluetooth Funcionando
```
Log mostra: "BLE scan started"
Devices detectados listados
```

### ✅ Serviço em Background
```
Notificação de foreground visível
Mesmo com app fechado, detecta presença
```

### ✅ Notificações
```
Ao detectar presença: Push notification
Vibra + LED acende
Som (se ativado no dispositivo)
```

---

## 🎯 Próximos Testes

### Teste 1: Presença
```
1. Abrir Presence Detector
2. Start Detection
3. Aproximar outro celular
4. Observar:
   ✓ Indicador fica verde
   ✓ Notificação chega
   ✓ Log atualiza
```

### Teste 2: App em Background
```
1. Iniciar detecção
2. Pressionar Home
3. Afastar o celular (para ninguém detectar)
4. Aproximar novamente
5. Notificação chega mesmo sem app visível
```

### Teste 3: Parada
```
1. Abrir Presence Detector novamente
2. Clicar "Stop Detection"
3. Notificação desaparece
4. Detecção para
```

---

## 🐛 Problemas Comuns

### "Cannot resolve symbol 'R'"
```
→ Build → Clean Project
→ Build → Rebuild Project
```

### "Missing google-services.json"
```
→ OK - é opcional
→ App funciona com notificações locais
→ Ou seguir passo 2 para configurar
```

### Compilação falha com Kotlin
```
→ Verificar JDK em:
   File → Project Structure → SDK Location
→ Usar JDK 11+
```

### App não detecta presença
```
→ Verificar se WiFi está ativado
→ Próximo ao app, aproximar outro celular
→ Verificar permissões: Settings → Apps → Presence Detector
→ Observar LogCat: Ctrl+6
```

### Notificações não aparecem
```
→ Verificar: Settings → Notifications → Presence Detector
→ Ativar notificações
→ Certificar que não está em modo silencioso
```

---

## 📱 Configurações Recomendadas (Dispositivo)

### Battery
- ❌ Battery Saver/Modo Economia OFF
- ✅ Deixar app executar em background

### Permissions
- ✅ WiFi: ON
- ✅ Bluetooth: ON (para fallback)
- ✅ Location: ON (necessário para WiFi/BLE scan)
- ✅ Notifications: ON

### Developer Options
- ✅ USB Debugging: ON (se usar emulador)
- ✅ Stay Awake: ON (para testes)

---

## 📊 Monitoramento

### Via Logcat
```bash
# Terminal
adb logcat | grep "PresenceDetector"

# Ou em Android Studio
View → Tool Windows → Logcat
Filter: "PresenceDetector"
```

### Informações úteis
```
✓ WiFi scanning ativo
✓ Bluetooth scanning ativo
✓ Presença detectada/removida
✓ Erros de permission
✓ Eventos de notificação
```

---

## 🎓 Próximas Leituras

1. **README.md** - Documentação completa
2. **FIREBASE_SETUP.md** - Configurar push via Firebase
3. **BACKGROUND_SERVICE.md** - Como funciona o serviço
4. **TECHNICAL.md** - Detalhes técnicos de algoritmos

---

## 💡 Dicas

- 🔄 A detecção WiFi roda a cada 5 segundos
- 🔄 A detecção Bluetooth roda a cada 10 segundos
- ⏱️ Presença se mantém por 30 segundos após última detecção
- 🔔 Notificações push são limitadas a 1 por 30 segundos (evita spam)
- 📊 Verificar logs para compreender o funcionamento

---

## ✨ Pronto!

Seu Presence Detector está rodando! 🎉

```
🟢 WiFi Detection   ✓
🔵 Bluetooth Detection ✓
📱 Background Service  ✓
🔔 Push Notifications  ✓
📊 Real-time Log      ✓
```

---

**Versão**: 2.0
**Data**: Janeiro 2026
**Tempo Estimado**: 5-10 minutos
**Dificuldade**: Muito Fácil
