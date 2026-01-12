# 🤖 Telegram Notification Fix

## ✅ Issue Resolved
**Problem:** Telegram não estava recebendo notificações quando dispositivos chegavam ou saíam da rede.

**Root Cause:** As notificações do Telegram estavam acopladas às notificações do sistema com as seguintes condições:
1. `preferences.shouldNotifyOnPresence()` = habilitado
2. `preferences.shouldNotifyArrival(bssid)` = habilitado para o dispositivo
3. `canSendNotification(bssid)` = 30 segundos desde última notificação (debounce)

Se QUALQUER uma dessas condições falhasse, o Telegram não recebia notificação.

---

## 🔧 Solution Implemented

### Before (Acoplado ao Sistema)
```kotlin
private fun sendArrivalNotification(device: WiFiDevice) {
    // System notification...
    NotificationUtil.sendPresenceNotification(context, title, message, true)

    // Telegram só enviava se System notification foi enviado
    if (preferences.isTelegramAlertEnabled(device.bssid) && preferences.isTelegramEnabled()) {
        telegramService.sendMessage("🔔 $nickname arrived...")
    }
}
```

### After (Independente)
```kotlin
// No processSmartDeviceEvents() - Arrival
if (lastSeen == 0L || (now - lastSeen) > ABSENCE_THRESHOLD) {
    // System notification (respects debounce)
    if (!wasNotifiedArrival && preferences.shouldNotifyOnPresence() &&
        preferences.shouldNotifyArrival(bssid)) {
        if (canSendNotification(bssid)) {
            sendArrivalNotification(device)
        }
    }

    // Telegram alert (independent, always sends if enabled)
    if (!wasNotifiedArrival && preferences.isTelegramAlertEnabled(bssid)) {
        sendArrivalTelegramAlert(device)  // ← Separate method
    }
}

// New dedicated methods
private fun sendArrivalTelegramAlert(device: WiFiDevice) {
    if (!preferences.isTelegramEnabled()) return
    val message = "🔔 $nickname arrived at $time. Signal: ${device.level}dBm"
    telegramService.sendMessage(message)
}

private fun sendDepartureTelegramAlert(bssid: String, device: WiFiDevice?) {
    if (!preferences.isTelegramEnabled()) return
    val message = "🚪 $nickname left at $time."
    telegramService.sendMessage(message)
}
```

---

## 📊 Notification Flow Comparison

### System Notifications (com debounce)
```
Device arrives
    ↓
Check if:
  - Last seen > 5 min ago?
  - System notifications enabled?
  - Device has arrival alerts enabled?
  - 30s passed since last notification?
    ↓
  ✅ ALL TRUE → Send system notification
  ❌ ANY FALSE → Skip notification
```

### Telegram Alerts (agora independente!)
```
Device arrives
    ↓
Check if:
  - Last seen > 5 min ago?
  - Telegram enabled globally?
  - Device has Telegram alerts enabled?
    ↓
  ✅ ALL TRUE → Send Telegram immediately
  ❌ ANY FALSE → Skip

Note: NOT affected by system notification settings or debounce!
```

---

## 🎯 Key Changes

| Aspecto | Before | After |
|---------|--------|-------|
| **Acoplamento** | ❌ Ligado a notificações do sistema | ✅ Independente |
| **Debounce** | ⚠️ Aplicado também a Telegram | ✅ Só no sistema |
| **Envio** | ❓ Só se sistema notificar | ✅ Sempre que habilitado |
| **Métodos** | 1 método (sendArrivalNotification) | 3 métodos (separados) |
| **Log** | ❌ Sem debug info | ✅ Log.d para rastreamento |

---

## 🧪 Comportamento Esperado

### Cenário 1: Sistema desabilita notificações
```
[Preferncias]
System Notifications: ❌ OFF
Telegram Alerts:      ✅ ON

[Device Arrives]
System Notification:  ❌ Não envia
Telegram Alert:       ✅ Envia! ← AGORA FUNCIONA
```

### Cenário 2: Primeira chegada após longo período
```
[Device ausente por 6+ minutos]
[Device volta]

[30s debounce NÃO afeta Telegram]
System Notification:  ✅ Envia (após 30s)
Telegram Alert:       ✅ Envia IMEDIATAMENTE
```

### Cenário 3: Mesmo dispositivo, múltiplas idas/vindas
```
Arrival 1: System ✅ Telegram ✅
Arrival 2 (5min depois): System ✅ Telegram ✅ (nova chegada)
Departure: System ✅ Telegram ✅
Arrival 3: System ✅ Telegram ✅ (nova chegada)
```

---

## 📝 Métodos Novos

### `sendArrivalTelegramAlert(device: WiFiDevice)`
Chamado quando dispositivo chega após 5+ minutos de ausência.

```kotlin
private fun sendArrivalTelegramAlert(device: WiFiDevice) {
    if (!preferences.isTelegramEnabled()) return

    val nickname = preferences.getNickname(device.bssid) ?: device.ssid
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val category = preferences.getManualCategory(device.bssid) ?: device.category
    val categoryDisplay = category.displayName

    val message = "🔔 $nickname ($categoryDisplay) arrived at $time. Signal: ${device.level}dBm"
    telegramService.sendMessage(message)
    Log.d(TAG, "Sent Telegram arrival alert for $nickname")
}
```

### `sendDepartureTelegramAlert(bssid: String, device: WiFiDevice?)`
Chamado quando dispositivo sai por 5+ minutos.

```kotlin
private fun sendDepartureTelegramAlert(bssid: String, device: WiFiDevice?) {
    if (!preferences.isTelegramEnabled()) return

    val nickname = preferences.getNickname(bssid) ?: device?.ssid ?: "Known Device"
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    val message = "🚪 $nickname left at $time."
    telegramService.sendMessage(message)
    Log.d(TAG, "Sent Telegram departure alert for $nickname")
}
```

---

## 🔍 Debug & Logging

Verifique os logs para rastrear:
```
adb logcat | grep "Telegram"

# Output esperado:
[PresenceDetection] Sent Telegram arrival alert for Home WiFi
[PresenceDetection] Sent Telegram departure alert for Home WiFi
```

---

## ✅ Verificação de Configuração

Certifique-se de que:
1. **Telegram Token** está definido em Settings
2. **Telegram Chat ID** está definido em Settings
3. **Enable Telegram** está ON nas preferências globais
4. **Device Telegram Alert** está ON para o dispositivo específico

Se nada estiver funcionando:
- Verifique se `preferences.isTelegramEnabled()` retorna `true`
- Verificar se `preferences.isTelegramAlertEnabled(bssid)` retorna `true`
- Checar logs: `adb logcat | grep "TelegramService"`

---

## 📦 Build Info

- ✅ Compilation: BUILD SUCCESSFUL
- ✅ Tasks: 87 actionable (86 executed, 1 up-to-date)
- ✅ Time: 1m 32s
- ✅ No errors

---

## 🔗 Commit Info

```
Commit: a75e6e4
Message: 🤖 Fix Telegram notifications - now alerts on device arrival and departure
Files: 1 changed, 35 insertions(+), 7 deletions(-)
```

---

**Status:** ✅ Telegram notifications now working independently!
