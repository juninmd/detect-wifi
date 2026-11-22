package com.example.presencedetector.utils

import com.example.presencedetector.model.DeviceCategory

/** Utility to classify devices based on SSID and BSSID patterns. */
object DeviceClassifier {

  private object Patterns {
    val SMARTPHONE_HIGH_CONFIDENCE = setOf("iphone", "android", "galaxy", "note", "pixel")
    val E_READERS = setOf("kindle", "ebook")
    val SMART_HOME_ASSISTANTS = setOf("alexa", "echo", "amazon")
    val SMART_LIGHTS =
      setOf("light", "bulb", "hue", "tuya", "smart life", "yeelight", "continua", "batcaverna")
    val SMART_TVS =
      setOf("tv", "samsung", "lg", "sony", "bravia", "firestick", "chromecast", "roku")
    val ROUTERS =
      setOf("2.4g", "5g", "router", "gateway", "tp-link", "d-link", "familia", "adriana")
    val MOBILE_HOTSPOT_PATTERNS =
      setOf(
        "iphone",
        "android",
        "samsung",
        "xiaomi",
        "redmi",
        "oneplus",
        "pixel",
        "motorola",
        "huawei",
        "poco",
        "nokia",
        "realme",
        "vivo",
        "oppo",
        "honor",
        "personal",
        "hotspot",
        "moto",
        "galaxy",
        "note",
        "12 pro",
        "s21",
        "s22",
        "note 20",
        "iphone 13",
      )
  }

  // Pre-compiled regex for hotspot detection
  private val SHORT_NAME_REGEX = Regex("[A-Za-z0-9]+")
  private val CAMEL_BOUNDARY = Regex("(?<=[a-z])(?=[A-Z])")
  private val NON_ALNUM = Regex("[^a-z0-9]+")
  private val SHORT_WORD = Regex("[a-z0-9]{1,4}")
  // Longer patterns that are also common word endings ("notebook", "netbook").
  private val WORD_ONLY = setOf("ebook")

  fun classify(ssid: String, bssid: String, isHotspot: Boolean = false): DeviceCategory {
    if (isHotspot) {
      return DeviceCategory.SMARTPHONE
    }

    val name = SsidText(ssid)

    return when {
      name.containsAny(Patterns.SMARTPHONE_HIGH_CONFIDENCE) -> DeviceCategory.SMARTPHONE
      name.containsAny(Patterns.E_READERS) -> DeviceCategory.KINDLE
      name.containsAny(Patterns.SMART_HOME_ASSISTANTS) -> DeviceCategory.ALEXA
      name.containsAny(Patterns.SMART_LIGHTS) -> DeviceCategory.SMART_LIGHT
      name.containsAny(Patterns.SMART_TVS) -> DeviceCategory.SMART_TV
      // Check hotspot patterns. If it matches, classify as Smartphone (assuming hotspots are mostly
      // phones)
      name.containsAny(Patterns.MOBILE_HOTSPOT_PATTERNS) -> DeviceCategory.SMARTPHONE
      name.containsAny(Patterns.ROUTERS) -> DeviceCategory.ROUTER
      else -> DeviceCategory.UNKNOWN
    }
  }

  /**
   * Detects if SSID looks like a mobile hotspot. Encapsulates logic for hotspot detection including
   * patterns and heuristics.
   */
  fun isMobileHotspot(ssid: String): Boolean {
    // Check if contains mobile patterns
    val containsMobilePattern = SsidText(ssid).containsAny(Patterns.MOBILE_HOTSPOT_PATTERNS)

    // Check if SSID is very short (typical for hotspots)
    val isShortName = ssid.length < 15 && !ssid.contains("_") && !ssid.contains("-")

    return containsMobilePattern || (isShortName && ssid.matches(SHORT_NAME_REGEX))
  }

  /**
   * Short single-word patterns ("lg", "tv", "hue", "note", "5g", plus "ebook") must match a whole
   * word: as substrings they hit "Algar", "Ativa", "Hueston", "Notebook". Longer or multi-word
   * patterns still match anywhere, so concatenated names like "SamsungTV" keep working.
   */
  private class SsidText(ssid: String) {
    val lower = ssid.lowercase()
    val words: Set<String> =
      ssid
        .replace(CAMEL_BOUNDARY, " ")
        .lowercase()
        .split(NON_ALNUM)
        .filter { it.isNotEmpty() }
        .toSet()

    fun containsAny(patterns: Collection<String>): Boolean =
      patterns.any {
        if (SHORT_WORD.matches(it) || it in WORD_ONLY) it in words else lower.contains(it)
      }
  }
}
