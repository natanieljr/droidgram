package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.RandomFuzzer
import org.droidmate.droidgram.grammar.Grammar
import org.junit.Test
import kotlin.test.assertEquals

class GrammarFuzzerTest {
    @Test
    fun randomFuzz() {
        val generator = RandomFuzzer(Grammar(initialGrammar = grammarTitle))
        val result = generator.fuzz()

        assertEquals(
            "[The Joy of , Fuzzing, :, Principles, Techniques and Tools,  for, Fun, and, Fun]",
            result.toString()
        )
        println("Fuzzed input: $result")
        assert(true)
    }
}