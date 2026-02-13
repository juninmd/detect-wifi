package com.example.presencedetector.utils

/**
 * Utility extension functions.
 */

/**
 * Checks if the string contains any of the provided patterns.
 */
fun String.containsAny(patterns: List<String>, ignoreCase: Boolean = false): Boolean {
    return patterns.any { this.contains(it, ignoreCase = ignoreCase) }
}
