package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.RandomFuzzer
import org.droidmate.droidgram.fuzzer.TerminalCoverageGuidedFuzzer
import org.droidmate.droidgram.grammar.Grammar
import org.junit.Test
import kotlin.test.assertEquals

class GrammarFuzzerTest {
    @Test
    fun randomFuzz() {
        val generator = RandomFuzzer(Grammar(initialGrammar = grammarTitle))
        val result = generator.fuzz()
        println("Fuzzed input: $result")

        assertEquals(
            "[The Joy of , Fuzzing, :, Principles, Techniques and Tools,  for, Fun, and, Fun]",
            result.toString()
        )
    }

    @Test
    fun terminalGuidedFuzz() {
        val generator = TerminalCoverageGuidedFuzzer(Grammar(initialGrammar = grammarTitle))
        val result = generator.fuzz()
        println("Fuzzed input: $result")

        assertEquals(
            "[Generating Software Tests, :, Breaking Software,  for, Fun, and, Profit]",
            result.toString()
        )
    }

    @Test
    fun terminalGuidedFuzzURL() {
        val generator = TerminalCoverageGuidedFuzzer(Grammar(initialGrammar = grammarURL))
        val result = generator.fuzz()
        println("Fuzzed input: $result")

        assertEquals(
            "[https, ://, user:password, @, fuzzingbook.com, :, 80, /, ?, x, 6, 4, =, abc, &, def, &, def, =, def]",
            result.toString()
        )
    }
}