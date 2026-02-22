package com.example.presencedetector.services

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignalHistoryManagerTest {

    @Before
    fun setUp() {
        SignalHistoryManager.clear()
    }

    @After
    fun tearDown() {
        SignalHistoryManager.clear()
    }

    @Test
    fun addPoint_addsEntryToHistory() {
        val bssid = "test:mac:address"
        val level = -50

        SignalHistoryManager.addPoint(bssid, level)

        val history = SignalHistoryManager.getHistory(bssid)
        assertEquals(1, history.size)
        assertEquals(level, history[0].second)
    }

    @Test
    fun addPoint_limitsHistorySize() {
        val bssid = "test:mac:address"

        // Add 65 points (max is 60)
        for (i in 1..65) {
            SignalHistoryManager.addPoint(bssid, -i)
        }

        val history = SignalHistoryManager.getHistory(bssid)
        assertEquals(60, history.size)

        // The last added point should be the last in the list (or depending on impl, the latest)
        // Impl: list.add(pair) -> appends to end. removeFirst() removes oldest.
        // So the list should contain -6 to -65.
        // history.last() should be -65.

        assertEquals(-65, history.last().second)
        assertEquals(-6, history.first().second)
    }

    @Test
    fun getHistory_returnsEmptyListForUnknownBssid() {
        val history = SignalHistoryManager.getHistory("unknown:bssid")
        assertTrue(history.isEmpty())
    }

    @Test
    fun clear_removesAllHistory() {
        SignalHistoryManager.addPoint("bssid1", -50)
        SignalHistoryManager.addPoint("bssid2", -60)

        SignalHistoryManager.clear()

        assertTrue(SignalHistoryManager.getHistory("bssid1").isEmpty())
        assertTrue(SignalHistoryManager.getHistory("bssid2").isEmpty())
    }
}
