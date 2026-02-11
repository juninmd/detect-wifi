# Product Roadmap: Detect-WiFi (Presence Detector)

## 1. Vision & Goals

**Vision:**
To become the definitive, privacy-focused Android solution for residential presence detection and anti-theft security, bridging the gap between personal device monitoring and smart home automation. We aim to provide professional-grade reliability without sacrificing user privacy or battery life.

**Goals:**
*   **Reliability:** Ensure 99.9% uptime for background detection services across all supported Android versions (8.0+).
*   **Integration:** Seamlessly connect with major home automation platforms (Home Assistant, Node-RED) via MQTT.
*   **User Experience:** Deliver an intuitive, "set-and-forget" experience with actionable insights and minimal false alarms.
*   **Privacy:** Process all data locally on the device, ensuring user location and habits remain private.

## 2. Current Status

**Version:** 2.1 (Production Ready)
**Tech Stack:** Kotlin, Android 15 Target, Coroutines, Foreground Services.

**Key Achievements:**
*   ✅ **Robust Background Service:** Reliable detection even when the app is closed or the screen is off.
*   ✅ **Modern UI:** Vibrant, responsive interface with Dark Mode support and intuitive controls.
*   ✅ **Notification Intelligence:** Implemented "Smart Notifications" with debounce to reduce spam and focus on meaningful events (Arrival/Departure).
*   ✅ **Anti-Theft Features:** Motion, Pocket, and Charger alarms fully implemented.
*   ✅ **Labeled Badges:** Visual distinction for manually named devices.
*   ✅ **Dual-Tech Detection:** Simultaneous WiFi and Bluetooth LE scanning for robust presence verification.
*   ✅ **Hotspot Recognition:** Identifies mobile hotspots based on common SSID naming patterns.
*   ✅ **Telegram Integration:** Decoupled, independent alerts for arrival/departure events.

**Metrics:**
*   **Open Issues:** 8 (Maintenance & Minor Bugs)
*   **Urgency:** Medium (Feature Development)

## 3. Quarterly Roadmap (2026)

### Q1 2026: Foundation & Refinement
*Focus: Stability, Bug Fixes, and UI Polish*

*   **High Priority (Immediate):**
    *   🐛 **Bug Squashing:** Address the 8 open issues (minor bugs, edge cases in detection).
    *   ⚡ **Battery Optimization:** Profile and optimize background scanning intervals to reduce drain by 15%.
*   **Medium Priority:**
    *   🎨 **UI/UX Polish:** Implement "Toast on Refresh", "Refresh Animation", and complete custom icon set.
    *   gadget **Device Management:** Add "Sort by Labeled Devices" to prioritize known devices in the list.
*   **Low Priority:**
    *   Code cleanup and dependency updates (Gradle, Kotlin).

### Q2 2026: Connectivity & Integration
*Focus: Expanding beyond the device*

*   **High Priority:**
    *   🔗 **MQTT Integration:** (See Feature Details below) - Enable integration with Home Assistant.
*   **Medium Priority:**
    *   🔔 **Notification Channels:** Separate "Arrival", "Departure", and "Security Alert" channels for granular user control.
    *   💾 **Room Database Migration:** Migrate from SharedPreferences/File storage to Room for robust history and device management.

### Q3 2026: Intelligence & Context
*Focus: Smarter detection and multi-context support*

*   **High Priority:**
    *   🧠 **Machine Learning Patterns:** Local ML model to learn user routines and suppress false alarms based on historical data.
*   **Medium Priority:**
    *   📍 **Multi-Location Support:** Profiles for "Home", "Office", "Parents' House" with automatic switching.
    *   🛡️ **Advanced Anti-Theft:** Trigger alarms based on specific BLE device absence (e.g., "Key Fob missing").

### Q4 2026: Ecosystem Expansion
*Focus: New platforms and remote access*

*   **Medium Priority:**
    *   🖥️ **Web Dashboard:** Local HTTP server on the phone to view status from a PC browser.
    *   ⌚ **Wear OS Companion:** Watch app for quick arm/disarm and status checks.
