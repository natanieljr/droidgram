package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import java.util.LinkedList

abstract class GrammarFuzzer(private val grammar: Grammar) {
    init {
        check(grammar.isValid()) { "The grammar is not valid." }
    }

    private val nodeList: MutableList<Node> = mutableListOf()

    private val root by lazy {
        val root = grammar.getRoot()

        Node(root)
    }

    private fun allTerminals(): List<Symbol> {
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

    protected open fun expandNode(node: Node): List<Node> {
        val children = grammar[node.value].orEmpty()
        val chosenChild = chooseChild(children)

        val result = mutableListOf<Node>()

        for (symbol in chosenChild.values) {
            val node = node.addChild(symbol)
            nodeList.add(node)

            result.add(node)
        }

        return result
    }

    protected open fun expandOnce(nonExpandedNodes: List<Node>) {
        val node = chooseNodeExpansion(nonExpandedNodes)

        val newNodes = expandNode(node)
        println("Expanding $node into $newNodes")

        onExpanded(node, newNodes)
    }

    open fun fuzz(): List<Symbol> {
        var nonExpandedNodes = nonExpandedNodes()

        while (nonExpandedNodes.isNotEmpty()) {
            expandOnce(nonExpandedNodes)

            nonExpandedNodes = nonExpandedNodes()
        }

        return getInput()
    }

    protected open fun getInput(): List<Symbol> {
        return allTerminals()
    }

    protected abstract fun chooseChild(children: Collection<Production>): Production
    protected abstract fun chooseNodeExpansion(nodes: List<Node>): Node
    protected abstract fun onExpanded(node: Node, newNodes: List<Node>)
}