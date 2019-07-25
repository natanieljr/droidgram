package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.TerminalCoverageGuidedFuzzer
import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Symbol
import org.droidmate.droidgram.mining.GrammarExtractor
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertTrue

@Ignore
class ExtractAndFuzzTest {
    private fun extractGrammar(): Grammar {
        val args = arrayOf("./input/apks2/droidMate/model/", "./input/apks2")
        return GrammarExtractor.extract(args)
    }

    @Test
    fun extractGrammarTest() {
        val grammar = extractGrammar()

        assertTrue(grammar.isValid(), "Grammar is not valid")
    }

    @Test
    fun fuzzExtractedGrammar() {
        val grammar = extractGrammar()
        val generator = TerminalCoverageGuidedFuzzer(Grammar(initialGrammar = grammar.extractedGrammar), printLog = true)

        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(
            guidedFuzz(
                generator,
                "[]"
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
}