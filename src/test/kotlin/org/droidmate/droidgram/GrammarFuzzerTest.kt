package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.GrammarFuzzer
import org.droidmate.droidgram.grammar.Grammar
import org.junit.Test

class GrammarFuzzerTest {
    private val generator
        get() = GrammarFuzzer(Grammar(initialGrammar = grammarTitle))

    @Test
    fun getChildren() {
        val fuzz = generator
        fuzz.fuzz()
        print(fuzz.allTerminals())
        assert(true)
    }
}