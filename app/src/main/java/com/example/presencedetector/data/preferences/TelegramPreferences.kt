package com.example.presencedetector.data.preferences

import android.content.Context

class TelegramPreferences(context: Context) : BasePreferences(context, PREF_NAME) {

    companion object {
        const val PREF_NAME = "presence_detector_prefs"
        private const val KEY_TELEGRAM_ENABLED = "telegram_enabled"
        private const val KEY_TELEGRAM_TOKEN = "telegram_token"
        private const val KEY_TELEGRAM_CHAT_ID = "telegram_chat_id"
    }

    fun setTelegramEnabled(enabled: Boolean) = putBoolean(KEY_TELEGRAM_ENABLED, enabled)

    fun isTelegramEnabled() = getBoolean(KEY_TELEGRAM_ENABLED, false)

    fun setTelegramToken(token: String) = putString(KEY_TELEGRAM_TOKEN, token)

    fun getTelegramToken(): String? = getString(KEY_TELEGRAM_TOKEN)

    fun setTelegramChatId(chatId: String) = putString(KEY_TELEGRAM_CHAT_ID, chatId)

    fun getTelegramChatId(): String? = getString(KEY_TELEGRAM_CHAT_ID)
}
