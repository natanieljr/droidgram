package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import java.util.LinkedList
import kotlin.random.Random

abstract class CoverageGuidedFuzzer(
    grammar: Grammar,
    random: Random = Random(0),
    printLog: Boolean = false
) : BaseGrammarFuzzer(grammar, random, printLog) {
    protected val coveredSymbols: MutableSet<Symbol> = mutableSetOf()

    abstract val nonCoveredSymbols: Set<Symbol>

    private fun Production.getCoverage(): Set<Symbol> {
        return this.uncoveredSymbols()
            .toMutableSet()
    }

    private fun Production.getExpansionCoverage(): Map<Production, Set<Symbol>> {
        return if (this.isTerminal()) {
            mapOf(Pair(this, this.getCoverage()))
        } else {
            grammar[this]
                .map { Pair(it, it.getCoverage()) }
                .toMap()
        }
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

        debug("Possible expansions: ${queue.joinToString { it.baseExpansion.toString() } }")

        // Only 1 possible expansion, skip everything
        if (queue.size == 1) {
            val singleElement = queue.single()
            return Pair(singleElement.node, singleElement.baseExpansion)
        }

        var lastDepth = initialDepth
        val currentDepthMap = mutableMapOf<SearchData, Map<Production, Set<Symbol>>>()

        val seenElements = queue.map { it.baseExpansion }.distinct().toMutableSet()

        while (queue.isNotEmpty()) {
            val searchData = queue.pop()

            if (lastDepth > maxDepth) {
                break
                // While in the same depth, calculate and add to list
            } else if (searchData.currentDepth != lastDepth &&
                currentDepthMap.any { it.value.any { p -> p.value.isNotEmpty() } }) {
                break
            } else {
                if (searchData.currentDepth != lastDepth) {
                    debug("Seeking new productions with depth $lastDepth")
                }

                val possibleExpansions = searchData.currentExpansion.getExpansionCoverage()
                currentDepthMap[searchData] = possibleExpansions

                val newSearch = possibleExpansions
                    .map {
                        grammar.key(it.key)
                            .map { expansionKey ->
                                SearchData(
                                    searchData.node,
                                    searchData.baseExpansion,
                                    expansionKey,
                                    searchData.currentDepth + 1
                                )
                            }
                    }.flatten()

                val filteredSearch = newSearch.filterNot {
                    it.currentExpansion in seenElements }
                seenElements.addAll(filteredSearch.map {
                    it.currentExpansion }
                )

                queue.addAll(filteredSearch)
                // Changed depth, check if any production leads to new coverage
            }
        }

        // If any element has new coverage, take the best
        return if (currentDepthMap.any { it.value.any { p -> p.value.isNotEmpty() } }) {
            val maxPerEntry = currentDepthMap.entries
                .map { entry ->
                    Pair(
                        entry.key,
                        entry.value.map { it.value.size }
                            .shuffled(random)
                            .max()
                            ?: throw IllegalStateException("This should never happen")
                    )
                }

            val bestResult = maxPerEntry
                .shuffled(random)
                .maxBy { it.second }
                ?.first ?: throw IllegalStateException("This should never happen")

            Pair(bestResult.node, bestResult.baseExpansion)
            // Otherwise look for an epsilon
        } else {
            nodes.epsilonOrRandom()
        }
    }

    abstract fun Production.uncoveredSymbols(): Set<Symbol>
}
