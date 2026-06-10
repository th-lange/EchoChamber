package com.echochamber.engine.adapter.persistence.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for the append-only `captured_requests` table.
 *
 * Headers are stored as a JSON-serialised TEXT column; serialisation is handled by the
 * adapter / mapper, not by JPA, to keep entity classes free of conversion logic.
 */
@Entity
@Table(name = "captured_requests")
class CapturedRequestEntity(

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    var id: UUID,

    @Column(name = "captured_at", nullable = false)
    var capturedAt: Instant,

    @Column(name = "method", nullable = false, length = 10)
    var method: String,

    @Column(name = "uri", nullable = false)
    var uri: String,

    @Column(name = "authority", nullable = false, length = 255)
    var authority: String,

    @Column(name = "headers", nullable = false, columnDefinition = "TEXT")
    var headersJson: String,

    @Column(name = "body", columnDefinition = "TEXT")
    var body: String?
)
