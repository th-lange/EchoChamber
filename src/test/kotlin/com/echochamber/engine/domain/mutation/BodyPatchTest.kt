package com.echochamber.engine.domain.mutation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class BodyPatchTest {

    @Test
    fun `applyPlaceholders replaces known tokens and leaves unknown`() {
        val out = BodyPatch.applyPlaceholders("""{"id":"{{id}}","x":"{{missing}}"}""", mapOf("id" to "7"))
        assertEquals("""{"id":"7","x":"{{missing}}"}""", out)
    }

    @Test
    fun `applyPlaceholders on null is null`() {
        assertNull(BodyPatch.applyPlaceholders(null, mapOf("a" to "b")))
    }

    @Test
    fun `applyFieldPatches replaces string and numeric field values`() {
        val out = BodyPatch.applyFieldPatches("""{"userId":1,"name":"old"}""", mapOf("userId" to "42", "name" to "new"))
        assertEquals("""{"userId":"42","name":"new"}""", out)
    }

    @Test
    fun `applyFieldPatches leaves missing fields unchanged`() {
        val out = BodyPatch.applyFieldPatches("""{"a":1}""", mapOf("b" to "2"))
        assertEquals("""{"a":1}""", out)
    }
}
