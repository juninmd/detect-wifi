// Passive Wi-Fi presence sensor for ESP32 (Arduino core).
// Listens for probe requests (management frames) and sends MAC + RSSI to the backend in batches.
// It never transmits probes and never reads frame payloads other than the source MAC.
#include <WiFi.h>
#include <HTTPClient.h>
#include <esp_wifi.h>

const char* WIFI_SSID = "YOUR_SSID";
const char* WIFI_PASS = "YOUR_PASSWORD";
const char* BACKEND_URL = "http://192.168.1.10:3000/sightings";
const char* API_KEY = "change-me-at-least-16-chars";
const char* SENSOR_ID = "esp32-sala";

const uint32_t SEND_INTERVAL_MS = 10000;
const uint32_t HOP_INTERVAL_MS = 300;
const uint8_t MAX_CHANNEL = 11;  // use 13 outside the Americas
const size_t MAX_ENTRIES = 64;

struct Sighting { uint8_t mac[6]; int8_t rssi; };
volatile size_t count = 0;
Sighting buffer[MAX_ENTRIES];
portMUX_TYPE mux = portMUX_INITIALIZER_UNLOCKED;

void IRAM_ATTR onFrame(void* buf, wifi_promiscuous_pkt_type_t type) {
  if (type != WIFI_PKT_MGMT) return;
  const wifi_promiscuous_pkt_t* pkt = (wifi_promiscuous_pkt_t*)buf;
  const uint8_t* frame = pkt->payload;
  if (frame[0] != 0x40) return;  // probe request
  const uint8_t* src = frame + 10;  // addr2 = transmitter
  portENTER_CRITICAL_ISR(&mux);
  for (size_t i = 0; i < count; i++) {
    if (memcmp(buffer[i].mac, src, 6) == 0) {
      buffer[i].rssi = pkt->rx_ctrl.rssi;
      portEXIT_CRITICAL_ISR(&mux);
      return;
    }
  }
  if (count < MAX_ENTRIES) {
    memcpy(buffer[count].mac, src, 6);
    buffer[count].rssi = pkt->rx_ctrl.rssi;
    count++;
  }
  portEXIT_CRITICAL_ISR(&mux);
}

// Channel hopping breaks an AP association, so the radio alternates: sniff (hopping) -> connect -> send.
bool connectWifi() {
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  for (int i = 0; i < 40 && WiFi.status() != WL_CONNECTED; i++) delay(250);
  return WiFi.status() == WL_CONNECTED;
}

void startSniffing() {
  esp_wifi_set_promiscuous_rx_cb(&onFrame);
  esp_wifi_set_promiscuous(true);
}

void sendBatch() {
  Sighting local[MAX_ENTRIES];
  portENTER_CRITICAL(&mux);
  size_t n = count;
  memcpy(local, buffer, n * sizeof(Sighting));
  count = 0;
  portEXIT_CRITICAL(&mux);
  if (n == 0) return;

  esp_wifi_set_promiscuous(false);
  if (!connectWifi()) {
    esp_wifi_set_promiscuous(true);
    return;  // batch dropped; sensor keeps sniffing
  }

  String json = String("{\"sensorId\":\"") + SENSOR_ID + "\",\"sightings\":[";
  for (size_t i = 0; i < n; i++) {
    char mac[18];
    snprintf(mac, sizeof(mac), "%02X:%02X:%02X:%02X:%02X:%02X", local[i].mac[0], local[i].mac[1],
             local[i].mac[2], local[i].mac[3], local[i].mac[4], local[i].mac[5]);
    json += String(i ? "," : "") + "{\"mac\":\"" + mac + "\",\"rssi\":" + local[i].rssi + "}";
  }
  json += "]}";

  HTTPClient http;
  http.begin(BACKEND_URL);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("x-api-key", API_KEY);
  http.POST(json);
  http.end();
  WiFi.disconnect();
  esp_wifi_set_promiscuous(true);
}

void setup() {
  WiFi.mode(WIFI_STA);
  WiFi.disconnect();
  startSniffing();
}

void loop() {
  static uint32_t lastSend = 0, lastHop = 0;
  static uint8_t channel = 1;
  uint32_t now = millis();
  if (now - lastHop >= HOP_INTERVAL_MS) {
    lastHop = now;
    channel = channel % MAX_CHANNEL + 1;
    esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
  }
  if (now - lastSend >= SEND_INTERVAL_MS) {
    lastSend = now;
    sendBatch();
  }
}
