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
*   ✅ **Robust Background Service:** Reliable detection even when the app is closed.
*   ✅ **Smart Notifications:** Debounced alerts to prevent spam (Arrival/Departure).
*   ✅ **Dual-Tech Detection:** WiFi + Bluetooth LE scanning.
*   ✅ **Anti-Theft Suite:** Motion, Pocket, and Charger alarms.
*   ✅ **UI Improvements:** Manual refresh, labeled device badges, and dark mode support.

**Metrics:**
*   **Open Issues:** 0 (Clean Slate)
*   **Urgency:** Medium (Feature Development & Enhancement)

## 3. Quarterly Roadmap (2026)

### Q1 2026: Experience & Organization
*Focus: Refining the user interface and device management.*

*   **High Priority:**
    *   🎨 **UI/UX Polish:** Implement visual feedback for refresh actions (Toast + Animation) and complete the custom icon set.
    *   📱 **Device Management:** Implement "Sort by Labeled Devices" to prioritize known devices in the list.
*   **Medium Priority:**
    *   ⚡ **Battery Optimization:** Profile and optimize background scanning intervals to reduce drain by 15%.
*   **Low Priority:**
    *   Dependency updates and code cleanup.

### Q2 2026: Connectivity & Data
*Focus: Expanding integration and robust data handling.*

*   **High Priority:**
    *   🔔 **Notification Channels:** Separate "Arrival", "Departure", and "Security Alert" channels for granular user control.
    *   🔗 **MQTT Integration:** Enable integration with Home Assistant/Mosquitto.
*   **Medium Priority:**
    *   💾 **Room Database Migration:** Migrate from SharedPreferences to Room for robust history and device management.

### Q3 2026: Intelligence & Context
*Focus: Smarter detection and multi-context support.*

*   **High Priority:**
    *   🧠 **Machine Learning Patterns:** Local ML model to learn user routines and suppress false alarms.
*   **Medium Priority:**
    *   📍 **Multi-Location Support:** Profiles for different locations (Home, Office).
    *   🛡️ **Advanced Anti-Theft:** Trigger alarms based on specific BLE device absence.

### Q4 2026: Ecosystem Expansion
*Focus: New platforms and remote access.*

*   **Medium Priority:**
    *   🖥️ **Web Dashboard:** Local HTTP server for status viewing.
    *   ⌚ **Wear OS Companion:** Watch app for quick controls.
*   **Low Priority:**
    *   🌐 **Internationalization:** Complete translations for ES, FR, DE, PT-BR.

## 4. Feature Details

### UI/UX Polish (Q1)
**User Value Proposition:**
Provides immediate visual feedback to user actions, making the app feel more responsive and polished.

**Technical Approach:**
*   **Feedback:** Add `Toast` messages upon manual refresh trigger.
*   **Visuals:** Implement a rotation animation for the refresh icon during scanning.
*   **Success Criteria:** User receives instant feedback on refresh; scanning animation indicates background activity.
*   **Estimated Effort:** Small (2-3 days)

### Device Management (Q1)
**User Value Proposition:**
Helps users quickly find important devices in crowded networks by keeping labeled devices at the top.

**Technical Approach:**
*   **Sorting Logic:** Implement a custom `Comparator` in `WifiAdapter` prioritizing `nickname != null`.
*   **Persistence:** Store sort preference in SharedPreferences.
*   **Success Criteria:** Labeled devices appear at the top of the list when sorting is enabled.
*   **Estimated Effort:** Small (3-5 days)

### Notification Channels (Q2)
**User Value Proposition:**
Gives users control over which events trigger sounds/vibrations. E.g., Silent for "Arrival", Loud for "Security Alert".

**Technical Approach:**
*   **Channels:** Define specific Notification Channels in `NotificationManager`.
*   **Migration:** Update notification builder logic to use new channel IDs.
*   **Success Criteria:** Users can customize notification settings per category in Android Settings.
*   **Estimated Effort:** Medium (1-2 weeks)

### MQTT Integration (Q2)
**User Value Proposition:**
Enables powerful home automation scenarios (lights, locks) based on presence.

**Technical Approach:**
*   **Library:** Integrate Paho MQTT Android Client.
*   **Configuration:** Create settings UI for Broker URL, Port, Auth.
*   **Logic:** Publish JSON payloads on presence change events.
*   **Success Criteria:** Successful connection to local broker; reliable message publishing.
*   **Estimated Effort:** Medium (2-3 weeks)

## 5. Dependencies & Risks

*   **Android Background Restrictions:** Future Android versions may restrict background processes further.
    *   *Mitigation:* Continuously monitor Android APIs and adapt Foreground Service types.
*   **Battery Impact:** Continuous scanning and MQTT can drain battery.
    *   *Mitigation:* Implement "Eco Mode" and user-configurable update intervals.
*   **Hardware Variability:** Bluetooth/WiFi performance varies across devices.
    *   *Mitigation:* Maintain a device compatibility list and allow sensitivity adjustments.
