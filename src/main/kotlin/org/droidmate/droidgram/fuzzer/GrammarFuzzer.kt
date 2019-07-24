package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import java.util.LinkedList
import kotlin.random.Random

open class GrammarFuzzer(private val grammar: Grammar) {
    init {
        check(grammar.isValid()) { "The grammar is not valid." }
    }

    private val nodeList: MutableList<Node> = mutableListOf()

    private val root by lazy {
        val root = grammar.getRoot()

        Node(root)
    }

    fun allTerminals(): List<Symbol> {
        val stack = LinkedList<Node>()
        val terminals = mutableListOf<Symbol>()

        // Add first node.
        stack.push(root)

        // Use stack to create breadth first traversal.
        while (stack.isNotEmpty()) {
            val currentNode = stack.pop()

            if (currentNode.isTerminal()) {
                terminals.add(currentNode.value)
            } else {
                for (child in currentNode.children.reversed()) {
                    stack.push(child)
                }
            }
        }

        return terminals
    }

    private fun nonExpandedNodes(): List<Node> {
        val queue = LinkedList<Node>()
        val nonExpanded = mutableListOf<Node>()

        // Add first node.
        queue.add(root)

        // Use queue to create breadth first traversal.
        while (queue.isNotEmpty()) {
            val currentNode = queue.poll()

            if (currentNode.canExpand()) {
                nonExpanded.add(currentNode)
            } else {
                val nonTerminalChildren = currentNode.children
                    .filter { it.isNonTerminal() }

                for (child in nonTerminalChildren) {
                    queue.add(child)
                }
            }
        }

        return nonExpanded
    }

    protected open fun Collection<Production>.chooseChild(): Production {
        return this.random(Random(0))
    }

    private fun Node.expand(): List<Node> {
        val chosenChild = grammar[this.value]
            .orEmpty()
            .chooseChild()

        val result = mutableListOf<Node>()

        for (symbol in chosenChild.values) {
            val node = this.addChild(symbol)
            nodeList.add(node)

            result.add(node)
        }

        return result
    }

    protected open fun List<Node>.chooseExpansion(): Node {
        return this.random(Random(0))
    }

    fun fuzz() {
        var nonExpandedNodes = nonExpandedNodes()

        while (nonExpandedNodes.isNotEmpty()) {
            val node = nonExpandedNodes.chooseExpansion()

            val newNodes = node.expand()
            println("Expanding $node into $newNodes")

            nonExpandedNodes = nonExpandedNodes()
        }
    }
}