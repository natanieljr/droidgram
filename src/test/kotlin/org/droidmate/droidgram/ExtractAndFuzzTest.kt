package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.CodeTerminalGuidedFuzzer
import org.droidmate.droidgram.fuzzer.TerminalGuidedFuzzer
import org.droidmate.droidgram.fuzzer.toCoverageGrammar
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
        return GrammarExtractor.extract(args, true)
    }

    @Test
    fun extractGrammarTest() {
        val grammar = extractGrammar()

        assertTrue(grammar.isValid(), "Grammar is not valid")
    }

    @Test
    fun fuzzExtractedGrammar() {
        val grammar = extractGrammar()
        val generator = TerminalGuidedFuzzer(Grammar(initialGrammar = grammar.asMap()), printLog = false)

        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(
            guidedFuzz(
                generator,
                "[]"
            )
        )

        inputList.add(
            guidedFuzz(
                generator,
                "[ClickEvent(w01), LongClickEvent(w13), ClickEvent(w13), ClickEvent(w54), ClickEvent(w55), LongClickEvent(w59), LongClickEvent(w60), ClickEvent(w57), ClickEvent(w11), ClickEvent(w06), ClickEvent(w05), ClickEvent(w09), ClickEvent(w08), LongClickEvent(w08), LongClickEvent(w05), LongClickEvent(w11), LongClickEvent(w06), ClickEvent(w07), LongClickEvent(w00), LongClickEvent(w07), LongClickEvent(w10), ClickEvent(w00), LongClickEvent(w09), ClickEvent(w10), ClickEvent(w01), ClickEvent(w12), ClickEvent(w02), PressBack(s02), ClickEvent(w04), TextInsert(w03,apezsdzspmqcxjt), TextInsert(w03,wkrxnfmqg), ]"
            )
        )

        inputList.add(
            guidedFuzz(
                generator,
                "[ClickEvent(w01), ClickEvent(w14), ClickEvent(w27), ClickEvent(w14), ClickEvent(w25), ClickEvent(w19), ClickEvent(w34), ClickEvent(w29), ClickEvent(w32), ClickEvent(w30), ClickEvent(w33), ClickEvent(w35), ClickEvent(w36), ClickEvent(w37), Click(w21), ClickEvent(w39), TextInsert(w40,qdxymtaroanfdlrh), ClickEvent(w20), ClickEvent(w28), TextInsert(w40,siwesli), ClickEvent(w16), ClickEvent(w42), ClickEvent(w23), Tick(w24), Click(w24), ClickEvent(w22), Tick(w23), ClickEvent(w26), TextInsert(w18,lixxdwxhjctsalrmgb), TextInsert(w18,jcmxghpteqrgfnzdjsj), PressBack(s05), ClickEvent(w16), ClickEvent(w15), TextInsert(w18,xzbvoua), TextInsert(w18,qjlbqorrepnhuagxqy), PressBack(s04), ClickEvent(w01), ClickEvent(w44), ]"
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
    fun fuzzExtractedCodeGrammarTerminalFuzzer() {
        val grammar = extractGrammar().asMap().toCoverageGrammar()
        val generator = TerminalGuidedFuzzer(Grammar(initialGrammar = grammar), printLog = false)

        val inputList = mutableListOf<List<Symbol>>()

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }

    @Test
    fun fuzzExtractedCodeGrammarCodeFuzzer() {
        val grammar = extractGrammar().asMap().toCoverageGrammar()
        val generator = CodeTerminalGuidedFuzzer(Grammar(initialGrammar = grammar), printLog = false)

        val inputList = mutableListOf<List<Symbol>>()

        while (generator.nonCoveredSymbols.isNotEmpty()) {
            inputList.add(guidedFuzz(generator, ""))
        }

        inputList.forEach { input ->
            println(input)
        }
    }
}