package org.droidmate.droidgram.mining

import org.droidmate.droidgram.grammar.Grammar
import org.droidmate.droidgram.grammar.Production
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path

class GrammarMerger(private val outputDir: Path, private val printToConsole: Boolean) {
    fun merge(grammars: Collection<Grammar>): Grammar {
        val mergedProductions: MutableMap<Production, MutableSet<Production>> = mutableMapOf()

        grammars.map { it.asMap() }
            .forEach { grammar ->
                grammar.forEach { (rule, expansions) ->
                    mergedProductions.mergeProduction(rule, expansions)
                }
        }

        val mergedGrammar = Grammar(initialGrammar = mergedProductions)

        writeGrammar(mergedGrammar)
        writeStatistics(mergedGrammar, grammars)

        return mergedGrammar
    }

    private fun MutableMap<Production, MutableSet<Production>>.mergeProduction(
        rule: Production,
        expansions: Set<Production>
    ) {
        if (rule in this) {
            this.mergeExpansions(rule, expansions)
        } else {
            this[rule] = expansions.toMutableSet()
        }
    }

    private fun MutableMap<Production, MutableSet<Production>>.mergeExpansions(
        rule: Production,
        expansions: Set<Production>
    ) {
        val expansionSet = this[rule].orEmpty()

        expansions.forEach { expansion ->
            if (expansion !in expansionSet) {
                this[rule]?.add(expansion)
            } else {
                this.mergeCoverages(rule, expansion)
            }
        }
    }

    private fun MutableMap<Production, MutableSet<Production>>.mergeCoverages(
        rule: Production,
        expansion: Production
    ) {
        val currentExpansionSet = this[rule].orEmpty()
        val currentExpansion = currentExpansionSet.first { it == expansion }
        val newCoverage = expansion.coverage

        val currentCoverage = currentExpansion.coverage.toMutableSet()
        currentCoverage.addAll(newCoverage)

        val newProduction = Production(currentExpansion.values, currentCoverage)
        val newExpansionSet = currentExpansionSet
            .filterNot { it == currentExpansion }
            .toMutableSet()
            .also {
                it.add(newProduction)
            }

        this.replace(rule, newExpansionSet)
    }

    private fun writeGrammar(grammar: Grammar) {
        val grammarJSON = grammar.asJsonStr(false)
        val grammarFile = outputDir.resolve("grammarMerged.txt")
        Files.write(grammarFile, grammarJSON.toByteArray())

        if (printToConsole) {
            println("Grammar:")
            val grammarStr = grammar.asString(false)
            print(grammarStr)
        }
    }

    private fun getStatistics(grammar: Map<Production, Set<Production>>): String {
        return StringBuilder()
            .append("Production rules ")
            .append(grammar.size)
            .appendln()
            .append("Terminals ")
            .append(grammar.flatMap { it.value.flatMap { p -> p.terminals } }.distinct().size)
            .appendln()
            .append("Non Terminals ")
            .append(grammar.flatMap { it.value.flatMap { p -> p.nonTerminals } }.distinct().size)
            .appendln()
            .append("Largest production rule ")
            .append(grammar.map { it.value.size }.max())
            .appendln()
            .toString()
    }

    private fun writeStatistics(mergedGrammar: Grammar, grammars: Collection<Grammar>) {
        val sb = StringBuilder()

        sb.appendln("Original grammars:")

        grammars.map { it.asMap() }.forEachIndexed { index, grammar ->
            sb.appendln("Grammar $index")
                .append(getStatistics(grammar))
                .appendln()
        }

        sb.appendln()
            .appendln("Merged grammar:")
            .append(getStatistics(mergedGrammar.asMap()))

        println(sb.toString())
    }
}