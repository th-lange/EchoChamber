package com.echochamber.engine.domain.model

/**
 * A one-off, per-request override applied at replay time ("modify-before-retry").
 *
 * Pure domain value object — no framework dependencies. Applied to a [MutableRequest] copy
 * by [com.echochamber.engine.domain.mutation.RequestOverrideApplier]; the original
 * [CapturedRequest] is never modified (Agent.md §2).
 */
data class RequestOverride(
    /** Replaces scheme + authority (host[:port]) of the target URL. */
    val targetUrl: String? = null,
    /** Replaces the URL path component (query string preserved). */
    val pathOverride: String? = null,
    /** Headers to add or override (case-insensitive on name). */
    val headersSet: Map<String, String> = emptyMap(),
    /** Header names to remove (case-insensitive). */
    val headersRemove: List<String> = emptyList(),
    /** Top-level JSON field patches: field name -> new (string) value. */
    val bodyPatches: Map<String, String> = emptyMap(),
) {
    val isEmpty: Boolean
        get() = targetUrl == null && pathOverride == null &&
            headersSet.isEmpty() && headersRemove.isEmpty() && bodyPatches.isEmpty()
}
