package com.echochamber.engine.domain.model

import java.time.Instant
import java.util.UUID

data class MutableRequest(
    val id: UUID,
    val capturedAt: Instant,
    var method: String,
    var uri: String,
    var authority: String,
    var headers: Map<String, String>,
    var body: String?
)

fun CapturedRequest.toMutable(): MutableRequest = MutableRequest(
    id = id,
    capturedAt = capturedAt,
    method = method,
    uri = uri,
    authority = authority,
    headers = headers.toMap(),
    body = body
)
