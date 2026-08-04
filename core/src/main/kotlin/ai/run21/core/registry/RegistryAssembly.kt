package ai.run21.core.registry

import ai.run21.core.execution.ToolExecutor
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor

/** Stable issue codes produced while validating a code-based runtime registration graph. */
enum class RegistryAssemblyIssueCode {
    NO_TOOLS,
    DUPLICATE_TOOL,
    DUPLICATE_EXECUTOR,
    MISSING_EXECUTOR,
    EXECUTOR_WITHOUT_TOOL,
    EXECUTOR_REGISTRATION_MISSING,
    EXECUTOR_TOOL_MISMATCH,
    NO_IMPLEMENTATIONS,
    DUPLICATE_IMPLEMENTATION,
    IMPLEMENTATION_TOOL_MISMATCH,
    DEFAULT_IMPLEMENTATION_MISSING,
    DEFAULT_IMPLEMENTATION_NOT_REGISTERED,
}

/** One deterministic registration-graph validation issue. */
data class RegistryAssemblyIssue(
    val code: RegistryAssemblyIssueCode,
    val toolCode: ToolCode? = null,
    val implementationCode: ImplementationCode? = null,
)

/** Counts and explicit defaults for a validated runtime registration graph. */
data class RegistryAssemblySnapshot(
    val toolCount: Int,
    val executorCount: Int,
    val implementationCount: Int,
    val defaultImplementations: Map<ToolCode, ImplementationCode>,
)

/** Result used both for startup validation and runtime readiness reporting. */
data class RegistryAssemblyValidation(
    val snapshot: RegistryAssemblySnapshot,
    val issues: List<RegistryAssemblyIssue>,
) {
    val valid: Boolean
        get() = issues.isEmpty()
}

/** Thrown when the standard runtime assembly receives an incomplete or contradictory graph. */
class RegistryAssemblyException(
    val issues: List<RegistryAssemblyIssue>,
) : IllegalStateException(
    issues.joinToString(
        prefix = "Invalid registry assembly: ",
        separator = "; ",
    ) { issue ->
        buildString {
            append(issue.code.name)
            issue.toolCode?.let { append(" tool=").append(it.value) }
            issue.implementationCode?.let { append(" implementation=").append(it.value) }
        }
    },
)

/** Validates descriptor, executor, implementation, and explicit-default consistency. */
object RegistryAssemblyValidator {
    fun inspect(
        tools: Iterable<ToolDescriptor>,
        executors: Iterable<ToolExecutor>,
    ): RegistryAssemblyValidation {
        val toolList = tools.toList()
        val executorList = executors.toList()
        val issues = mutableListOf<RegistryAssemblyIssue>()

        if (toolList.isEmpty()) {
            issues += RegistryAssemblyIssue(RegistryAssemblyIssueCode.NO_TOOLS)
        }

        toolList.groupBy { it.code }
            .filterValues { it.size > 1 }
            .keys
            .sortedBy { it.value }
            .forEach { issues += RegistryAssemblyIssue(RegistryAssemblyIssueCode.DUPLICATE_TOOL, it) }

        executorList.groupBy { it.toolCode }
            .filterValues { it.size > 1 }
            .keys
            .sortedBy { it.value }
            .forEach { issues += RegistryAssemblyIssue(RegistryAssemblyIssueCode.DUPLICATE_EXECUTOR, it) }

        val toolCodes = toolList.map { it.code }.toSet()
        val executorCodes = executorList.map { it.toolCode }.toSet()

        (toolCodes - executorCodes).sortedBy { it.value }.forEach {
            issues += RegistryAssemblyIssue(RegistryAssemblyIssueCode.MISSING_EXECUTOR, it)
        }
        (executorCodes - toolCodes).sortedBy { it.value }.forEach {
            issues += RegistryAssemblyIssue(RegistryAssemblyIssueCode.EXECUTOR_WITHOUT_TOOL, it)
        }

        val defaults = linkedMapOf<ToolCode, ImplementationCode>()
        var implementationCount = 0

        executorList.sortedBy { it.toolCode.value }.forEach { executor ->
            val registration = executor.registration
            if (registration == null) {
                issues += RegistryAssemblyIssue(
                    RegistryAssemblyIssueCode.EXECUTOR_REGISTRATION_MISSING,
                    executor.toolCode,
                )
                return@forEach
            }
            if (registration.toolCode != executor.toolCode) {
                issues += RegistryAssemblyIssue(
                    RegistryAssemblyIssueCode.EXECUTOR_TOOL_MISMATCH,
                    executor.toolCode,
                )
            }

            val implementations = registration.implementations
            implementationCount += implementations.size
            if (implementations.isEmpty()) {
                issues += RegistryAssemblyIssue(RegistryAssemblyIssueCode.NO_IMPLEMENTATIONS, executor.toolCode)
            }

            implementations.groupBy { it.implementationCode }
                .filterValues { it.size > 1 }
                .keys
                .sortedBy { it.value }
                .forEach {
                    issues += RegistryAssemblyIssue(
                        RegistryAssemblyIssueCode.DUPLICATE_IMPLEMENTATION,
                        executor.toolCode,
                        it,
                    )
                }

            implementations.filter { it.toolCode != executor.toolCode }.forEach {
                issues += RegistryAssemblyIssue(
                    RegistryAssemblyIssueCode.IMPLEMENTATION_TOOL_MISMATCH,
                    executor.toolCode,
                    it.implementationCode,
                )
            }

            val defaultImplementation = registration.defaultImplementation
            if (defaultImplementation == null) {
                issues += RegistryAssemblyIssue(
                    RegistryAssemblyIssueCode.DEFAULT_IMPLEMENTATION_MISSING,
                    executor.toolCode,
                )
            } else if (implementations.none { it.implementationCode == defaultImplementation }) {
                issues += RegistryAssemblyIssue(
                    RegistryAssemblyIssueCode.DEFAULT_IMPLEMENTATION_NOT_REGISTERED,
                    executor.toolCode,
                    defaultImplementation,
                )
            } else {
                defaults[executor.toolCode] = defaultImplementation
            }
        }

        return RegistryAssemblyValidation(
            snapshot = RegistryAssemblySnapshot(
                toolCount = toolList.map { it.code }.distinct().size,
                executorCount = executorList.map { it.toolCode }.distinct().size,
                implementationCount = implementationCount,
                defaultImplementations = defaults.toMap(),
            ),
            issues = issues.distinct(),
        )
    }

    fun requireValid(
        tools: Iterable<ToolDescriptor>,
        executors: Iterable<ToolExecutor>,
    ): RegistryAssemblySnapshot {
        val validation = inspect(tools, executors)
        if (!validation.valid) {
            throw RegistryAssemblyException(validation.issues)
        }
        return validation.snapshot
    }
}
