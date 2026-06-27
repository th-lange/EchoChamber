package com.echochamber.engine.domain.mutation

/**
 * Pure body-string transforms shared between the placeholder mutation handler (TICKET-009)
 * and the inline body-patch path of modify-before-retry (TICKET-008/014). No framework
 * dependencies — only the Kotlin stdlib.
 */
object BodyPatch {

    private val PLACEHOLDER = Regex("""\{\{(\w+)\}\}""")

    /** Replace `{{key}}` tokens using [params]; unknown tokens are left as-is. */
    fun applyPlaceholders(text: String?, params: Map<String, String>): String? {
        if (text == null || params.isEmpty()) return text
        return PLACEHOLDER.replace(text) { m -> params[m.groupValues[1]] ?: m.value }
    }

    /**
     * Replace the value of top-level JSON fields named by [patches] keys with the given
     * (string) values. Fields not present are left unchanged (no error). Intended for
     * simple field edits, not arbitrary JSON-path manipulation.
     */
    fun applyFieldPatches(body: String?, patches: Map<String, String>): String? {
        if (body == null || patches.isEmpty()) return body
        var result: String = body
        for ((key, value) in patches) {
            val regex = Regex(
                "(\"" + Regex.escape(key) + "\"\\s*:\\s*)" +
                    "(\"(?:\\\\.|[^\"\\\\])*\"|true|false|null|-?\\d+(?:\\.\\d+)?)",
            )
            result = regex.replace(result) { m -> m.groupValues[1] + "\"" + value + "\"" }
        }
        return result
    }
}
