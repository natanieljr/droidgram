package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import kotlin.random.Random

class TerminalCoverageGuidedFuzzer(
    grammar: Grammar,
    private val random: Random = Random(0)
) : GrammarFuzzer(grammar) {
    private val coveredSymbols: MutableSet<Symbol> = mutableSetOf()

    /**
     * Gets the best available expansion for the current symbol
     */
    private fun Symbol.getBestExpansion(): Pair<Production, Set<Symbol>> {
        val possibleExpansions = grammar[this].orEmpty()
        val productionCoverage = possibleExpansions.map { expansion ->
            Pair(
                expansion,
                expansion.newTerminals(coveredSymbols)
            )
        }

        val maxCoverage = productionCoverage
            .map { it.second.size }
            .max()

        return productionCoverage.filter { it.second.size == maxCoverage }
            .random(random)
    }

    /**
     * Returns a production which contains <EMPTY> (Epsilon) or a random one.
     * Used when no expansion rule provided new coverage
     */
    private fun List<Symbol>.epsilonOrRandom(): Pair<Symbol, Production> {
        val productionsWithEpsilon = this.map { production ->
            val epsilon = grammar[production]
                .orEmpty()
                .first { it.isEpsilon() }

            Pair(production, epsilon)
        }

        return if (productionsWithEpsilon.isNotEmpty()) {
            productionsWithEpsilon.random(random)
        } else {
            val randomProduction = this.random(random)
            val randomExpansion = grammar[randomProduction]
                .orEmpty()
                .random(random)

            Pair(randomProduction, randomExpansion)
        }
    }

    /**
     * Returns the symbol and expansion which have the highest new coverage
     */
    private fun Map<Symbol, Pair<Production, Set<Symbol>>>.best(): Pair<Symbol, Production> {
        val maxCoverage = this.values.map { it.second.size }.max()

        val bestExpansions = this
            .filter { it.value.second.size == maxCoverage }

        val randomNode = bestExpansions.entries.random(random)

        return Pair(randomNode.key, randomNode.value.first)
    }

    private fun getBestProduction(symbols: List<Symbol>, maxDepth: Int): Pair<Symbol, Production> {
        if (maxDepth <= 0) {
            return symbols.epsilonOrRandom()
        }

        val symbolCoverage = symbols.map { symbol ->
            Pair(symbol, symbol.getBestExpansion())
        }.toMap()

        if (symbolCoverage.size == 1) {
            return symbolCoverage.entries
                .map { Pair(it.key, it.value.first) }
                .first()
        } else if (symbolCoverage.any { it.value.second.isNotEmpty() }) {
            return symbolCoverage.best()
        }

        val grandChildren = symbols
            .map {
                Pair(
                    it,
                    grammar[it]
                        .orEmpty()
                        .filter { p -> p.nonTerminals.isNotEmpty() }
                        .map { p -> p.nonTerminals.first() }
                )
            }.toMap()

        return grandChildren.entries
            .map { entry ->
                val symbol = entry.key
                val possibleExpansion = entry.value

                val bestExpansion = getBestProduction(possibleExpansion, maxDepth - 1)

                Pair(symbol, bestExpansion.second)
            }
            .maxBy {
                it.second.terminals
                    .filter { p -> p !in coveredSymbols }.size
            } ?: throw IllegalStateException("Should not have happened")
    }

    override fun chooseNodeExpansion(nodes: List<Node>): Pair<Node, Production> {
        val currDepth = nodes.first().depth
        val maxGrammarDepth = grammar.definedNonTerminals().size
        val maxDepth = maxGrammarDepth - currDepth

        val bestSymbol = getBestProduction(
            nodes.map { it.value },
            maxDepth
        )

        return Pair(
            nodes.first { it.value == bestSymbol.first },
            bestSymbol.second
        )
    }

    override fun onExpanded(node: Node, newNodes: List<Node>) {
        val terminals = newNodes
            .filter { it.isTerminal() }
            .map { it.value }

        coveredSymbols.addAll(terminals)
    }
}