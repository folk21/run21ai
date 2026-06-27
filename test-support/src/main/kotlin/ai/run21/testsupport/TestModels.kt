package ai.run21.testsupport

import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType

/**
 * Creates a deterministic actor reference for module-level tests.
 */
fun testActor(
    id: String = "test-actor",
    type: ActorType = ActorType.HUMAN,
    displayName: String? = "Test Actor",
): ActorReference = ActorReference(
    id = id,
    type = type,
    displayName = displayName,
)

/**
 * Creates a deterministic execution source for module-level tests.
 */
fun testSource(
    type: ExecutionSourceType = ExecutionSourceType.TEST,
    name: String? = "test",
    metadata: Map<String, Any?> = emptyMap(),
): ExecutionSource = ExecutionSource(
    type = type,
    name = name,
    metadata = metadata,
)
