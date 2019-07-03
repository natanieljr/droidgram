package org.droidmate.droidgram.grammar

import com.google.gson.GsonBuilder

class Grammar @JvmOverloads constructor(
    private val startSymbol: String = "<start>",
    private val emptySymbol: String = "<empty>",
    // val nonTerminalRegex: Regex = defaultNonTerminalRegex,
    initialGrammar: Map<String, Set<String>> =
        mapOf(
            emptySymbol to setOf(""),
            startSymbol to setOf(emptySymbol)
        )
) {
    operator fun get(key: String): Set<String>? {
        return grammar[key]
    }

    private val grammar: MutableMap<String, MutableSet<String>> by lazy {
        initialGrammar
            .map { Pair(it.key, it.value.toMutableSet()) }
            .toMap()
            .toMutableMap()
    }

    fun removeTerminateActions() {
        val terminateActions = grammar.keys
            .filter { it.contains("Terminate") }

        terminateActions.forEach { terminateProduction ->
            grammar.replaceAll { _, value ->
                value.map { it.replace(terminateProduction, "<empty>") }.toMutableSet()
            }
            grammar.remove(terminateProduction)
        }
    }

    private fun removeEmptyStateTransitions() {
        val singleState = grammar.entries
            .filter { it.key.contains("(") }
            .filter { it.value.isEmpty() }

        singleState.forEach { entry ->
            val oldValue = entry.key
            val newValue = "<empty>"

            grammar.replaceAll { _, v ->
                v.map { it.replace(oldValue, newValue) }.toMutableSet()
            }
            grammar.remove(oldValue)
        }

    }

    fun removeSingleStateTransitions() {
        val singleState = grammar.entries
            .filter { it.key.contains("(") }
            .filter { it.value.size == 1 && it.value.any { p -> p != "<empty>" } }

        singleState.forEach { entry ->
            val oldValue = entry.key
            val newValue = entry.value.first { it != "<empty>" }

            grammar.replaceAll { _, v ->
                v.map { it.replace(oldValue, newValue) }.toMutableSet()
            }
            grammar.remove(oldValue)
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
                    p.nonTerminals().map { q ->
                        Pair(it.key, q)
                    }
                }
            }
            .filter { !grammar.containsKey(it.second) }

        nonExistingState.forEach { entry ->
            val key = entry.first
            val illegalState = entry.second

            val newValue = grammar.getValue(key)
                .map { it.replace(illegalState, "") }
                .filter { it.isNotEmpty() }
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
            .map { it.value.map { p -> p.key } }

        duplicates.forEach { keys ->
            val target = keys.first()

            keys.drop(1).forEach { oldKey ->
                grammar.replaceAll { _, v ->
                    v.map { it.replace(oldKey, target) }.toMutableSet()
                }
                grammar.remove(oldKey)
            }
        }
    }

    fun addRule(name: String, item: String) {
        val emptySet = if (name.contains("(")) {
            mutableSetOf()
        } else {
            mutableSetOf(emptySymbol)
        }

        grammar.getOrPut(name) { emptySet }
            .add(item)
    }

    private fun grammarMap(): Map<String, Set<String>> {
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
        val gson = GsonBuilder().setPrettyPrinting().create()
        val entries = grammarMap()
        return gson.toJson(entries)
    }

    fun asString(): String {
        return grammarMap()
            .map { entry ->
                val key = entry.key
                val value = entry.value
                "'${("$key'").padEnd(20)} : \t\t[${value.joinToString(", ") { "'$it'" } }],"
            }.joinToString("\n")
    }

    // private fun String.nonTerminals() = nonTerminals(nonTerminalRegex)

    private fun reachableNonTerminals(
        symbol: String = startSymbol,
        reachable: MutableSet<String> = mutableSetOf()
    ): Set<String> {
        reachable.add(symbol)
        grammar[symbol]?.flatMap { value ->
            val nonTerminals = value.nonTerminals()

            nonTerminals
                .filterNot { reachable.contains(it) }
                .flatMap { nonTerminal ->
                reachableNonTerminals(nonTerminal, reachable)
            }
        }.orEmpty()

        return reachable
    }

    private fun unreachableNonTerminals(): Set<String> =
        grammar.keys - reachableNonTerminals(startSymbol)

    private fun definedNonTerminals(): Set<String> = grammar.keys

    private fun usedNonTerminals(): Set<String> {
        return grammar.keys.flatMap { definedNonTerminal ->
            val expansions = grammar[definedNonTerminal]

            if (expansions.isNullOrEmpty()) {
                println("Non-terminal $definedNonTerminal: expansion list empty")
            }

            expansions.orEmpty().flatMap { expansion -> (expansion.nonTerminals()) }
        }.toSet()
    }

    fun isValid(): Boolean {

        val definedNonTerminals = definedNonTerminals()
        val usedNonTerminals = usedNonTerminals().toMutableSet()

        // It must have terms and all terms must have a value
        if (definedNonTerminals.isEmpty() || usedNonTerminals.isEmpty())
            return false

        // Do not complain about '<start>' being not used, even if [startSymbol] is different
        usedNonTerminals.add(startSymbol)

        val unusedNonTerminals = definedNonTerminals
            .filterNot { usedNonTerminals.contains(it) }

        if (unusedNonTerminals.isNotEmpty()) {
            unusedNonTerminals.forEach { println("$it defined but not used") }

            return false
        }

        val undefinedNonTerminals = usedNonTerminals
            .filterNot { definedNonTerminals.contains(it) }

        if (undefinedNonTerminals.isNotEmpty()) {
            undefinedNonTerminals.forEach { println("$it used but not defined") }

            return false
        }

        // Symbols must be reachable either from <start> or given start symbol
        val unreachable = unreachableNonTerminals()

        if (unreachable.isNotEmpty()) {
            unreachable.forEach { println("$it is unreachable from $startSymbol") }

            return false
        }

        return true
    }
}
