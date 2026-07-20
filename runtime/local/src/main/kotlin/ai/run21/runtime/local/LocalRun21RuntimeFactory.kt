package ai.run21.runtime.local

import ai.run21.core.CoreToolRuntime
import ai.run21.tools.formula.FormulaToolWiring

/**
 * Creates local in-memory runtime instances.
 */
object LocalRun21RuntimeFactory {
    /**
     * Creates a runtime with the formula evaluation tool registered.
     */
    fun formulaOnly(): LocalRun21Runtime {
        return LocalRun21Runtime(
            CoreToolRuntime.inMemory(FormulaToolWiring.registration()),
        )
    }
}