*   **Low Priority:**
    *   🌐 **Internationalization:** Complete translation for ES, FR, DE, PT-BR.

## 4. Feature Details

### Device Management (Q1)

**User Value Proposition:**
Allows users to prioritize and quickly access their most important devices (those with custom labels) by keeping them at the top of the list, especially useful in crowded WiFi environments.

**Technical Approach:**
*   **Sorting Logic:** Implement a custom `Comparator` in `WifiAdapter` that prioritizes devices with `nickname != null`.
*   **UI Control:** Add an "Importance" option to the "Sort by" dialog to prioritize labeled devices.
*   **State:** Persist the sort preference in `SharedPreferences`.

**Success Criteria:**
*   Labeled devices appear at the top of the list when enabled.
*   Sorting is applied instantly upon labeling a device.
*   Default sort order remains by Signal Strength until changed.

**Estimated Effort:** Small (3-5 days)

### MQTT Integration (Q2)

**User Value Proposition:**
Allows users to trigger real-world actions based on presence. For example, turning on lights when "Smartphone" connects to WiFi, or arming the home security system when "Everyone" leaves. This transforms the app from a passive monitor to an active home automation trigger.

**Technical Approach:**
*   **Library:** Integrate Paho MQTT Android Client or similar lightweight Kotlin MQTT library.
*   **Configuration:** Add "Integrations" screen in Settings for Broker URL, Port, Username, Password, and Topic Prefix.
*   **Logic:**
    *   Publish JSON payloads to `detect_wifi/status` (Global) and `detect_wifi/device/{mac}` (Individual).
    *   Implement "Last Will and Testament" to signal if the service dies unexpectedly.
    *   Ensure MQTT connection is managed within the existing `DetectionBackgroundService` to maintain uptime.

**Success Criteria:**
*   User can successfully connect to a local Mosquitto/Home Assistant broker.
*   Latency between detection event and MQTT publish is < 2 seconds.
*   Reconnection logic handles network switches (WiFi <-> Data) gracefully.

**Estimated Effort:** Medium (2-3 weeks)

### Room Database Migration (Q2)

**User Value Proposition:**
Improved performance and data integrity for device history and logs, especially with large datasets. Enables complex queries for future analytics features.

**Technical Approach:**
*   **Framework:** Use Android Room Persistence Library.
*   **Entities:** Define `Device`, `LogEntry`, `Settings` entities.
*   **Migration:** Implement `Migration` strategies to import existing SharedPreferences/File data without data loss.
*   **Async:** Ensure all DB operations use Coroutines/Flow.

**Success Criteria:**
*   No data loss during migration.
*   Faster load times for device lists and logs.
*   Complex queries (e.g., "Show logs for device X in last 7 days") become possible.

**Estimated Effort:** Medium (2 weeks)

### Machine Learning Patterns (Q3)

**User Value Proposition:**
Reduces false alarms by learning normal arrival/departure patterns. For example, if user usually arrives at 6 PM, a brief signal loss at 5:55 PM might be ignored more aggressively.

**Technical Approach:**
*   **Model:** Lightweight TensorFlow Lite model or custom heuristic algorithm.
*   **Input:** Time of day, day of week, signal strength history, other devices present.
*   **Output:** Probability of presence.
*   **Privacy:** Training and inference happen 100% on-device.

**Success Criteria:**
*   False positive rate reduced by 30%.
*   Model size < 5MB.
*   Minimal impact on battery life.

**Estimated Effort:** Large (4 weeks)

## 5. Dependencies & Risks

*   **Android Background Restrictions:** Future Android versions (Android 16+) may further restrict background processes. Mitigation: Continuously monitor Google I/O updates and adapt `ForegroundServiceType`.
*   **Battery Impact:** Continuous MQTT connection and ML processing can drain battery. Mitigation: Implement "Eco Mode" and configurable update intervals.
*   **Hardware Variability:** Bluetooth/WiFi chipsets vary wildly across manufacturers. Mitigation: Maintain a device compatibility list and community-sourced bug reports.
