package com.example.presencedetector.security.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraChannelTest {

  @Test
  fun `test rtspUrl generation`() {
    val channel =
      CameraChannel(
        id = 1,
        name = "Test Cam",
        host = "192.168.1.100",
        port = 554,
        channel = 2,
        username = "admin",
        password = "password",
        streamPath = "cam/realmonitor"
      )

    val expectedUrl = "rtsp://admin:password@192.168.1.100:554/cam/realmonitor?channel=2&subtype=0"
    assertEquals(expectedUrl, channel.rtspUrl)

    val expectedSubUrl =
      "rtsp://admin:password@192.168.1.100:554/cam/realmonitor?channel=2&subtype=1"
    assertEquals(expectedSubUrl, channel.rtspUrlSubstream)
  }

  @Test
  fun `test rtspUrl generation without auth`() {
    val channel =
      CameraChannel(id = 1, name = "Test Cam", host = "192.168.1.100", username = "", password = "")

    val expectedUrl = "rtsp://192.168.1.100:554/cam/realmonitor?channel=1&subtype=0"
    assertEquals(expectedUrl, channel.rtspUrl)
  }

  @Test
  fun `test fromUrl with auth`() {
    val url = "rtsp://admin:password@192.168.1.100:554/cam/realmonitor?channel=2"
    val channel = CameraChannel.fromUrl(1, "Test Cam", url)

    assertEquals("admin", channel.username)
    assertEquals("password", channel.password)
    assertEquals("192.168.1.100", channel.host)
    assertEquals(554, channel.port)
    assertEquals(2, channel.channel)
    assertEquals("cam/realmonitor", channel.streamPath)
  }

  @Test
  fun `test fromUrl without auth`() {
    val url = "rtsp://192.168.1.100:554/cam/realmonitor?channel=1"

    val channel = CameraChannel.fromUrl(1, "Test Cam", url)

    assertEquals("", channel.username)
    assertEquals("", channel.password)
    assertEquals("192.168.1.100", channel.host)
  }

  @Test
  fun `test fromUrl invalid`() {
    val url = "http://invalid-url"
    val channel = CameraChannel.fromUrl(1, "Test Cam", url)

    assertEquals("", channel.host)
  }
}
