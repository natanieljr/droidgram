package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import java.util.LinkedList
import kotlin.random.Random

abstract class GrammarFuzzer(
    protected val grammar: Grammar,
    protected val random: Random,
    protected val printLog: Boolean
) {
    init {
        check(grammar.isValid()) { "The grammar is not valid." }
    }

    private val nodeList: MutableList<Node> = mutableListOf()

    protected lateinit var root: Node

    protected fun debug(msg: String) {
        if (printLog) {
            println(msg)
        }
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

    protected open fun expandNode(node: Node, child: Production): List<Node> {
        val result = mutableListOf<Node>()

        for (symbol in child.values) {
            val newNode = node.addChild(symbol)

            nodeList.add(newNode)
            result.add(newNode)
        }

        return result
    }

    protected open fun expandOnce(nonExpandedNodes: List<Node>) {
        val nodeAndChild = chooseNodeExpansion(nonExpandedNodes)
        val node = nodeAndChild.first
        val child = nodeAndChild.second

        val newNodes = expandNode(node, child)

        debug("Expanding $node into $newNodes")

        onExpanded(node, newNodes)
    }

    open fun fuzz(): List<Symbol> {
        root = Node(grammar.getRoot())
        nodeList.clear()

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

    protected abstract fun chooseNodeExpansion(nodes: List<Node>): Pair<Node, Production>
    protected abstract fun onExpanded(node: Node, newNodes: List<Node>)
}