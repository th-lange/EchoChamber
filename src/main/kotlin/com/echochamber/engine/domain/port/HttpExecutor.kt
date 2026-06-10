package com.echochamber.engine.domain.port

import com.echochamber.engine.domain.model.ExecutionResult
import com.echochamber.engine.domain.model.MutableRequest

/**
 * Transport port for firing a mutated request at the target system and capturing the response.
 *
 * Implementations (e.g. a WebClient-based adapter) are responsible for measuring
 * `responseTimeMs` and translating transport-level errors into a [ExecutionResult] with
 * an appropriate [com.echochamber.engine.domain.model.ExecutionStatus]. They must never
 * throw out of [execute] for normal HTTP failures — callers depend on a result object.
 */
interface HttpExecutor {
    suspend fun execute(request: MutableRequest): ExecutionResult
}
