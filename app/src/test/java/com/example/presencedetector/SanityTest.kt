package com.example.presencedetector

import org.junit.Test
import org.junit.Assert.*

class SanityTest {
    @Test
    fun testMath() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testPackageName() {
        // Just a sanity check to ensure we can access classes if needed
        val pkg = "com.example.presencedetector"
        assertEquals("com.example.presencedetector", pkg)
    }
}
