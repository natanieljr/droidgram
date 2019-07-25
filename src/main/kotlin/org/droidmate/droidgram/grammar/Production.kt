package org.droidmate.droidgram.grammar

open class Production(
    val values: List<Symbol>,
    val coverage: Set<Symbol> = emptySet()
) : Comparable<Production> {
    companion object {
        @JvmStatic
        val epsilon = Production(Symbol.epsilon)
        @JvmStatic
        val start = Production(Symbol.start)
        @JvmStatic
        val empty = Production(Symbol.empty)
    }

    constructor(value: Symbol, coverage: Set<Symbol> = emptySet()) : this(listOf(value), coverage)

    constructor(value: String, coverage: Set<Symbol> = emptySet()) : this(listOf(Symbol(value)), coverage)

    constructor(values: Array<String>, coverage: Set<Symbol> = emptySet()) : this(values.map { Symbol(it) }, coverage)

    val terminals by lazy {
        values.filter { it.isTerminal() }
    }

    val nonTerminals by lazy {
        values.filterNot { it.isTerminal() }
    }

    fun isTerminal(): Boolean {
        return values.all { it.isTerminal() }
    }

    fun asString(useCoverage: Boolean): String {
        return if (useCoverage) {
            coverage.joinToString(" ") { p -> p.value } +
                    nonTerminals.joinToString("") { p -> p.value }
        } else {
            values.joinToString("") { p -> p.value }
        }
    }

    fun replace(condition: (Symbol) -> Boolean, newSymbol: Symbol): Production {
        val newValues = values.map { symbol ->
            if (condition(symbol)) {
                newSymbol
            } else {
                symbol
            }
        }

        return Production(newValues, coverage)
    }

    fun replace(oldSymbol: Symbol, newSymbol: Symbol): Production {
        return replace({ symbol -> symbol == oldSymbol }, newSymbol)
    }

    fun replaceByEpsilon(oldSymbol: Symbol): Production {
        return replace(oldSymbol, Symbol.epsilon)
    }

    fun isStart(): Boolean {
        return values.any { it == Symbol.start }
    }

    fun isEpsilon(): Boolean {
        return values.all { it == Symbol.epsilon }
    }

    fun isEmpty(): Boolean {
        return values.all { it == Symbol.empty }
    }

    fun hasValue(): Boolean {
        return values.isNotEmpty()
    }

    fun contains(symbol: Symbol): Boolean {
        return this.values.contains(symbol)
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Production -> values == other.values
            else -> false
        }
    }

    override fun toString(): String {
        return values.joinToString("")
    }

    override fun compareTo(other: Production): Int {
        return values.toString().compareTo(other.values.toString())
    }

    override fun hashCode(): Int {
        var result = values.hashCode()
        result = 31 * result + coverage.hashCode()
        return result
    }
}