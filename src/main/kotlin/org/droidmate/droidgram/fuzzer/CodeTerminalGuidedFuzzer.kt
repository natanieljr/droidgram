package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Symbol
import java.util.LinkedList
import kotlin.random.Random

class CodeTerminalGuidedFuzzer(
    grammar: Grammar,
    random: Random = Random(0),
    printLog: Boolean = false
) : TerminalGuidedFuzzer(grammar, random, printLog) {

    private fun allNonTerminals(): List<Symbol> {
        val stack = LinkedList<Node>()
        val nonTerminals = mutableListOf<Symbol>()

        // Add first node.
        stack.push(root)

        // Use stack to create breadth first traversal.
        while (stack.isNotEmpty()) {
            val currentNode = stack.pop()

            if (currentNode.isNonTerminal()) {
                nonTerminals.add(currentNode.value)
            }

            for (child in currentNode.children.reversed()) {
                stack.push(child)
            }
        }

        return nonTerminals
    }

    override fun getInput(): List<Symbol> {
        return allNonTerminals()
    }
}