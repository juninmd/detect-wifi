<<<<<<< HEAD
# 🗺️ ROADMAP.md - Detect Wifi Evolution

## 🏁 Phase 1: Core Foundation ✅
- [x] Initial scaffolding and repository setup.
- [x] Basic architecture and structural implementation.
- [x] Antigravity documentation alignment.

## 🚧 Phase 2: Refinement & Depth (Current)
- [ ] **Feature Expansion**: Implement core business logic and missing functionalities.
- [ ] **Testing Suite**: Increase test coverage to at least 80%.
- [ ] **Optimization**: Improve performance and reduce technical debt.

## 🚀 Phase 3: Polish & Scale (Future)
- [ ] **CI/CD Integration**: Fully automated release pipelines.
- [ ] **Advanced Features**: Integration with external APIs and AI tools.
- [ ] **Global Launch**: Prepare for production release and community use.

## 🎯 v1.0 Milestone
A robust, stable, and Antigravity-compliant project ready for scale.
=======
# Product Roadmap: Detect-WiFi (Presence Detector)

## 1. Vision & Goals

**Vision:**
To become the definitive, privacy-focused Android solution for residential presence detection and anti-theft security. By leveraging multi-layer technology (WiFi, Bluetooth LE, Hotspot, Camera), we aim to bridge the gap between personal device monitoring and smart home automation, providing professional-grade reliability without sacrificing user privacy or battery life.

**Goals:**
*   **Reliability:** Ensure 99.9% uptime for background detection services across all supported Android versions (8.0+).
*   **Precision:** Minimize false alarms through dual-tech verification (WiFi + Bluetooth) and smart debouncing.
*   **Integration:** Seamlessly connect with major home automation platforms (Home Assistant, Node-RED) via MQTT.
*   **Privacy:** Process all data locally on the device, ensuring user location and habits remain private.
*   **User Experience:** Deliver an intuitive, "set-and-forget" experience with actionable insights.

## 2. Current Status

**Version:** 2.1 (Production Ready)
**Tech Stack:** Kotlin, Android 15 Target, Coroutines, Foreground Services.

**Key Achievements:**
*   ✅ **Robust Background Service:** Reliable detection even when the app is closed.
*   ✅ **Smart Notifications:** Debounced alerts to prevent spam (Arrival/Departure).
*   ✅ **Multi-Layer Detection:** Simultaneous WiFi, Bluetooth LE, and Hotspot scanning.
*   ✅ **Hotspot Recognition:** Identifies mobile hotspots (e.g., "iPhone (Hotspot)") even without connection.
*   ✅ **Anti-Theft Suite:** Motion, Pocket, and Charger alarms.
*   ✅ **UI Improvements:** Manual refresh, labeled device badges, and dark mode support.

**Metrics:**
*   **Open Issues:** 0 (Clean Slate)
*   **Urgency:** Medium (Feature Development & Enhancement)

## 3. Quarterly Roadmap (2026)

### Q1 2026: Experience & Visibility
*Focus: Refining the user interface and providing deeper insights into detection sources.*

*   **High Priority:**
    *   🎨 **UI/UX Polish:** Implement visual feedback for refresh actions (Toast + Animation) and complete the custom icon set.
    *   📱 **Device Management:** Implement "Sort by Labeled Devices" to prioritize known devices in the list.
*   **Medium Priority:**
    *   📶 **Bluetooth Visualization:** Show real-time signal strength and distinct badges for WiFi vs. Bluetooth detection sources.
    *   ⚡ **Battery Optimization:** Profile and optimize background scanning intervals to reduce drain by 15%.
*   **Low Priority:**
    *   Dependency updates and code cleanup.

### Q2 2026: Connectivity & Control
*Focus: Expanding integration and giving users granular control over notifications.*

*   **High Priority:**
    *   🔔 **Notification Channels:** Separate "Arrival", "Departure", and "Security Alert" channels for granular user control.
    *   🔗 **MQTT Integration:** Enable integration with Home Assistant/Mosquitto for automation triggers.
*   **Medium Priority:**
    *   ⏰ **Time-Based Rules:** Schedule notification suppression (e.g., "Don't notify departure between 8 AM - 6 PM").
    *   💾 **Room Database Migration:** Migrate from SharedPreferences to Room for robust history and device management.

### Q3 2026: Intelligence & Context
*Focus: Smarter detection and device understanding.*

*   **High Priority:**
    *   🧠 **Machine Learning Patterns:** Local ML model to learn user routines and suppress false alarms.
*   **Medium Priority:**
    *   🏷️ **Smart Device Categorization:** Automatically categorize devices (Phone, Watch, Laptop, Earbuds) based on MAC OUI and Bluetooth class.
    *   📍 **Multi-Location Support:** Profiles for different locations (Home, Office).
    *   🛡️ **Advanced Anti-Theft:** Trigger alarms based on specific BLE device absence (e.g., "Keys Left Behind").

### Q4 2026: Ecosystem Expansion
*Focus: New platforms and remote access.*

*   **Medium Priority:**
    *   🖥️ **Web Dashboard:** Local HTTP server for status viewing from a browser.
    *   ⌚ **Wear OS Companion:** Watch app for quick controls and wrist-based alerts.
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

### Bluetooth Visualization (Q1)
**User Value Proposition:**
Clarifies *how* a device was detected (WiFi vs. Bluetooth), increasing user confidence in the dual-tech system.
**Technical Approach:**
*   **UI Update:** Add source icons (WiFi/Bluetooth) to list items.
*   **Real-time Data:** Display BLE signal strength (RSSI) with color-coded indicators (Green/Yellow/Red).
*   **Success Criteria:** Users can instantly distinguish between WiFi-connected devices and BLE-detected devices.
*   **Estimated Effort:** Medium (1 week)

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

### Time-Based Rules (Q2)
**User Value Proposition:**
Reduces annoyance by silencing routine notifications during specific hours (e.g., work hours or sleep time).
**Technical Approach:**
*   **Settings UI:** Time picker for "Quiet Hours" or "Active Hours".
*   **Logic:** Check current time against schedule before triggering notification service.
*   **Success Criteria:** Notifications are suppressed during configured windows but logged in history.
*   **Estimated Effort:** Medium (1 week)

## 5. Dependencies & Risks

*   **Android Background Restrictions:** Future Android versions may restrict background processes further.
    *   *Mitigation:* Continuously monitor Android APIs and adapt Foreground Service types.
*   **Bluetooth Permissions:** Permission complexity increases with Android versions (Scan vs Connect).
    *   *Mitigation:* Maintain robust permission handling logic and educate users via UI prompts.
*   **Battery Impact:** Continuous dual scanning and MQTT can drain battery.
    *   *Mitigation:* Implement "Eco Mode" and user-configurable update intervals.
*   **Hardware Variability:** Bluetooth/WiFi performance varies across devices.
    *   *Mitigation:* Maintain a device compatibility list and allow sensitivity adjustments.
>>>>>>> 8d78a26eab8225dd04311bfe2aed1297b7db2d18
