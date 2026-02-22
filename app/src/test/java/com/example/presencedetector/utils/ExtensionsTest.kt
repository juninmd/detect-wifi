package com.example.presencedetector.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtensionsTest {

    @Test
    fun containsAny_returnsTrue_whenStringContainsOnePattern() {
        val input = "This is a test string"
        val patterns = listOf("test", "example")
        assertTrue(input.containsAny(patterns))
    }

    @Test
    fun containsAny_returnsTrue_whenStringContainsMultiplePatterns() {
        val input = "This is a test example"
        val patterns = listOf("test", "example")
        assertTrue(input.containsAny(patterns))
    }

    @Test
    fun containsAny_returnsFalse_whenStringContainsNone() {
        val input = "This is a sample string"
        val patterns = listOf("test", "example")
        assertFalse(input.containsAny(patterns))
    }

    @Test
    fun containsAny_returnsFalse_whenPatternsListIsEmpty() {
        val input = "This is a test string"
        val patterns = emptyList<String>()
        assertFalse(input.containsAny(patterns))
    }

    @Test
    fun containsAny_returnsTrue_whenStringContainsPatternWithDifferentCase_ifCaseInsensitiveIsImplemented() {
        // Assuming implementation is case-sensitive based on `this.contains(it)`
        // If it should be case-insensitive, we would need to update the implementation.
        // For now, let's verify case sensitivity.
        val input = "This is a Test string"
        val patterns = listOf("test")
        assertFalse("Expected containsAny to be case-sensitive", input.containsAny(patterns))
    }
}
