package org.droidmate.droidgram.grammar

import com.google.gson.GsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Grammar @JvmOverloads constructor(
    initialGrammar: Map<SingleValueProduction, Set<Production>> =
        mapOf(
            SingleValueProduction.epsilon to setOf(SingleValueProduction.empty),
            SingleValueProduction.start to setOf(SingleValueProduction.epsilon)
        )
) {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }

    operator fun get(key: Production): Set<Production>? {
        return grammar[key]
    }

    private val grammar: MutableMap<SingleValueProduction, MutableSet<Production>> by lazy {
        initialGrammar
            .map { Pair(it.key, it.value.toMutableSet()) }
            .toMap()
            .toMutableMap()
    }

    private fun remove(symbol: Symbol) {
        grammar.remove(Production(symbol))
    }

    private fun Map<SingleValueProduction, Set<Production>>.containsKey(symbol: Symbol): Boolean {
        val key = SingleValueProduction(symbol)
        return this.keys.any { it == key }
    }

    fun Map<SingleValueProduction, Set<Production>>.get(key: Symbol): Set<Production>? {
        val symbol = SingleValueProduction(key)
        return this.entries
            .first { it.key == symbol }
            .value
    }

    fun removeTerminateActions() {
        val terminateActions = grammar.keys
            .filter { it.isTerminate() }
            .map {
                check(it.nonTerminals.size == 1) {
                    "Can only remove a production with a single non terminal"
                }

                it.nonTerminals.first()
            }

        terminateActions.forEach { terminateProduction ->
            grammar.replaceAll { _, value ->
                value.map {
                    it.replaceByEpsilon(terminateProduction)
                }.toMutableSet()
            }

            remove(terminateProduction)
        }
    }

    private fun removeEmptyStateTransitions() {
        val singleState = grammar.entries
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
            grammar.replaceAll { _, v ->
                v.map { it.replaceByEpsilon(oldValue) }.toMutableSet()
            }

            remove(oldValue)
        }
    }

    fun removeSingleStateTransitions() {
        val singleState = grammar.entries
            .filterNot { it.key.isState() }
            .filterNot { it.key.isEpsilon() || it.key.isStart() }
            .filter { it.value.size == 1 && it.value.any { p -> !p.isEpsilon() } }

        singleState.forEach { entry ->
            val oldValue = entry.key.nonTerminals.first()
            val newValue = entry.value.first { !it.isEpsilon() }.values.first()

            grammar.replaceAll { _, v ->
                v.map {
                    it.replace(oldValue, newValue)
                }.toMutableSet()
            }
            remove(oldValue)
        }
    }

    fun removeUnusedSymbols() {
        val originalGrammarSize = grammar.size

        val unusedEntries = grammar.entries
            .filterNot { x ->
                grammar.any { y ->
                    y.value.any { z ->
                        z.contains(x.key.symbol)
                    }
                }
            }
            .filterNot { it.key.isStart() }

        unusedEntries.forEach { entry ->
            grammar.remove(entry.key)
        }

        if (grammar.size != originalGrammarSize) {
            this.removeUnusedSymbols()
        }
    }

    /**
     * When an action points to a state an the next action is a reset.
     * The transition state is not created
     */
    fun removeNonExistingStates() {
        val nonExistingState = grammar.entries
            .flatMap {
                it.value.flatMap { p ->
                    p.nonTerminals.map { q ->
                        Pair(it.key, q)
                    }
                }
            }
            .filter { !grammar.containsKey(it.second) }

        nonExistingState.forEach { entry ->
            val key = entry.first
            val illegalState = entry.second

            val newValue = grammar.getValue(key)
                .map { it.replace(illegalState, Symbol.empty) }
                .filter { it.hasValue() }
                .toMutableSet()
            grammar.replace(key, newValue)
        }

        // After removing states, some resulting states may be empty
        removeEmptyStateTransitions()
    }

    fun mergeEquivalentTransitions() {
        val duplicates = grammar.entries
            .groupBy { it.value }
            .filter { it.value.size > 1 }
            .map { it.value.map { p -> p.key.symbol } }

        duplicates.forEach { keys ->
            val target = keys.first()

            keys.drop(1).forEach { oldKey ->
                grammar.replaceAll { _, v ->
                    v.map { it.replace(oldKey, target) }.toMutableSet()
                }
                remove(oldKey)
            }
        }
    }

    fun addRule(name: String, item: String) {
        addRule(name, arrayOf(item))
    }

    fun addRule(name: String, item: Array<String>) {
        val key = SingleValueProduction(name)
        val value = Production(item
            .filter { it.isNotEmpty() }
            .toTypedArray()
        )

        val emptySet: MutableSet<Production> = if (key.isAction()) {
            mutableSetOf()
        } else {
            mutableSetOf(SingleValueProduction.epsilon)
        }

        if (!grammar.containsKey(key.symbol)) {
            grammar[key] = emptySet
        }

        val existingKey = grammar.keys.first{ it == key }
        grammar[existingKey]?.add(value)
    }

    private fun grammarMap(): Map<SingleValueProduction, Set<Production>> {
        return grammar
            .entries
            .sortedBy { it.key }
            .map { entry ->
                val key = entry.key
                val value = entry.value.toSortedSet()

                Pair(key, value)
            }.toMap()
    }

    fun asJsonStr(): String {
        val gSon = GsonBuilder().setPrettyPrinting().create()
        val entries = grammarMap()
        return gSon.toJson(entries)
    }

    fun asString(): String {
        return grammarMap()
            .map { entry ->
                val key = entry.key
                val value = entry.value
                "'${("$key'").padEnd(20)} : \t\t[${value.joinToString(", ") { "'$it'" } }],"
            }.joinToString("\n")
    }

    private fun reachableNonTerminals(
        symbol: Symbol,
        reachable: MutableSet<Symbol> = mutableSetOf()
    ): Set<Symbol> {
        reachable.add(symbol)
        grammar.get(symbol)?.flatMap { value ->
            val nonTerminals = value.nonTerminals

            nonTerminals
                .filterNot { reachable.contains(it) }
                .flatMap { nonTerminal ->
                reachableNonTerminals(nonTerminal, reachable)
            }
        }.orEmpty()

        return reachable
    }

    private fun unreachableNonTerminals(): Set<Symbol> =
        grammar.keys
            .map { it.symbol }
            .filterNot { it in reachableNonTerminals(Symbol.start) }
            .toSet()

    private fun definedNonTerminals(): Set<Symbol> =
        grammar.keys.map { it.symbol }.toSet()

    private fun usedNonTerminals(): Set<Symbol> {
        return grammar.keys.flatMap { definedNonTerminal ->
            val expansions = grammar[definedNonTerminal]

            if (expansions.isNullOrEmpty()) {
                log.error("Non-terminal $definedNonTerminal: expansion list empty")
            }

            expansions.orEmpty().flatMap { expansion -> (expansion.nonTerminals) }
        }.toSet()
    }

    fun isValid(): Boolean {
        val definedNonTerminals = definedNonTerminals()
        val usedNonTerminals = usedNonTerminals().toMutableSet()

        // It must have terms and all terms must have a value
        if (definedNonTerminals.isEmpty() || usedNonTerminals.isEmpty())
            return false

        // Do not complain about '<start>' being not used, even if [startSymbol] is different
        usedNonTerminals.add(Symbol.start)

        val unusedNonTerminals = definedNonTerminals
            .filterNot { usedNonTerminals.contains(it) }

        if (unusedNonTerminals.isNotEmpty()) {
            unusedNonTerminals.forEach {
                log.error("$it defined but not used")
            }

            return false
        }

        val undefinedNonTerminals = usedNonTerminals
            .filterNot { definedNonTerminals.contains(it) }

        if (undefinedNonTerminals.isNotEmpty()) {
            undefinedNonTerminals.forEach {
                log.error("$it used but not defined")
            }

            return false
        }

        // Symbols must be reachable either from <start> or given start symbol
        val unreachable = unreachableNonTerminals()

        if (unreachable.isNotEmpty()) {
            unreachable.forEach {
                log.error("$it is unreachable from ${Symbol.start}")
            }

            return false
        }

        return true
    }
}
