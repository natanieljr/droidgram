package org.droidmate.droidgram.grammar

class Grammar @JvmOverloads constructor(
    val startSymbol: String = "<start>",
    private val emptySymbol: String = "<empty>",
    // val nonTerminalRegex: Regex = defaultNonTerminalRegex,
    initialGrammar: Map<String, Set<String>> =
        mapOf(
            emptySymbol to setOf(""),
            startSymbol to setOf(emptySymbol)
        )
) {
    companion object {
        @JvmStatic
        fun from(grammar: Map<String, Set<String>>) = Grammar(initialGrammar = grammar)
    }

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

    fun mergeEquivalentTransitions() {
        val duplicates = grammar.entries
            .groupBy { it.value }
            .filter { it.value.size > 1 }
            .map { it.value.map { it.key } }

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
        val emptySet = if (name.contains(".")) {
            mutableSetOf(emptySymbol)
        } else {
            mutableSetOf()
        }

        grammar.getOrPut(name) { emptySet }
            .add(item)
    }

    fun print() {
        grammar
            .entries
            .sortedBy { it.key }
            .forEach { entry ->
                val key = entry.key
                val value = entry.value.toSortedSet()
                println("'${("$key'").padEnd(20)} : \t\t[${value.joinToString(", ") { "'$it'" } }],")
            }
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
