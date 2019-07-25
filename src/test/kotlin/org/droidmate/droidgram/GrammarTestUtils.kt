package org.droidmate.droidgram

import org.droidmate.droidgram.fuzzer.TerminalGuidedFuzzer
import org.droidmate.droidgram.grammar.Symbol
import kotlin.system.measureTimeMillis
import kotlin.test.assertEquals

val defaultNonTerminalRegex = "(<[^<> ]*>)".toPattern().toRegex()

/**
 * In later chapters, we allow expansions to be tuples, with the expansion being the first element
 */
fun String.nonTerminals(): Set<String> {
    return defaultNonTerminalRegex.findAll(this)
        .map { it.value }
        .toSet()
}

fun String.splitByNonTerminals(): List<String> {
    val matcher = defaultNonTerminalRegex.toPattern().matcher(this)

    if (!matcher.find()) {
        return emptyList()
    }

    val result = mutableListOf<String>()
    var lastStart = matcher.end()
    do {
        if (matcher.start() > (lastStart + 1)) {
            result.add(this.substring(lastStart, matcher.start()))
        }

        result.add(this.substring(matcher.start(), matcher.end()))
        lastStart = matcher.end()
    } while (matcher.find())

    return result
}

fun String.isNonTerminal(): Boolean {
    return defaultNonTerminalRegex.matches(this)
}

fun guidedFuzz(generator: TerminalGuidedFuzzer, expected: String): List<Symbol> {
    var result: List<Symbol> = emptyList()
    val nonCoveredSymbolsBeforeRun = generator.nonCoveredSymbols
    println("Fuzzing time: ${measureTimeMillis {
        result = generator.fuzz()
    }} millis")

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
