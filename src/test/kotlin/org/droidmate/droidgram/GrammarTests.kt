package org.droidmate.droidgram

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import org.droidmate.droidgram.grammar.SingleValueProduction
import org.droidmate.droidgram.grammar.isNonTerminal
import org.droidmate.droidgram.grammar.nonTerminals
import org.junit.Test
import kotlin.test.assertEquals

class GrammarTests {
    @Test
    fun nonTerminalRegexTest() {
        assertEquals(setOf("<factor>"), "+<factor>".nonTerminals())
        assertEquals(setOf("<term>", "<factor>"), "<term> * <factor>".nonTerminals())
        assertEquals(setOf("<digit>", "<integer>"), "<digit><integer>".nonTerminals())
        assertEquals(emptySet(), "1 < 3 > 2".nonTerminals())
        assertEquals(setOf("<3>"), "1 <3> 2".nonTerminals())
        assertEquals(emptySet(), "1 + 2".nonTerminals())
        assertEquals(setOf("<1>"), "<1>".nonTerminals())
    }

    @Test
    fun isNonTerminalTest() {
        assert("<abc>".isNonTerminal())
        assert("<symbol-1>".isNonTerminal())
        assert(!"+".isNonTerminal())
    }

    @Test
    fun isValidGrammarTest() {
        assert(Grammar(initialGrammar = grammarExpr).isValid())
        assert(Grammar(initialGrammar = grammarCGI).isValid())
        assert(Grammar(initialGrammar = grammarURL).isValid())
        assert(Grammar(initialGrammar = grammarTitle).isValid())
    }

    @Test
    fun invalidGrammarTest() {
        assert(!Grammar(
            initialGrammar = mapOf(
                SingleValueProduction("<start>") to setOf(Production("<x>")),
                SingleValueProduction("<y>") to setOf(Production("1"))
            )
        ).isValid())

        assert(!Grammar(initialGrammar = mapOf(
            SingleValueProduction("<start>") to setOf(Production("123")))
        ).isValid())

        assert(!Grammar(initialGrammar = mapOf(
            SingleValueProduction("<start>") to emptySet())
        ).isValid())

        assert(!Grammar(initialGrammar = mapOf(
            SingleValueProduction("<start>") to setOf(Production("1", "2", "3")))
        ).isValid())
    }
}