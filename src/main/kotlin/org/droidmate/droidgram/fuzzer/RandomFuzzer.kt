package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import kotlin.random.Random

class RandomFuzzer(
    grammar: Grammar,
    random: Random = Random(0),
    printLog: Boolean = false
) : BaseGrammarFuzzer(grammar, random, printLog) {
    override fun chooseNodeExpansion(nodes: List<Node>): Pair<Node, Production> {
        val node = nodes.random(random)
        val children = grammar[node.value].orEmpty()
        val child = children.random(random)

        return Pair(node, child)
    }

    override fun onExpanded(node: Node, newNodes: List<Node>) {
        // nothing to do in this class
    }
}