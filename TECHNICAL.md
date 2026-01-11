# Guia Técnico - Detecção de Presença WiFi/Bluetooth

## 1. Visão Geral Técnica

O sistema utiliza dois métodos de detecção complementares:

### 1.1 Método WiFi (Primário)
- **Mecanismo**: Análise de redes WiFi disponíveis
- **Princípio**: Dispositivos conectados ao WiFi emitem sinais de beacon
- **Frequência**: 2.4 GHz e 5 GHz
- **Vantagem**: Detecta presença sem necessidade de emparelamento
- **Cobertura**: ~30-50 metros em linha reta

### 1.2 Método Bluetooth (Fallback)
- **Mecanismo**: Varredura BLE (Bluetooth Low Energy)
- **Princípio**: Detecta anúncios de dispositivos BLE
- **Vantagem**: Menor consumo de bateria
- **Cobertura**: ~10-20 metros

## 2. Algoritmos de Detecção

### 2.1 WiFi Signal Analysis

```
Algoritmo: SIGNAL_STRENGTH_ANALYSIS
Entrada: Lista de ScanResult
Saída: boolean (presença detectada)

1. Para cada rede WiFi escaneada:
   - Obter RSSI (Received Signal Strength Indicator)
   - Se RSSI >= THRESHOLD (-70 dBm):
     - Incrementar contador de sinais fortes
     - Registrar SSID e frequência

2. Se contador > 0:
   - Presença detectada = TRUE
   - Log detalhes da rede
Senão:
   - Presença detectada = FALSE

Threshold RSSI (dBm):
  -30 a -50: Muito perto (< 1 metro)
  -50 a -70: Perto (1-5 metros)
  -70 a -80: Moderado (5-10 metros)
  -80 a -100: Longe (10-30 metros)
  -100+: Muito longe ou bloqueado
```

### 2.2 Bluetooth BLE Scanning

```
Algoritmo: BLE_DEVICE_DETECTION
Entrada: ScanResult com RSSI
Saída: boolean (presença detectada)

1. Iniciar varredura BLE
2. Para cada dispositivo anunciado:
   - Obter endereço MAC e nome
   - Obter RSSI do anúncio
   - Se RSSI >= -70 dBm:
     - Adicionar a conjunto detectado
     - Notificar imediatamente

3. Se tamanho(conjunto) > 0:
   - Presença detectada = TRUE
Senão:
   - Presença detectada = FALSE
```

### 2.3 Decisão Combinada

```
Algoritmo: COMBINED_PRESENCE_DECISION
Entrada: wifi_detected, bt_detected, timeouts
Saída: boolean (presença final)

tempo_atual = currentTimeMillis()

// Verificar WiFi
se (wifi_detected E tempo_atual - last_wifi_detection < 30000):
    retornar TRUE

// Verificar Bluetooth
se (bt_detected E tempo_atual - last_bt_detection < 30000):
    retornar TRUE

// Nenhuma detecção recente
retornar FALSE
```

## 3. Implementação de Detalhes

### 3.1 WiFiDetectionService

```java
// Ciclo de operação
performScan() {
    scanResults = wifiManager.getScanResults()
    presenceDetected = analyzeSignals(scanResults)
    notifyPresence(presenceDetected)
}

// Análise de sinais
analyzeSignals(results) {
    strongSignalCount = 0
    para cada result em results:
        se result.level >= -70:
            strongSignalCount++
    retornar strongSignalCount > 0
}

// Intervalo: 5 segundos
timer.scheduleAtFixedRate(performScan, 0, 5000)
```

### 3.2 BluetoothDetectionService

```java
// Callback de BLE Scan
ScanCallback {
    onScanResult(callbackType, result) {
        rssi = result.getRssi()
        se rssi >= -70:
            detectedDevices.add(device)
            notifyPresence(true)
    }

    onBatchScanResults(results) {
        se results.isEmpty() == false:
            notifyPresence(true)
    }
}

// Intervalo: 10 segundos
timer.scheduleAtFixedRate(performScan, 0, 10000)
```

## 4. Calibração e Otimização

### 4.1 Ajuste de Threshold RSSI

