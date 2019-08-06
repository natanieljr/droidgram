package org.droidmate.droidgram.grammar

import com.google.gson.GsonBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.LinkedList

class Grammar @JvmOverloads constructor(
    initialGrammar: Map<Production, Set<Production>> =
        mapOf(
            Production.epsilon to setOf(Production.empty),
            Production.start to setOf(Production.epsilon)
        )
) {
    companion object {
        @JvmStatic
        private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }
    }

    operator fun get(key: Production): Set<Production> {
        return grammar[key].orEmpty()
    }

    operator fun get(key: Symbol): Set<Production> {
        return grammar.get(key)
    }

    val extractedGrammar: Map<Production, Set<Production>>
        get() = grammar.toMap()

    private val grammar: MutableMap<Production, MutableSet<Production>> by lazy {
        initialGrammar
            .map { Pair(it.key, it.value.toMutableSet()) }
            .toMap()
            .toMutableMap()
    }

    private fun remove(symbol: Symbol) {
        grammar.remove(Production(symbol))
    }

    fun key(production: Production): Set<Production> {
        val keys = mutableSetOf<Production>()

        for (symbol in production.values) {
            val key = Production(symbol)

            if (symbol.isNonTerminal()) {
                check(grammar.keys.any { it == key })
            }

            keys.add(key)
        }

        return keys
    }

    private fun Map<Production, Set<Production>>.containsKey(symbol: Symbol): Boolean {
        val key = Production(symbol)
        return this.keys.any { it == key }
    }

    fun Map<Production, Set<Production>>.get(key: Symbol): Set<Production> {
        val symbol = Production(key)
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

        val unusedKeys = grammar.keys
            .filterNot { key ->
                grammar.values.any { value ->
                    value.any { key.values.first() in it.nonTerminals }
                }
            }
            .filterNot { it.isStart() }

        unusedKeys.forEach { entry ->
            grammar.remove(entry)
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
            .map { it.value.map { p -> p.key.values.first() } }

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

    fun addRule(name: String, item: Symbol, coverage: Set<Long>) {
        addRule(name, item.value, coverage)
    }

    fun addRule(name: Symbol, item: String, coverage: Set<Long>) {
        addRule(name.value, item, coverage)
    }

    fun addRule(name: String, item: String, coverage: Set<Long>) {
        addRule(name, arrayOf(item), coverage)
    }

    fun addRule(name: String, item: Array<String>, coverage: Set<Long>) {
        val key = Production(name)
        val coverageSymbol = coverage
            .map { Symbol(it.toString()) }
            .toSet()

        val value = Production(item
            .filter { it.isNotEmpty() }
            .toTypedArray(),
            coverageSymbol
        )

        val emptySet: MutableSet<Production> = if (key.isAction()) {
            mutableSetOf()
        } else {
            mutableSetOf(Production.epsilon)
        }

        if (!grammar.containsKey(key.values.first())) {
            grammar[key] = emptySet
        }

        val existingKey = grammar.keys.first { it == key }

        val values = grammar[existingKey].orEmpty()

        // There's a bug in Kotlin
        // If we do value in values, it returns false due to different overrides of equals method
        // If we do value in values.map { it }, it returns true (correctly)
        if (value in values.map { it }) {
            // If the value was already there, need to merge the coverages
            val oldValue = values.first { it == value }
            val mergedCoverage = oldValue.coverage + value.coverage
            val newValue = Production(value.values, mergedCoverage)

            val newValues = values
                .filterNot { it == value }
                .toMutableSet()
                .also { it.add(newValue) }

            grammar.replace(existingKey, values.toMutableSet(), newValues)
        } else {
            grammar[existingKey]?.add(value)
        }
    }

    private fun grammarMap(useCoverage: Boolean): Map<String, Set<String>> {
        return grammar
            .entries
            .sortedBy { it.key }
            .map { entry ->
                val key = entry.key.values.first().value
                val value = entry.value.map { it.asString(useCoverage) }.toSortedSet()

                Pair(key, value)
            }.toMap()
    }

    fun asJsonStr(useCoverage: Boolean): String {
        val gSon = GsonBuilder().setPrettyPrinting().create()
        val entries = grammarMap(useCoverage)
        return gSon.toJson(entries)
    }

    fun asString(useCoverage: Boolean): String {
        return grammarMap(useCoverage)
            .map { entry ->
                val key = entry.key
                val value = entry.value
                "'${("$key'").padEnd(20)} : \t\t[${value.joinToString(", ") { "'$it'" } }],"
            }.joinToString("\n")
    }

    private fun reachableNonTerminals(): Set<Symbol> {
        val reachable = mutableSetOf<Symbol>()

        val queue = LinkedList<Symbol>()
        queue.add(Symbol.start)

        while (queue.isNotEmpty()) {
            val symbol = queue.pop()

            if (symbol !in reachable) {
                val newSymbols = grammar.get(symbol)
                    .flatMap { value ->
                        val nonTerminals = value.nonTerminals
                            .filterNot { reachable.contains(it) }

                        nonTerminals
                    }

                queue.addAll(newSymbols)
                reachable.add(symbol)
            }
        }

        return reachable
    }

    private fun unreachableNonTerminals(): Set<Symbol> {
        val reachableNonTerminals = reachableNonTerminals()

        return grammar.keys
            .map { it.values.first() }
            .filterNot { it in reachableNonTerminals }
            .toSet()
    }

    fun definedNonTerminals(): Set<Symbol> =
        grammar.keys
            .flatMap { it.values }
            .toSet()

    fun definedTerminals(): Set<Symbol> =
        grammar.values
            .flatMap { it.flatMap { production -> production.terminals } }
            .filter { it.isTerminal() }
            .toSet()

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
        // All keys should be a single non terminal
        val multiSymbolKeys = grammar.keys.filterNot { it.values.size == 1 }
        if (multiSymbolKeys.isNotEmpty()) {
            multiSymbolKeys.forEach {
                log.error("Key $it contains more than 1 symbol")
            }
            return false
        }

        val terminalKeys = grammar.keys
            .filter { it.terminals.isNotEmpty() }
        if (terminalKeys.isNotEmpty()) {
            terminalKeys.forEach {
                log.error("Key $it contains 1 or more terminal symbols")
            }
            return false
        }

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

    fun getRoot(): Symbol {
        return Symbol.start
    }
}
