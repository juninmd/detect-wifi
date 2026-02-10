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
*   ✅ **Notification Intelligence:** implemented "Smart Notifications" to reduce spam and focus on meaningful events (Arrival/Departure).
*   ✅ **Anti-Theft Features:** Motion, Pocket, and Charger alarms fully implemented.

**Metrics:**
*   **Open Issues:** 15 (Maintenance & Minor Bugs)
*   **Urgency:** Medium (Feature Development)

## 3. Quarterly Roadmap (2026)

### Q1 2026: Foundation & Refinement
*Focus: Stability, Bug Fixes, and UI Polish*

*   **High Priority (Immediate):**
    *   🐛 **Bug Squashing:** Address the 15 open issues (minor bugs, edge cases in detection).
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

## 4. Feature Spotlight: MQTT Integration (Q2)

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

## 5. Dependencies & Risks

*   **Android Background Restrictions:** Future Android versions (Android 16+) may further restrict background processes. Mitigation: Continuously monitor Google I/O updates and adapt `ForegroundServiceType`.
*   **Battery Impact:** Continuous MQTT connection and ML processing can drain battery. Mitigation: Implement "Eco Mode" and configurable update intervals.
*   **Hardware Variability:** Bluetooth/WiFi chipsets vary wildly across manufacturers. Mitigation: Maintain a device compatibility list and community-sourced bug reports.
