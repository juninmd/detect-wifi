# Detecção de presença por Wi-Fi (sensor passivo + backend)

Complementa o app Android: um **ESP32** escuta *probe requests* (aparelhos com Wi-Fi ligado, mesmo sem estar
conectados ao seu roteador) e envia MAC + RSSI para o `backend/`, que responde "quem está por perto".

## Fluxo

```
ESP32 (firmware/esp32-sniffer) --POST /sightings--> backend --GET /presence--> app/mobile
```

## Backend

```bash
cd backend && npm ci && npm run build
API_KEY=uma-chave-com-16+-caracteres \
KNOWN_MACS="AA:BB:CC:DD:EE:FF=Celular Ana,11:22:33:44:55:66=Celular Beto" \
PORT=3000 npm start
```

| Rota | Auth | Descrição |
|------|------|-----------|
| `GET /health` | não | Saúde do serviço |
| `POST /sightings` | `x-api-key` | `{"sensorId":"esp32-sala","sightings":[{"mac":"AA:..","rssi":-60}]}` → `202 {"accepted":n}` |
| `GET /presence?window=300` | `x-api-key` | Janela em segundos (1–3600, padrão 300) |

Resposta de `/presence`: `known` (MACs em `KNOWN_MACS`), `unknown` (MAC fixo não cadastrado — candidato a intruso) e
`anonymousCount` (MAC aleatório: só contagem).

Os dados ficam só em memória (1 h) e o servidor fala HTTP puro: use apenas na LAN ou atrás de um proxy com TLS.

## Limitações (importante)

- **MAC aleatório:** iOS e Android atuais randomizam o MAC nos probe requests. Esses aparelhos só são *contados*,
  nunca identificados. Só aparelhos com MAC fixo (IoT, notebooks, celulares antigos ou com randomização desligada
  para a sua rede) entram em `known`/`unknown`.
- Para identificar quem está **conectado**, use a tela do roteador do app (TP-Link) — o roteador vê o MAC real.
- O ESP32 não compila neste ambiente de CI: o firmware é um ponto de partida, teste na placa
  (Arduino IDE → placa "ESP32 Dev Module").

## Privacidade e LGPD

Monitore apenas a sua casa e com ciência dos moradores. Capturar identificadores de vizinhos/visitantes sem
consentimento pode violar a LGPD. O firmware envia apenas MAC e RSSI, nunca o conteúdo dos quadros.
