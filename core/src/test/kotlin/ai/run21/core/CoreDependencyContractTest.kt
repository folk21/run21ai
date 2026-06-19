package ai.run21.core

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Verifies the architectural boundary of the core module by checking that runtime frameworks do
 * not appear in core build dependencies.
 */
class CoreDependencyContractTest {
    @Test
    fun `TEST-CORE-001 core gradle module has no runtime framework dependencies`() {
        val buildFile = java.io.File("build.gradle.kts").readText()
        val forbidden = listOf(
            "spring-boot",
            "ktor-server",
            "hibernate",
            "exposed",
            "picocli",
            "ollama",
        )

        forbidden.forEach { dependency ->
            assertFalse(buildFile.contains(dependency, ignoreCase = true), "Forbidden dependency found: $dependency")
        }
    }
}
