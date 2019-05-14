/*package org.droidmate.droidgram

import kotlin.random.Random

class GrammarFuzzer(
    private val grammar: Grammar,
    private val minNonTerminals: Int = 0,
    private val maxNonTerminals: Int = 10,
    randomSeed: Int = 0
) {
    init {
        assert(grammar.isValid())
    }

    private val random = Random(randomSeed)

    private val tree = mutableMapOf<String, MutableList<String>?>()

    fun initTree() {
        tree.clear()
        tree[grammar.startSymbol] = null
    }

    fun testGetChildren(expansion: String) = expansion.expansionToChildren()

    /**
     * Converting [this] into an expansion contains all substrings -- both terminals and
     * non-terminals such that ''.join(strings) == expansion
     */
    private fun String.expansionToChildren(): Map<String, List<String>?> {
        // Special case: epsilon expansion
        return if (this.isEmpty()) {
            mapOf(this to emptyList())
        } else {
            val strings = this.splitByNonTerminals()

            strings.map {
                if (!it.isNonTerminal()) {
                    this to emptyList<String>()
                } else {
                    it to null
                }
            }.toMap()
        }
    }

    /**
     * Return index of expansion in `possible_children` to be selected.  Defaults to random.
     */
    private fun chooseNodeExpansion(node: Map<String, List<String>?>, possibleChildren: List<String>): Int {
        return random.nextInt(possibleChildren.size)
    }

    private fun expandNodeRandomly(node: Map<String, List<String>?>): Map<String, List<String>?> {
        assert(node.children == null) { "Node was already expanded" }

        // Fetch the possible expansions from grammar...
        val expansions = grammar[node.symbol] ?: throw IllegalStateException("Node ${node.symbol} not found in grammar")
        val possibleChildren = expansions.map { it.expansionToChildren() }

        // ... and select a random expansion
        val index = chooseNodeExpansion(node, possibleChildren)
        chosen_children = possible_children[index]

        # Process children (for subclasses)
        chosen_children = self.process_chosen_children(chosen_children,
            expansions[index])

        # Return with new children
                return (symbol, chosen_children)
    }

    /*

    def expand_node_randomly(self, symbol):
        (symbol, children) = symbol
        assert children is None

        if self.log:
            print("Expanding", all_terminals(symbol), "randomly")

        # Fetch the possible expansions from grammar...
        expansions = self.grammar[symbol]
        possible_children = [self.expansion_to_children(
            expansion) for expansion in expansions]

        # ... and select a random expansion
        index = self.choose_node_expansion(symbol, possible_children)
        chosen_children = possible_children[index]

        # Process children (for subclasses)
        chosen_children = self.process_chosen_children(chosen_children,
                                                       expansions[index])

        # Return with new children
        return (symbol, chosen_children)

     */
}*/
