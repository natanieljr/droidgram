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
                "[The Fuzzing Book, :, Generating Software Tests,  for, Security, and, Reliability]"
            )
        )

        inputList.add(guidedFuzz(generator, "[, Fuzzing, :, Breaking Software,  for, Fun, and, Profit]"))

        inputList.add(
            guidedFuzz(
                generator,
                "[The Joy of , Fuzzing, :, Tools and Techniques for , Principles, Techniques and Tools]"
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

        inputList.add(guidedFuzz(generator, "[https, ://, user:password, @, cispa.saarland, , ?, x, 1, 5, =, 4]"))

        inputList.add(
            guidedFuzz(
                generator,
                "[http, ://, fuzzingbook.com, /, ?, abc, =, def, &, x, 6, 3, =, x, 0, 7]"
            )
        )

        inputList.add(
            guidedFuzz(
                generator,
                "[ftps, ://, user:password, @, www.google.com, :, 80, /<id>, ?, x, 2, 8, =, x, 9, 7]"
            )
        )

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }
}