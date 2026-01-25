package com.example.presencedetector.services

import android.content.Context
import com.example.presencedetector.utils.PreferencesUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TelegramServiceTest {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockPreferences: PreferencesUtil
    @Mock private lateinit var mockClient: OkHttpClient
    @Mock private lateinit var mockCall: Call

    private lateinit var telegramService: TelegramService
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // Reset Singleton for test isolation
        TelegramService.client = mockClient
        telegramService = TelegramService(mockContext, mockPreferences, testDispatcher)
    }

    @Test
    fun `sendMessage should execute request when enabled`() = runTest {
        // Given
        whenever(mockPreferences.isTelegramEnabled()).thenReturn(true)
        whenever(mockPreferences.getTelegramToken()).thenReturn("TOKEN")
        whenever(mockPreferences.getTelegramChatId()).thenReturn("12345")
        whenever(mockClient.newCall(any())).thenReturn(mockCall)

        val successResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
            .build()

        whenever(mockCall.execute()).thenReturn(successResponse)

        // When
        telegramService.sendMessage("Test Message")

        // Then
        verify(mockClient).newCall(any())
        verify(mockCall).execute()
    }

    @Test
    fun `sendMessage should not send if disabled`() = runTest {
        whenever(mockPreferences.isTelegramEnabled()).thenReturn(false)

        telegramService.sendMessage("Test")

        verify(mockClient, never()).newCall(any())
    }

    @Test
    fun `sendPhoto should execute request when enabled`() = runTest {
        // Given
        whenever(mockPreferences.isTelegramEnabled()).thenReturn(true)
        whenever(mockPreferences.getTelegramToken()).thenReturn("TOKEN")
        whenever(mockPreferences.getTelegramChatId()).thenReturn("12345")

        // Real temp file
        val tempFile = File.createTempFile("test", ".jpg")

        whenever(mockClient.newCall(any())).thenReturn(mockCall)
        val successResponse = Response.Builder()
            .request(Request.Builder().url("https://example.com").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
            .build()
        whenever(mockCall.execute()).thenReturn(successResponse)

        // When
        telegramService.sendPhoto(tempFile, "Caption")

        // Then
        verify(mockClient).newCall(any())

        tempFile.delete()
    }
}