```
Ambiente          | Threshold Recomendado | Notas
---------------------------------------------------
Casa (WiFi 5GHz)  | -70 dBm              | Padrão
Casa (WiFi 2.4GHz)| -75 dBm              | Mais sensível
Apartamento       | -65 dBm              | Mais seletivo
Ao ar livre       | -60 dBm              | Menos interferência
```

### 4.2 Ajuste de Intervalo de Varredura

```
Caso de Uso           | Intervalo    | Consumo    | Latência
----------------------------------------------------------
Tempo Real Crítico    | 1-2 seg       | Alto       | Baixa
Detecção Padrão       | 5 seg         | Normal     | Média
Econômico             | 10-15 seg     | Baixo      | Alta
```

### 4.3 Timeout de Detecção

```
Timeout Padrão: 30 segundos

Lógica:
- Se presença detectada, marcar timestamp
- Se nenhuma detecção por 30s, considerar ausência
- Previne falsos negativos temporários
```

## 5. Fluxo de Execução Detalhado

### 5.1 Inicialização

```
MainActivity.onCreate()
├─ initializeViews()
├─ checkPermissions()
└─ setupDetectionManager()
    ├─ WiFiDetectionService.init()
    └─ BluetoothDetectionService.init()
```

### 5.2 Início de Detecção

```
startDetection()
├─ requestPermissions()
├─ WiFiDetectionService.startScanning()
│  └─ Timer: performScan() a cada 5s
└─ BluetoothDetectionService.startScanning()
   └─ Timer: performScan() a cada 10s
```

### 5.3 Ciclo de Detecção

```
WiFi: a cada 5s
├─ wifiManager.getScanResults()
├─ analyzeSignals()
└─ notifyPresence() → MainHandler → UI

Bluetooth: a cada 10s
├─ bleScanner.startScan()
├─ ScanCallback.onScanResult()
└─ notifyPresence() → MainHandler → UI
```

### 5.4 Parada

```
stopDetection()
├─ WiFiDetectionService.stopScanning()
├─ BluetoothDetectionService.stopScanning()
└─ UI: Update status
```

## 6. Tratamento de Erros

### 6.1 WiFi

```
Possíveis Erros:
- WiFi Manager null → Log warning, fallback para BLE
- Permissões negadas → Solicitar ao usuário
- Sem redes disponíveis → Continuar monitorando
```

### 6.2 Bluetooth

```
Possíveis Erros:
- Bluetooth não disponível → Log e desabilitar
- BLE Scanner null → Ignorar, apenas clássico
- Scan failed (errorCode) → Retry após 5s
```

## 7. Comunicação com UI

### 7.1 Thread-Safety

```java
// Sempre usar MainHandler para atualizar UI
mainHandler.post(() -> {
    presenceListener.onPresenceDetected(detected, details);
});
```

### 7.2 Listeners

```java
interface PresenceListener {
    void onPresenceDetected(boolean detected, String details);
    void onStatusUpdate(String status);
}
```

## 8. Consumo de Recursos

### 8.1 Bateria

```
WiFi Scanning:     ~15-20 mA (ativo)
Bluetooth Scanning: ~5-10 mA (BLE)
Processamento:      <1 mA
Total (ativo):      ~20-30 mA

Modo Econômico (30s intervalo): ~5 mA média
```

### 8.2 Memória

```
WiFiDetectionService:     ~2 MB
BluetoothDetectionService: ~1 MB
MainActivity:             ~3 MB
Total:                    ~6 MB
```

## 9. Casos de Uso e Limitações

### 9.1 Funciona Bem Para

- ✅ Detectar presença geral na casa
- ✅ Múltiplos dispositivos
- ✅ Ativação de automações baseadas em presença
- ✅ Alarmes quando casa vazia

### 9.2 Limitações

- ❌ Não detecta pessoas sem dispositivos
- ❌ Pode ter falsos positivos com gadgets deixados
- ❌ Depende de WiFi/BLE ativo
- ❌ Alcance limitado por paredes

## 10. Extensões Futuras

```
Melhorias Possíveis:
- [ ] Integração com Home Assistant
- [ ] Machine Learning para padrões
- [ ] Detecção de movimento com sensors
- [ ] Integração MQTT
- [ ] Interface web remota
- [ ] Notificações push
```

---

**Versão**: 1.0
**Data**: Janeiro 2026
**Mantido por**: Detectar Presença Project
