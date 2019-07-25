package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production

fun Map<Production, Set<Production>>.toCoverageGrammar(): Map<Production, Set<Production>> {
    val newGrammar: MutableMap<Production, MutableSet<Production>> = mutableMapOf()

    this.forEach { (key, productions) ->
        newGrammar[key] = mutableSetOf()
        productions.forEach { production ->
            val newProduction = if (production.isEmpty()) {
                Production.empty
            } else {
                Production(
                    (production.coverage + production.nonTerminals).toList(),
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

    return newGrammar
}