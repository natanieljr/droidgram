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
        return other is SingleValueProduction &&
                other.values == this.values
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }
}