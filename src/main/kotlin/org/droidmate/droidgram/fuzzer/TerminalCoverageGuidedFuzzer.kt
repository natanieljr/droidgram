package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import java.util.LinkedList
import kotlin.random.Random

data class SearchData(
    val node: Node,
    val baseExpansion: Production,
    val currentExpansion: Production,
    val currentDepth: Int
)

class TerminalCoverageGuidedFuzzer(
    grammar: Grammar,
    random: Random = Random(0),
    printLog: Boolean = false
) : GrammarFuzzer(grammar, random, printLog) {

    private val coveredSymbols: MutableSet<Symbol> = mutableSetOf()
    val nonCoveredSymbols
        get() = grammar.definedTerminals()
            .filterNot { it in coveredSymbols }
            .toSet()

    private fun Production.getCoverage(): Set<Symbol> {
        return this.newTerminals(coveredSymbols)
            .toMutableSet()
    }

    private fun Production.getExpansionCoverage(): Map<Production, Set<Symbol>> {
        val coverage = if (this.isTerminal()) {
            mapOf(Pair(this, this.getCoverage()))
        } else {
            grammar[this]
                .map { Pair(it, it.getCoverage()) }
                .toMap()
        }

        debug("Coverage of production $this: $coverage")
        return coverage
    }

    /**
     * Returns a production which contains <EMPTY> (Epsilon) or a random one.
     * Used when no expansion rule provided new coverage
     */
    private fun List<Node>.epsilonOrRandom(): Pair<Node, Production> {
        val productionsWithEpsilon = this.mapNotNull { production ->
            val epsilon = grammar[production.value]
                .firstOrNull { it.isEpsilon() }

            if (epsilon != null) {
                Pair(production, epsilon)
            } else {
                null
            }
        }

        return if (productionsWithEpsilon.isNotEmpty()) {
            productionsWithEpsilon.random(random)
        } else {
            val randomProduction = this.random(random)
            val randomExpansion = grammar[randomProduction.value]
                .random(random)

            Pair(randomProduction, randomExpansion)
        }
    }

    override fun onExpanded(node: Node, newNodes: List<Node>) {
        val terminals = newNodes
            .filter { it.isTerminal() }
            .map { it.value }

        coveredSymbols.addAll(terminals)
    }

    override fun chooseNodeExpansion(nodes: List<Node>): Pair<Node, Production> {
        val initialDepth = nodes.first().depth
        val maxGrammarDepth = grammar.definedNonTerminals().size
        val maxDepth = maxGrammarDepth - initialDepth

        val queue = LinkedList<SearchData>()

        queue.addAll(
            nodes
            .flatMap {
                grammar[it.value]
                    .map { expansion ->
                        grammar.key(expansion).map { expansionKey ->
                            SearchData(it, expansion, expansionKey, it.depth)
                        }
                    }
                    .flatten()
            }
        )

        // Only 1 possible expansion, skip everything
        if (queue.size == 1) {
            val singleElement = queue.single()
            return Pair(singleElement.node, singleElement.baseExpansion)
        }

        var lastDepth = initialDepth
        val currentDepthMap = mutableMapOf<SearchData, Map<Production, Set<Symbol>>>()

        while (queue.isNotEmpty()) {
            val searchData = queue.pop()

            if (lastDepth > maxDepth) {
                break
                // While in the same depth, calculate and add to list
            } else if (searchData.currentDepth == lastDepth) {
                val possibleExpansions = searchData.currentExpansion.getExpansionCoverage()
                currentDepthMap[searchData] = possibleExpansions

                val newSearch = possibleExpansions.map {
                    grammar.key(it.key).map { expansionKey ->
                        SearchData(
                            searchData.node,
                            searchData.baseExpansion,
                            expansionKey,
                            searchData.currentDepth + 1
                        )
                    }
                }.flatten()

                queue.addAll(newSearch)
            // Changed depth, check if any production leads to new coverage
            } else {
                // If has new coverage, stop and select from those
                if (currentDepthMap.any { it.value.any { p -> p.value.isNotEmpty() } }) {
                    break
                // Otherwise clear caches and add current item
                } else {
                    currentDepthMap.clear()
                    val possibleExpansions = searchData.currentExpansion.getExpansionCoverage()
                    currentDepthMap[searchData] = possibleExpansions
                }
                lastDepth = searchData.currentDepth
            }
        }

        // If any element has new coverage, take the best
        return if (currentDepthMap.any { it.value.any { p -> p.value.isNotEmpty() } }) {
            val maxPerEntry = currentDepthMap.entries
                .map { entry ->
                    Pair(
                        entry.key,
                        entry.value
                            .maxBy { it.value.size }
                            ?: throw IllegalStateException("This should never happen")
                    )
                }

            val bestResult = maxPerEntry
                .maxBy { it.second.value.size }
                ?.first ?: throw IllegalStateException("This should never happen")

            debug("Best production: $bestResult")
            Pair(bestResult.node, bestResult.baseExpansion)
        // Otherwise look for an epsilon
        } else {
            nodes.epsilonOrRandom()
        }
    }
}