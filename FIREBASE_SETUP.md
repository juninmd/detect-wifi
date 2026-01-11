# Firebase Cloud Messaging Setup Guide

## Configuração do Firebase para Push Notifications

O Presence Detector pode enviar push notifications através do Firebase Cloud Messaging (FCM). Siga os passos abaixo:

### 1. Criar Projeto Firebase

1. Acesse [Firebase Console](https://console.firebase.google.com/)
2. Clique em "Adicionar Projeto"
3. Digite um nome (ex: "PresenceDetector")
4. Aceite os termos e clique "Criar Projeto"
5. Aguarde a criação

### 2. Registrar App Android

1. No console do Firebase, clique no ícone Android
2. Preencha:
   - Package name: `com.example.presencedetector`
   - Debug signing certificate SHA-1 (opcional, mas recomendado)
3. Clique "Registrar App"

### 3. Baixar google-services.json

1. Na tela seguinte, clique "Baixar google-services.json"
2. Coloque o arquivo em `app/google-services.json`
3. Clique "Próximo"

### 4. Adicionar SDK do Firebase

O arquivo `app/build.gradle` já contém:
```gradle
id 'com.google.gms.google-services'
implementation platform('com.google.firebase:firebase-bom:32.7.0')
implementation 'com.google.firebase:firebase-messaging-ktx'
```

Se não estiver, adicione manualmente.

### 5. Ativar Cloud Messaging

1. No Firebase Console, vá para "Cloud Messaging"
2. Copie o "Server Key" para usar em suas APIs

### 6. Testar Push Notifications

**Opção A: Dentro do Firebase Console**

1. Vá para "Cloud Messaging"
2. Clique "Enviar sua primeira mensagem"
3. Preencha:
   - Título: "Test Notification"
   - Corpo: "This is a test"
4. Clique "Enviar"
5. Selecione o app no campo "Aplicativos registrados"
6. Clique "Publicar"

**Opção B: Via API REST**

```bash
curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=YOUR_SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "DEVICE_TOKEN",
    "notification": {
      "title": "Presença Detectada",
      "body": "Uma pessoa foi detectada em casa"
    }
  }'
```

### 7. Obter Token do Dispositivo

O app automaticamente obtém o token FCM. Para log:

```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        Log.d("FCM Token", token)
    }
}
```

## Funcionamento das Notificações

### Sem Firebase (Notificações Locais)

O app funcionará com notificações locais mesmo sem Firebase configurado:

```
Presença Detectada
    ↓
PresenceDetectionManager.sendNotification()
    ↓
NotificationUtil.sendPresenceNotification()
    ↓
Notificação Local (no dispositivo apenas)
```

### Com Firebase (Push Notifications)

```
Servidor Firebase
    ↓
Envia mensagem FCM
    ↓
Firebase Messaging Service
    ↓
Processa no app
    ↓
NotificationUtil cria notificação
    ↓
Notificação aparece no dispositivo
```

## Estrutura de Mensagem FCM

```json
{
  "notification": {
    "title": "Presença Detectada",
    "body": "Foram detectados sinais WiFi"
  },
  "data": {
    "method": "WiFi",
    "status": "detected",
    "timestamp": "2026-01-11T10:30:00Z"
  },
  "android": {
    "priority": "high",
    "notification": {
      "sound": "default",
      "channel_id": "presence_alerts_channel"
    }
  }
}
```

## Troubleshooting

### Notificações não chegam

1. Verificar se google-services.json está em `app/`
2. Confirmar se Cloud Messaging está ativado no Firebase Console
3. Verificar token do dispositivo nos logs
4. Desabilitar VPN ou proxy
5. Reinstalar o app

### Erro de compilação com Firebase

```
Error: Could not find com.google.gms:google-services
```

Solução:
1. No `build.gradle` raiz, adicione:
```gradle
classpath 'com.google.gms:google-services:4.4.0'
```

2. No `app/build.gradle`, adicione:
```gradle
plugins {
    id 'com.google.gms.google-services'
}
```

### Token não é gerado

```kotlin
// Debug token generation
FirebaseMessaging.getInstance().token
    .addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("FCM", "Token: ${task.result}")
        } else {
            Log.e("FCM", "Error getting token", task.exception)
        }
    }
```

## Integração com Sistema de Automação Residencial

### MQTT Bridge

O Firebase/FCM pode disparar eventos em um sistema MQTT:

```
Presença Detectada (FCM)
    ↓
Seu servidor recebe
    ↓
Publica em MQTT topic: `home/presence`
    ↓
Home Assistant / Node-RED recebem
    ↓
Acionam automações (luzes, alarme, etc)
```

### Webhook

Configure um webhook para receber notificações:

```
POST /api/presence-webhook
{
  "presence": true,
  "method": "WiFi",
  "timestamp": "2026-01-11T10:30:00Z"
}
```

## Segurança

⚠️ **Importante**: Nunca exponha seu Server Key publicamente!

```
❌ ERRADO: Colocar Server Key no código do app
✅ CORRETO: Usar Server Key apenas em servidores backend
```

## Recursos Adicionais

- [Firebase Documentation](https://firebase.google.com/docs)
- [FCM Guide](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Guide](https://developer.android.com/guide/topics/ui/notifiers/notifications)

## Suporte

Para problemas, consulte:
1. Firebase Console → Cloud Messaging → Debug
2. Android Logcat para mensagens de erro
3. GitHub Issues do projeto

---

**Versão**: 1.0
**Data**: Janeiro 2026
