package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol

fun Map<Production, Set<Production>>.toCoverageGrammar(symbol: Symbol? = null): Map<Production, Set<Production>> {
    val newGrammar: MutableMap<Production, MutableSet<Production>> = mutableMapOf()

    this.forEach { (key, productions) ->
        newGrammar[key] = mutableSetOf()
        productions.forEach { production ->
            val newProduction = if (production.isEmpty()) {
                Production.empty
            } else {
                val coverage = if (symbol != null) {
                    production.coverage
                        .filter { it == symbol }
                        .toSet()
                } else {
                    production.coverage
                }
                Production(
                    (coverage + production.nonTerminals).toList(),
                    emptySet()
                )
            }
            newGrammar[key]?.add(newProduction)
        }
    }

    val newGrammarCheck = Grammar(initialGrammar = newGrammar)
    check(newGrammarCheck.isValid()) {
        "Generated coverage grammar is invalid"
    }

    if (symbol != null) {
        check(newGrammarCheck.reachableTerminals().contains(symbol)) {
            "Target symbol not found in the grammar"
        }
    }

    return newGrammar
}