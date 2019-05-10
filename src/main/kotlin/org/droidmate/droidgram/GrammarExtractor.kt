package org.droidmate.droidgram

import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BiPredicate

class GrammarExtractor(private val mModelDir: Path) {
    private val mIdMapping = mutableMapOf<String, String>()
    private val mGrammar = Grammar()

    private fun String.getId(prefix: String): String {
        val uuid = this.split("_").firstOrNull().orEmpty()

        return if (uuid.isEmpty()) {
            ""
        } else {
            mIdMapping.getOrPut(uuid) { prefix + mIdMapping.values.count { it.startsWith(prefix) } }
        }
    }

    private fun List<String>.removeQueues(): List<String> = this
        .asSequence()
        .filterNot { it.contains(";ActionQueue-START;") }
        .filterNot { it.contains(";ActionQueue-End;") }
        .filterNot { it.contains(";EnableWifi;") }
        .filterNot { it.contains(";PressBack;") }
        .filterNot { it.contains(";CloseKeyboard;") }
        .filterNot { it.contains(";Terminate;") }
        .filterNot { it.contains(";Back;") }
        .toList()

    private fun getTraceFile(modelDir: Path): List<String> {
        val trace = Files.find(
            modelDir,
            1,
            BiPredicate { p, _ -> p.fileName.toString().contains("trace") },
            FileVisitOption.FOLLOW_LINKS
        )
            .findFirst()

        if (!trace.isPresent) {
            throw FileNotFoundException("Unable to find trace file in $modelDir")
        } else {
            return Files.readAllLines(trace.get())
                .drop(1)
                .removeQueues()
        }
    }

    private fun extractGrammar() {
        assert(mIdMapping.isEmpty()) { "Grammar cannot be re-generated in the same instance" }
        val trace = getTraceFile(mModelDir)

        trace.forEach { entry ->
            val data = entry.split(";")
            val sourceStateUID = data[0].getId("s")
            val sourceStateNonTerminal = "<$sourceStateUID>"
            val action = data[1]
            val widgetUID = if (data[2] != "null") {
                data[2].getId("w")
            } else {
                data[2]
            }
            val resultState = data[3].getId("s")
            val resultStateNonTerminal = "<$resultState>"

            val terminal = "$widgetUID.$action"
            val nonTerminal = "<$sourceStateUID.$widgetUID.$action>"

            if (action == "LaunchApp") {
                grammar.addRule("<start>", resultStateNonTerminal)
            } else {
                val productionRule = "$terminal $nonTerminal"
                grammar.addRule(sourceStateNonTerminal, productionRule)
                grammar.addRule(nonTerminal, resultStateNonTerminal)
            }
        }
    }

    val grammar by lazy {
        if (mIdMapping.isEmpty()) {
            extractGrammar()
        }
        mGrammar
    }

    val mapping by lazy {
        if (mIdMapping.isEmpty()) {
            extractGrammar()
        }

        mIdMapping
            .map { Pair(it.value, it.key) }
            .toMap()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val inputDir = Paths.get(args.firstOrNull() ?: throw IOException("Missing model dir path"))
                .toAbsolutePath()

            val extractor = GrammarExtractor(inputDir)

            println("Grammar:")
            extractor.grammar.print()

            println("\nMapping:")
            extractor.mapping
                .toSortedMap()
                .forEach { key, value ->
                    println("$key ->\t$value")
                }
        }
    }
}
