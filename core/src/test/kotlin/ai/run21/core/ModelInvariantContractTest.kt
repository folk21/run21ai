package ai.run21.core

import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.CorrelationMetadata
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Verifies model invariants that protect core validators from malformed identifiers.
 */
class ModelInvariantContractTest {
    @Test
    fun `identifier models reject blank values`() {
        assertFailsWith<IllegalArgumentException> { ToolCode("") }
        assertFailsWith<IllegalArgumentException> { DispatcherCode("") }
        assertFailsWith<IllegalArgumentException> { ImplementationCode("") }
    }

    @Test
    fun `actor reference rejects blank actor id`() {
        assertFailsWith<IllegalArgumentException> {
            ActorReference(
                id = "",
                type = ActorType.HUMAN,
            )
        }
    }

    @Test
    fun `correlation metadata rejects blank identifiers`() {
        assertFailsWith<IllegalArgumentException> {
            CorrelationMetadata(executionId = "")
        }
        assertFailsWith<IllegalArgumentException> {
            CorrelationMetadata(correlationId = "")
        }
    }
}
