package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.GrammarFuzzer
import org.droidmate.droidgram.fuzzer.Node
import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import kotlin.random.Random

class RandomFuzzer(grammar: Grammar, private val random: Random = Random(0)) : GrammarFuzzer(grammar) {
    override fun chooseChild(children: Collection<Production>): Production {
        return children.random(random)
    }

    override fun chooseNodeExpansion(nodes: List<Node>): Node {
        return nodes.random(random)
    }

    override fun onExpanded(node: Node, newNodes: List<Node>) {
        // nothing to do in this class
    }
}