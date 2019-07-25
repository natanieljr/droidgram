package org.droidmate.droidgram.fuzzer

import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.Symbol

fun Production.newTerminals(coveredTerminals: Set<Symbol>): Set<Symbol> {
    return this.terminals
        .filterNot { it in coveredTerminals }
        .toSet()
}
