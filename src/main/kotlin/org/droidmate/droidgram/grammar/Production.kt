package org.droidmate.droidgram.grammar

open class Production(val values: List<Symbol>) : Comparable<Production> {
    constructor(value: Symbol) : this(listOf(value))

    constructor(value: String) : this(listOf(Symbol(value)))

    constructor(values: Array<String>) : this(values.map { Symbol(it) })

    // val values: MutableList<Symbol> = originalValues.toMutableList()

    val terminals by lazy {
        values.filter { it.isTerminal() }
    }

    val nonTerminals by lazy {
        values.filterNot { it.isTerminal() }
    }

    fun replace(condition: (Symbol) -> Boolean, newSymbol: Symbol): Production {
        val newValues = values.map { symbol ->
            if (condition(symbol)) {
                newSymbol
            } else {
                symbol
            }
        }

        return Production(newValues)
    }

    fun replace(oldSymbol: Symbol, newSymbol: Symbol): Production {
        return replace({ symbol -> symbol == oldSymbol }, newSymbol)
    }

    fun replaceByEpsilon(oldSymbol: Symbol): Production {
        return replace(oldSymbol, Symbol.epsilon)
    }

    /*
    fun replaceByEpsilon(condition: (Symbol) -> Boolean): Production {
        return replace(condition, Symbol.epsilon)
    }
    */

    fun isStart(): Boolean {
        return values.any { it == Symbol.start }
    }

    fun isEpsilon(): Boolean {
        return values.all { it == Symbol.epsilon }
    }

    fun hasValue(): Boolean {
        return values.isNotEmpty()
    }

    fun contains(symbol: Symbol): Boolean {
        return this.values.contains(symbol)
    }

    override fun equals(other: Any?): Boolean {
        return other is Production &&
                other.values == this.values
    }

    override fun toString(): String {
        return values.joinToString("")
    }

    override fun compareTo(other: Production): Int {
        return values.toString().compareTo(other.values.toString())
    }
}