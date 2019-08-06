package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol
import kotlin.random.Random

class SymbolGuidedFuzzer(
    grammar: Grammar,
    random: Random = Random(0),
    printLog: Boolean = false,
    private val symbol: Symbol
) : CodeTerminalGuidedFuzzer(grammar, random, printLog) {
    override fun Production.uncoveredSymbols(): Set<Symbol> {
        return this.terminals
            .filter { it == symbol }
            .toSet()
    }

    override val nonCoveredSymbols
        get() = grammar.definedTerminals()
            .filter { it == symbol }
            .filterNot { it in coveredSymbols }
            .toSet()
}