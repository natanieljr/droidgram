package org.droidmate.droidgram.grammar

class SingleValueProduction(val symbol: Symbol) : Production(symbol) {
    companion object {
        @JvmStatic
        val epsilon = SingleValueProduction(Symbol.epsilon)
        @JvmStatic
        val start = SingleValueProduction(Symbol.start)
        @JvmStatic
        val empty = SingleValueProduction(Symbol.empty)
    }

    constructor(symbol: String) : this(Symbol(symbol))

    val value: String
        get() = symbol.value

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is SingleValueProduction -> values == other.values
            is Production -> values == other.values
            else -> false
        }
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }
}