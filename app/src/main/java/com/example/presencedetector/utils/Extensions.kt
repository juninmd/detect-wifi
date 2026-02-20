package com.example.presencedetector.utils

/**
 * Checks if the string contains any of the provided patterns.
 *
 * @param patterns The list of substrings to check for.
 * @return True if any of the patterns are found in the string, false otherwise.
 */
fun String.containsAny(patterns: List<String>): Boolean {
    return patterns.any { this.contains(it) }
}
