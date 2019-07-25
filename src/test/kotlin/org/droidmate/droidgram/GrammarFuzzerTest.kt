package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.RandomFuzzer
import org.droidmate.droidgram.fuzzer.TerminalCoverageGuidedFuzzer
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

    private fun guidedFuzz(generator: TerminalCoverageGuidedFuzzer, expected: String): List<Symbol> {
        val nonCoveredSymbolsBeforeRun = generator.nonCoveredSymbols
        val result = generator.fuzz()
        println("Fuzzed input: $result")

        if (expected.isNotEmpty()) {
            assertEquals(
                expected,
                result.toString()
            )
        }

        val newlyCovered = nonCoveredSymbolsBeforeRun - generator.nonCoveredSymbols
        println("Covered: $newlyCovered")
        println("Missing: ${generator.nonCoveredSymbols}")

        assert(generator.nonCoveredSymbols.isEmpty() || newlyCovered.isNotEmpty()) {
            "No new terminals were covered in this run. " +
                    "Original: $nonCoveredSymbolsBeforeRun. Actual: ${generator.nonCoveredSymbols}"
        }

        return result
    }

    @Test
    fun terminalGuidedFuzz() {
        val generator = TerminalCoverageGuidedFuzzer(Grammar(initialGrammar = grammarTitle))
        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(guidedFuzz(generator, "[Generating Software Tests, :, Breaking Software,  for, Fun, and, Profit]"))

        inputList.add(guidedFuzz(generator, "[, Fuzzing, :, Principles, Techniques and Tools]"))

        inputList.add(guidedFuzz(generator, "[The Art of , Fuzzing, :, Tools and Techniques for , Breaking Software]"))

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }

    @Test
    fun terminalGuidedFuzzURL() {
        val generator = TerminalCoverageGuidedFuzzer(Grammar(initialGrammar = grammarURL))
        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(guidedFuzz(generator, "[http, ://, cispa.saarland, , ?, abc, =, def]"))

        inputList.add(guidedFuzz(generator, "[https, ://, www.google.com, /, ?, x, 0, 1, =, x, 2, 3, &, x, 4, 5, =, x, 6, 7]"))

        inputList.add(guidedFuzz(generator, "[ftp, ://, fuzzingbook.com, /<id>, ?, x, 8, 9, =, x, 7, 8]"))

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }
}