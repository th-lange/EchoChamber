package com.echochamber.engine.web.ingestion

import com.echochamber.engine.application.IngestionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Ingest endpoint for SnapReq (Agent.md §4).
 *
 * Authentication is handled upstream by `InternalAuthFilter` on the `/internal/` paths. The
 * endpoint always returns `202 Accepted` regardless of whether the request was stored or
 * dropped as a duplicate — SnapReq does not read the response body.
 */
@RestController
class IngestController(
    private val ingestionService: IngestionService,
) {

    @PostMapping("/internal/ingest")
    @ResponseStatus(HttpStatus.ACCEPTED)
    suspend fun ingest(@Valid @RequestBody payload: IngestRequestDto) {
        ingestionService.ingest(
            method = payload.method!!,
            uri = payload.uri!!,
            authority = payload.authority!!,
            headers = payload.headers!!,
            body = payload.body,
        )
    }
}
