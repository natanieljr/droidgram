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
        val generator = TerminalCoverageGuidedFuzzer(Grammar(initialGrammar = grammar.extractedGrammar), printLog = false)

        val inputList = mutableListOf<List<Symbol>>()

        inputList.add(
            guidedFuzz(
                generator,
                "[]"
            )
        )

        inputList.add(guidedFuzz(generator, "[ClickEvent(w05), ClickEvent(w07), LongClickEvent(w00), ClickEvent(w08), ClickEvent(w11), LongClickEvent(w09), ClickEvent(w10), LongClickEvent(w06), ClickEvent(w06), LongClickEvent(w08), LongClickEvent(w11), LongClickEvent(w05), ClickEvent(w00), LongClickEvent(w07), ClickEvent(w09), LongClickEvent(w10), ClickEvent(w01), ClickEvent(w02), TextInsert(w03,wkrxnfmqg), ClickEvent(w04), TextInsert(w03,apezsdzspmqcxjt), PressBack(s02), ]"))

        inputList.add(
            guidedFuzz(
                generator,
                "[ClickEvent(w01), ClickEvent(w13), ClickEvent(w54), ClickEvent(w56), ClickEvent(w55), ClickEvent(w60), LongClickEvent(w59), ClickEvent(w58), LongClickEvent(w55), ClickEvent(w59), LongClickEvent(w56), LongClickEvent(w60), LongClickEvent(w58), ClickEvent(w57), ClickEvent(w01), LongClickEvent(w13), ClickEvent(w12), ClickEvent(w14), ClickEvent(w39), TextInsert(w40,grnrryqjbbdsgmome), ClickEvent(w20), TextInsert(w40,zumetf), ClickEvent(w30), ClickEvent(w16), ClickEvent(w15), TextInsert(w18,qjlbqorrepnhuagxqy), PressBack(s04), TextInsert(w18,xzbvoua), ClickEvent(w16), ClickEvent(w17), TextInsert(w18,jcmxghpteqrgfnzdjsj), TextInsert(w18,lixxdwxhjctsalrmgb), PressBack(s05), ClickEvent(w16), ClickEvent(w27), ClickEvent(w49), PressBack(s26), TextInsert(w50,kuhwruwvtdajqopxhac), ClickEvent(w16), ClickEvent(w48), ClickEvent(w51), ClickEvent(w52), ClickEvent(w53), ClickEvent(w47), ClickEvent(w02), PressBack(s28), ClickEvent(w51), ClickEvent(w38), TextInsert(w18,wfzizedvmxsf), PressBack(s29), ]"
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