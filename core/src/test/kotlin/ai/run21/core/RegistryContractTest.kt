package ai.run21.core

import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.UnsupportedToolExecutor
import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Verifies the in-memory registry contracts used by tests and lightweight runtime wiring.
 */
class RegistryContractTest {
    @Test
    fun `tool registry registers descriptors and preserves insertion order`() {
        val first = descriptor("first-tool")
        val second = descriptor("second-tool")
        val registry = InMemoryToolRegistry()

        registry.register(first)
        registry.register(second)

        assertEquals(first, registry.find(first.code))
        assertEquals(second, registry.find(second.code))
        assertEquals(listOf(first, second), registry.all())
    }

    @Test
    fun `tool registry replaces descriptor by tool code without changing order`() {
        val original = descriptor("test-tool", name = "Original Tool")
        val replacement = descriptor("test-tool", name = "Replacement Tool")
        val second = descriptor("second-tool")
        val registry = InMemoryToolRegistry(listOf(original, second))

        registry.register(replacement)

        assertEquals(replacement, registry.find(original.code))
        assertEquals(listOf(replacement, second), registry.all())
    }

    @Test
    fun `executor registry registers executors and preserves insertion order`() {
        val firstDescriptor = descriptor("first-tool")
        val secondDescriptor = descriptor("second-tool")
        val firstExecutor = UnsupportedToolExecutor(firstDescriptor.code, firstDescriptor)
        val secondExecutor = UnsupportedToolExecutor(secondDescriptor.code, secondDescriptor)
        val registry = InMemoryToolExecutorRegistry()

        registry.register(firstExecutor)
        registry.register(secondExecutor)

        assertSame(firstExecutor, registry.find(firstDescriptor.code))
        assertSame(secondExecutor, registry.find(secondDescriptor.code))
        assertEquals(listOf(firstExecutor, secondExecutor), registry.all())
    }

    @Test
    fun `executor registry replaces executor by tool code without changing order`() {
        val descriptor = descriptor("test-tool")
        val secondDescriptor = descriptor("second-tool")
        val original = UnsupportedToolExecutor(descriptor.code, descriptor)
        val replacement = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = InMemoryToolImplementationRegistry(listOf(EchoImplementation(toolCode = descriptor.code))),
        )
        val second = UnsupportedToolExecutor(secondDescriptor.code, secondDescriptor)
        val registry = InMemoryToolExecutorRegistry(listOf(original, second))

        registry.register(replacement)

        assertSame(replacement, registry.find(descriptor.code))
        assertEquals(listOf(replacement, second), registry.all())
    }

    @Test
    fun `implementation registry returns all implementations and all implementations for a tool`() {
        val firstTool = ToolCode("first-tool")
        val secondTool = ToolCode("second-tool")
        val first = EchoImplementation(toolCode = firstTool, implementationCode = ImplementationCode("first-a"))
        val second = EchoImplementation(toolCode = firstTool, implementationCode = ImplementationCode("first-b"))
        val third = EchoImplementation(toolCode = secondTool, implementationCode = ImplementationCode("second-a"))
        val registry = InMemoryToolImplementationRegistry(listOf(first, second, third))

        assertEquals(listOf(first, second, third), registry.all())
        assertEquals(listOf(first, second), registry.allFor(firstTool))
        assertEquals(listOf(third), registry.allFor(secondTool))
        assertEquals(emptyList(), registry.allFor(ToolCode("unknown-tool")))
    }

    @Test
    fun `implementation registry returns first registered implementation as default`() {
        val toolCode = ToolCode("test-tool")
        val first = EchoImplementation(toolCode = toolCode, implementationCode = ImplementationCode("first"))
        val second = EchoImplementation(toolCode = toolCode, implementationCode = ImplementationCode("second"))
        val registry = InMemoryToolImplementationRegistry(listOf(first, second))

        assertSame(first, registry.find(toolCode))
        assertSame(first, registry.find(toolCode, ImplementationCode("first")))
        assertSame(second, registry.find(toolCode, ImplementationCode("second")))
    }

    @Test
    fun `implementation registry replaces implementation by implementation code without changing order`() {
        val toolCode = ToolCode("test-tool")
        val original = EchoImplementation(
            toolCode = toolCode,
            implementationCode = ImplementationCode("echo"),
            outputKey = "original",
        )
        val replacement = EchoImplementation(
            toolCode = toolCode,
            implementationCode = ImplementationCode("echo"),
            outputKey = "replacement",
        )
        val second = EchoImplementation(toolCode = toolCode, implementationCode = ImplementationCode("second"))
        val registry = InMemoryToolImplementationRegistry(listOf(original, second))

        registry.register(replacement)

        assertSame(replacement, registry.find(toolCode))
        assertSame(replacement, registry.find(toolCode, ImplementationCode("echo")))
        assertEquals(listOf(replacement, second), registry.allFor(toolCode))
    }

    private fun descriptor(
        code: String,
        name: String = code,
    ): ToolDescriptor = ToolDescriptor(
        code = ToolCode(code),
        name = name,
        description = "Descriptor for $code.",
        status = CapabilityStatus.DONE,
    )
}
