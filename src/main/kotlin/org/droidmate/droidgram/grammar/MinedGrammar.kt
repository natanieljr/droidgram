package org.droidmate.droidgram.grammar

class MinedGrammar @JvmOverloads constructor(
    initialGrammar: Map<Production, Set<Production>> =
        mapOf(
                 Production.epsilon to setOf(Production.empty),
                 Production.start to setOf(Production.epsilon)
             )
) : Grammar(initialGrammar) {
    /**
     * Clean up includes: removing duplicate productions, removing terminate state
     */
    fun cleanUp(mergeEquivalent: Boolean) {
        removeNonExistingStates()
        removeTerminateActions()

        if (mergeEquivalent) {
            removeSingleStateTransitions()
            mergeEquivalentTransitions()
        }

        removeUnusedSymbols()
    }

    private fun removeTerminateActions() {
        val terminateActions = productionSet.keys
            .filter { it.isTerminate() }
            .map {
                check(it.nonTerminals.size == 1) {
                    "Can only remove a production with a single non terminal"
                }

                it.nonTerminals.first()
            }

        terminateActions.forEach { terminateProduction ->
            productionSet.replaceAll { _, value ->
                value.map {
                    it.replaceByEpsilon(terminateProduction)
                }.toMutableSet()
            }

            remove(terminateProduction)
        }
    }

    private fun removeEmptyStateTransitions() {
        val singleState = productionSet.entries
            .filter { it.key.isAction() }
            .filter { it.value.isEmpty() }
            .map { it.key }
            .map {
                check(it.nonTerminals.size == 1) {
                    "Can only remove a production with a single non terminal"
                }

                it.nonTerminals.first()
            }

        singleState.forEach { oldValue ->
            productionSet.replaceAll { _, v ->
                v.map { it.replaceByEpsilon(oldValue) }.toMutableSet()
            }

            remove(oldValue)
        }
    }

    private fun removeSingleStateTransitions() {
        val singleState = productionSet.entries
            .filterNot { it.key.isState() }
            .filterNot { it.key.isEpsilon() || it.key.isStart() }
            .filter { it.value.size == 1 && it.value.any { p -> !p.isEpsilon() } }

        singleState.forEach { entry ->
            val oldValue = entry.key.nonTerminals.first()
            val newValue = entry.value.first { !it.isEpsilon() }.values.first()

            productionSet.replaceAll { _, v ->
                v.map {
                    it.replace(oldValue, newValue)
                }.toMutableSet()
            }
            remove(oldValue)
        }
    }

    private fun removeUnusedSymbols() {
        val originalGrammarSize = productionSet.size

        val unusedKeys = productionSet.keys
            .filterNot { key ->
                productionSet.values.any { value ->
                    value.any { key.values.first() in it.nonTerminals }
                }
            }
            .filterNot { it.isStart() }

        unusedKeys.forEach { entry ->
            productionSet.remove(entry)
        }

        if (productionSet.size != originalGrammarSize) {
            this.removeUnusedSymbols()
        }
    }

    /**
     * When an action points to a state an the next action is a reset.
     * The transition state is not created
     */
    private fun removeNonExistingStates() {
        val nonExistingState = productionSet.entries
            .flatMap {
                it.value.flatMap { p ->
                    p.nonTerminals.map { q ->
                        Pair(it.key, q)
                    }
                }
            }
            .filter { !productionSet.containsKey(it.second) }

        nonExistingState.forEach { entry ->
            val key = entry.first
            val illegalState = entry.second

            val newValue = productionSet.getValue(key)
                .map { it.replace(illegalState, Symbol.empty) }
                .filter { it.hasValue() }
                .toMutableSet()
            productionSet.replace(key, newValue)
        }

        // After removing states, some resulting states may be empty
        removeEmptyStateTransitions()
    }

    private fun mergeEquivalentTransitions() {
        val duplicates = productionSet.entries
            .groupBy { it.value }
            .filter { it.value.size > 1 }
            .map { it.value.map { p -> p.key.values.first() } }

        duplicates.forEach { keys ->
            val target = keys.first()

            keys.drop(1).forEach { oldKey ->
                productionSet.replaceAll { _, v ->
                    v.map { it.replace(oldKey, target) }.toMutableSet()
                }
                remove(oldKey)
            }
        }
    }
}