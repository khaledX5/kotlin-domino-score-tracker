package io.github.khaledx5.dominoscore.domain.model

import io.github.khaledx5.dominoscore.domain.name
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerNameTest {

    @Test
    fun trimsSurroundingSpaces() {
        assertEquals("Omar", name("  Omar ").value)
    }

    @Test
    fun rejectsEmptyAndWhitespaceOnlyNames() {
        assertEquals(Outcome.Failure(PlayerNameError.BLANK), PlayerName.of(""))
        assertEquals(Outcome.Failure(PlayerNameError.BLANK), PlayerName.of("   "))
    }

    @Test
    fun acceptsTheMaximumLengthAfterTrimming() {
        val longest = "a".repeat(PlayerName.MAX_LENGTH)

        assertEquals(longest, name("  $longest  ").value)
        assertEquals(Outcome.Failure(PlayerNameError.TOO_LONG), PlayerName.of(longest + "a"))
    }

    @Test
    fun acceptsArabicNames() {
        assertEquals("خالد", name("خالد").value)
    }

    @Test
    fun matchesIgnoringCase() {
        assertTrue(name("Ali").matches(name("aLI")))
        assertFalse(name("Ali").matches(name("Alia")))
    }
}
