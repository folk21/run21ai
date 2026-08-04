package ai.run21.core

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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Verifies fail-fast registry behavior and explicit implementation defaults. */
class RegistryContractTest {
    @Test
    fun `tool registry preserves insertion order`() {
        val first = descriptor("first-tool")
        val second = descriptor("second-tool")
        val registry = InMemoryToolRegistry(listOf(first, second))

        assertEquals(first, registry.find(first.code))
        assertEquals(second, registry.find(second.code))
        assertEquals(listOf(first, second), registry.all())
    }

    @Test
    fun `tool registry rejects duplicate descriptor codes`() {
        val registry = InMemoryToolRegistry(listOf(descriptor("test-tool")))

        val error = assertFailsWith<IllegalArgumentException> {
            registry.register(descriptor("test-tool", name = "Duplicate"))
        }

        assertEquals("Duplicate tool descriptor registration: test-tool.", error.message)
    }

    @Test
    fun `executor registry preserves insertion order`() {
        val firstDescriptor = descriptor("first-tool")
        val secondDescriptor = descriptor("second-tool")
        val first = UnsupportedToolExecutor(firstDescriptor.code, firstDescriptor)
        val second = UnsupportedToolExecutor(secondDescriptor.code, secondDescriptor)
        val registry = InMemoryToolExecutorRegistry(listOf(first, second))

        assertSame(first, registry.find(firstDescriptor.code))
        assertSame(second, registry.find(secondDescriptor.code))
        assertEquals(listOf(first, second), registry.all())
    }

    @Test
    fun `executor registry rejects duplicate tool codes`() {
        val descriptor = descriptor("test-tool")
        val registry = InMemoryToolExecutorRegistry(listOf(UnsupportedToolExecutor(descriptor.code, descriptor)))

        val error = assertFailsWith<IllegalArgumentException> {
            registry.register(UnsupportedToolExecutor(descriptor.code, descriptor))
        }

        assertEquals("Duplicate tool executor registration: test-tool.", error.message)
    }

    @Test
    fun `implementation registry exposes implementations by tool`() {
        val firstTool = ToolCode("first-tool")
        val secondTool = ToolCode("second-tool")
        val first = EchoImplementation(firstTool, ImplementationCode("first-a"))
        val second = EchoImplementation(firstTool, ImplementationCode("first-b"))
        val third = EchoImplementation(secondTool, ImplementationCode("second-a"))
        val registry = InMemoryToolImplementationRegistry(listOf(first, second, third))

        assertEquals(listOf(first, second, third), registry.all())
        assertEquals(listOf(first, second), registry.allFor(firstTool))
        assertEquals(listOf(third), registry.allFor(secondTool))
    }

    @Test
    fun `implementation selection requires an explicit default`() {
        val toolCode = ToolCode("test-tool")
        val first = EchoImplementation(toolCode, ImplementationCode("first"))
        val second = EchoImplementation(toolCode, ImplementationCode("second"))
        val registry = InMemoryToolImplementationRegistry(listOf(first, second))

        assertNull(registry.find(toolCode))
        assertSame(first, registry.find(toolCode, ImplementationCode("first")))
        assertSame(second, registry.find(toolCode, ImplementationCode("second")))
    }

    @Test
    fun `implementation registry resolves the explicit default`() {
        val toolCode = ToolCode("test-tool")
        val first = EchoImplementation(toolCode, ImplementationCode("first"))
        val second = EchoImplementation(toolCode, ImplementationCode("second"))
        val registry = InMemoryToolImplementationRegistry(
            implementations = listOf(first, second),
            defaultImplementations = mapOf(toolCode to second.descriptor.implementationCode),
        )

        assertEquals(second.descriptor.implementationCode, registry.defaultImplementation(toolCode))
        assertSame(second, registry.find(toolCode))
    }

    @Test
    fun `implementation registry rejects duplicate implementation codes`() {
        val toolCode = ToolCode("test-tool")
        val registry = InMemoryToolImplementationRegistry(
            listOf(EchoImplementation(toolCode, ImplementationCode("echo"))),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            registry.register(EchoImplementation(toolCode, ImplementationCode("echo")))
        }

        assertEquals("Duplicate tool implementation registration: test-tool/echo.", error.message)
    }

    @Test
    fun `implementation registry rejects missing or duplicate defaults`() {
        val toolCode = ToolCode("test-tool")
        val implementation = EchoImplementation(toolCode, ImplementationCode("echo"))
        val registry = InMemoryToolImplementationRegistry(listOf(implementation))

        val missing = assertFailsWith<IllegalArgumentException> {
            registry.registerDefault(toolCode, ImplementationCode("missing"))
        }
        assertEquals("Default implementation missing is not registered for tool test-tool.", missing.message)

        registry.registerDefault(toolCode, implementation.descriptor.implementationCode)
        val duplicate = assertFailsWith<IllegalArgumentException> {
            registry.registerDefault(toolCode, implementation.descriptor.implementationCode)
        }
        assertEquals("Default implementation is already registered for tool test-tool.", duplicate.message)
    }

    private fun descriptor(code: String, name: String = code): ToolDescriptor = ToolDescriptor(
        code = ToolCode(code),
        name = name,
        description = "Descriptor for $code.",
        status = CapabilityStatus.DONE,
    )
}
