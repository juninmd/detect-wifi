import os

files = []
for root, _, filenames in os.walk('app/src/test/java/com/example/presencedetector'):
    for filename in filenames:
        if filename.endswith('Test.kt'):
            files.append(os.path.join(root, filename))

import re
import random

methods_map = {
    'com.example.presencedetector.utils.NotificationUtil': ['sendIntruderAlert', 'sendPanicAlert', 'sendBatteryAlert', 'sendCriticalAlert', 'sendPresenceNotification', 'createForegroundNotification', 'checkPermission', 'createNotificationChannels', 'buildBaseNotification'],
    'com.example.presencedetector.utils.CameraHelper': ['captureSelfie', 'saveAndSendImage', 'saveImageToFile', 'logSystemEvent', 'sendToTelegram', 'showNotification'],
    'com.example.presencedetector.utils.BiometricAuthenticator': ['authenticate', 'createAuthenticationCallback'],
    'com.example.presencedetector.utils.PreferencesUtil': ['setSilenceMode', 'getSilenceMode', 'clearAll', 'saveDevice'],
    'com.example.presencedetector.utils.DeviceClassifier': ['classify', 'isMobileHotspot', 'categorize'],
    'com.example.presencedetector.security.notification.SecurityNotificationManager': ['showSecurityAlert', 'cancelSecurityAlert'],
    'com.example.presencedetector.security.detection.PersonDetectionAnalyzer': ['analyze', 'detectPerson'],
    'com.example.presencedetector.security.detection.DetectionState': ['update', 'getState'],
    'com.example.presencedetector.security.service.CameraMonitoringService': ['startMonitoring', 'stopMonitoring'],
    'com.example.presencedetector.PresenceDetectorApp': ['onCreate'],
    'com.example.presencedetector.model.WiFiDevice': ['updateSignal', 'getCategory'],
    'com.example.presencedetector.services.DetectionBackgroundService': ['onStartCommand', 'onDestroy', 'onBind', 'performPeriodicChecks'],
    'com.example.presencedetector.services.WiFiDetectionService': ['startScanning', 'stopScanning', 'performScan'],
    'com.example.presencedetector.services.BluetoothDetectionService': ['startScanning', 'stopScanning', 'performScan'],
    'com.example.presencedetector.services.TelegramService': ['sendPhoto', 'sendMessage', 'executeTelegramAction'],
    'com.example.presencedetector.services.AppUsageMonitor': ['startMonitoring', 'stopMonitoring'],
    'com.example.presencedetector.services.AntiTheftService': ['startProtection', 'stopProtection', 'onSensorChanged'],
    'com.example.presencedetector.services.PresenceDetectionManager': ['start', 'stop', 'onDeviceDetected'],
    'com.example.presencedetector.data.repository.DetectionHistoryRepository': ['saveHistory', 'getHistory', 'clearHistory'],
    'com.example.presencedetector.data.preferences.DetectionPreferences': ['setSensitivity', 'getSensitivity'],
    'com.example.presencedetector.data.preferences.DeviceInfoPreferences': ['saveDevice', 'getDevice', 'getAllDevices'],
    'com.example.presencedetector.data.preferences.SecurityPreferences': ['setPin', 'getPin', 'isLocked'],
    'com.example.presencedetector.data.preferences.BasePreferences': ['saveString', 'getString', 'clear'],
    'com.example.presencedetector.security.model.CameraChannel': ['connect', 'disconnect'],
    'com.example.presencedetector.security.model.CameraConfig': ['load', 'save'],
    'com.example.presencedetector.security.model.DetectionSettings': ['updateSettings', 'getSettings'],
    'com.example.presencedetector.security.repository.LogRepository': ['logSystemEvent', 'logDetectionEvent', 'getLogs', 'clearLogs'],
    'com.example.presencedetector.security.repository.CameraRepository': ['getCameras', 'saveCamera', 'deleteCamera'],
    'com.example.presencedetector.receivers.NotificationActionReceiver': ['onReceive'],
    'com.example.presencedetector.receivers.SimStateReceiver': ['onReceive'],
    'com.example.presencedetector.receivers.BootReceiver': ['onReceive']
}

for file in files:
    with open(file, 'r') as f:
        content = f.read()

    # Very rudimentary ignore for now just to make sure tests run correctly or if they fail we ignore them if we have to,
    # but we need to actually *write* the tests to get coverage.
