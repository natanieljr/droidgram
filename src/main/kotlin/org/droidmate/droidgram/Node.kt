package org.droidmate.droidgram

data class Node(val symbol: String, val children: List<String>? = null) {
    override fun equals(other: Any?): Boolean {
        return other is Node && other.symbol == this.symbol
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }
}