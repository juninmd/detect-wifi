package com.example.presencedetector.services

import android.content.Context
import android.util.Log
import com.example.presencedetector.utils.PreferencesUtil
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.Executors

class MqttManager(private val context: Context) {
    companion object {
        private const val TAG = "MqttManager"
    }

    private var mqttClient: MqttClient? = null
    private val preferences = PreferencesUtil(context)
    private val executor = Executors.newSingleThreadExecutor()

    fun connect() {
        if (!preferences.isMqttEnabled()) return

        val host = preferences.getMqttHost()
        val port = preferences.getMqttPort()
        if (host.isEmpty()) return

        val serverUri = "tcp://$host:$port"
        val clientId = "PresenceDetectorAndroid_" + System.currentTimeMillis()

        try {
            mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())
            val options = MqttConnectOptions()
            options.isAutomaticReconnect = true
            options.isCleanSession = true
            options.connectionTimeout = 10

            val user = preferences.getMqttUser()
            val pass = preferences.getMqttPass()
            if (user.isNotEmpty()) {
                options.userName = user
                options.password = pass.toCharArray()
            }

            mqttClient?.setCallback(object : MqttCallbackExtended {
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "Connection lost", cause)
                }

                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    // Handle incoming messages if needed
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // Message sent
                }

                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.i(TAG, "Connected to MQTT Broker: $serverURI")
                }
            })

            executor.execute {
                try {
                    if (mqttClient?.isConnected == false) {
                        mqttClient?.connect(options)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to connect to MQTT", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MQTT Client", e)
        }
    }

    fun publish(subTopic: String, message: String) {
        if (!preferences.isMqttEnabled() || mqttClient == null || mqttClient?.isConnected == false) {
             // Try to reconnect if enabled but not connected
             if (preferences.isMqttEnabled() && mqttClient == null) {
                 connect()
             }
             return
        }

        val topicPrefix = preferences.getMqttTopic().removeSuffix("/")
        val fullTopic = "$topicPrefix/$subTopic"

        executor.execute {
            try {
                val mqttMessage = MqttMessage(message.toByteArray())
                mqttMessage.qos = 1
                mqttMessage.isRetained = false
                mqttClient?.publish(fullTopic, mqttMessage)
                Log.d(TAG, "Published to $fullTopic: $message")
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing message", e)
            }
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting", e)
        }
    }
}
