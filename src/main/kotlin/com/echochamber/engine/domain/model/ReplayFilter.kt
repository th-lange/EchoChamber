package com.echochamber.engine.domain.model

import java.time.Instant

data class ReplayFilter(
    val method: String? = null,
    val uriPattern: String? = null,
    val authority: String? = null,
    val capturedAfter: Instant? = null,
    val capturedBefore: Instant? = null
)
