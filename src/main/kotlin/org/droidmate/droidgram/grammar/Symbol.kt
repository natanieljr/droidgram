package org.droidmate.droidgram.grammar

data class Symbol(val value: String) : Comparable<Symbol> {
    override fun compareTo(other: Symbol): Int {
        return value.compareTo(other.value)
    }

    companion object {
        private const val startValue = "<start>"

        @JvmStatic
        val empty = Symbol("")
        @JvmStatic
        val epsilon = Symbol("<empty>")
        @JvmStatic
        val start = Symbol(startValue)
    }

    fun isNonTerminal(): Boolean {
        return value.startsWith("<")
    }

    fun isTerminal(): Boolean {
        return !isNonTerminal()
    }

    fun contains(str: String): Boolean {
        return value.contains(str)
    }

    override fun toString(): String {
        return value
    }

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is Symbol -> value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}