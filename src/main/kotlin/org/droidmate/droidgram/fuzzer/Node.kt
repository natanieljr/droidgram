package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Symbol

class Node(
    var value: Symbol,
    val parent: Node? = null,
    val depth: Int = 0
) {
    var children: MutableList<Node> = mutableListOf()

    fun hasChildren() = children.isNotEmpty()
    
    fun addChild(item: Symbol): Node {
        val node = Node(item, this, depth + 1)
        children.add(node)

        return node
    }

    fun isTerminal() = value.isTerminal()

    fun isNonTerminal() = value.isNonTerminal()

    fun canExpand() = isNonTerminal() && !hasChildren()

    override fun toString(): String {
        return "$value ($depth)"
    }
}