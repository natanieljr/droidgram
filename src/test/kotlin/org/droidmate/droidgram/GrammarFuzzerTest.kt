package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.RandomFuzzer
import org.droidmate.droidgram.fuzzer.TerminalGuidedFuzzer
import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Symbol
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
        val generator = TerminalGuidedFuzzer(Grammar(initialGrammar = grammarTitle))
        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(
            guidedFuzz(
                generator,
                "[Generating Software Tests, :, Breaking Software,  for, Fun, and, Profit]"
            )
        )

        inputList.add(guidedFuzz(generator, "[, Fuzzing, :, Principles, Techniques and Tools]"))

        inputList.add(
            guidedFuzz(
                generator,
                "[The Art of , Fuzzing, :, Tools and Techniques for , Breaking Software]"
            )
        )

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }

    @Test
    fun terminalGuidedFuzzURL() {
        val generator = TerminalGuidedFuzzer(Grammar(initialGrammar = grammarURL))
        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(guidedFuzz(generator, "[http, ://, cispa.saarland, , ?, abc, =, def]"))

        inputList.add(
            guidedFuzz(
                generator,
                "[https, ://, www.google.com, /, ?, x, 0, 1, =, x, 2, 3, &, x, 4, 5, =, x, 6, 7]"
            )
        )

        inputList.add(guidedFuzz(generator, "[ftp, ://, fuzzingbook.com, /<id>, ?, x, 8, 9, =, x, 7, 8]"))

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }
}