package org.droidmate.droidgram.mining

import org.droidmate.deviceInterface.exploration.ActionType
import org.droidmate.deviceInterface.exploration.LaunchApp
import org.droidmate.deviceInterface.exploration.TextInsert
import org.droidmate.droidgram.grammar.Grammar
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.StringBuilder
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.BiPredicate
import kotlin.streams.toList

class GrammarExtractor(private val mModelDir: Path) {
    private val mIdMapping = mutableMapOf<String, String>()
    private val mGrammar = Grammar()

    private fun String.getId(prefix: String): String {
        val uuid = this.split("_").firstOrNull().orEmpty()

        return if (uuid.isEmpty()) {
            ""
        } else {
            mIdMapping.getOrPut(uuid) {
                prefix + (mIdMapping.values.count { it.startsWith(prefix) }.toString().padStart(2, '0'))
            }
        }
    }

    private fun List<String>.removeQueues(): List<String> = this
        .asSequence()
        .filterNot { it.contains(";ActionQueue-START;") }
        .filterNot { it.contains(";ActionQueue-End;") }
        .filterNot { it.contains(";EnableWifi;") }
        // .filterNot { it.contains(";PressBack;") }
        // .filterNot { it.contains(";Terminate;") }
        .filterNot { it.contains(";CloseKeyboard;") }
        .filterNot { it.contains(";Back;") }
        .filterNot { it.contains(";FetchGUI;") }
        .filterNot { it.contains(";LongClickEvent;null;") }
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
            val textualData = if (action == TextInsert.name) {
                ",${data.dropLast(1).last()}"
            } else if (action == "Swipe") {
                ",${data.dropLast(1).last()
                    .replace(",", ";")
                    .replace(" TO ", "TO")}"
            } else {
                ""
            }

            val resultState = data[3].getId("s")
            val resultStateNonTerminal = "<$resultState>"

            when (action) {
                LaunchApp.name -> {
                    grammar.addRule("<start>", resultStateNonTerminal)
                    grammar.addRule(sourceStateNonTerminal, "<empty>")
                }
                ActionType.PressBack.name -> {
                    val terminal = "$action($sourceStateUID)"
                    val nonTerminal = "<$action($sourceStateUID)>"
                    val productionRule = "$terminal $nonTerminal"

                    grammar.addRule(sourceStateNonTerminal, productionRule)
                    grammar.addRule(nonTerminal, resultStateNonTerminal)
                }
                ActionType.Terminate.name -> {
                    val productionRule = "<$action($sourceStateUID)>"

                    grammar.addRule(sourceStateNonTerminal, productionRule)
                    grammar.addRule(productionRule, "<empty>")
                }
                else -> {
                    val terminal = "$action($widgetUID$textualData)"
                    val nonTerminal = "<$action($sourceStateUID.$widgetUID$textualData)>"
                    val productionRule = "$terminal $nonTerminal"

                    grammar.addRule(sourceStateNonTerminal, productionRule)
                    grammar.addRule(nonTerminal, resultStateNonTerminal)
                }
            }
        }

        postProcessGrammar()
    }

    /**
     * Postprocessing includes: removing duplicate productions, removing terminate state
     */
    private fun postProcessGrammar() {
        grammar.mergeEquivalentTransitions()
        grammar.removeTerminateActions()
        grammar.removeSingleStateTransitions()
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
            val inputDir = (
                    Files.list(Paths.get(args.firstOrNull()))
                        .toList()
                        .sorted()
                        .firstOrNull { Files.isDirectory(it) } ?: throw IOException("Missing model dir path")
                    ).toAbsolutePath()

            val outputDir = Paths.get(args.getOrNull(1) ?: throw IOException("Missing output dir path"))
                .toAbsolutePath()

            val extractor = GrammarExtractor(inputDir)

            val grammarJSON = extractor.grammar.asJsonStr()
            val grammarFile = outputDir.resolve("grammar.txt")
            Files.write(grammarFile, grammarJSON.toByteArray())

            val mapping = StringBuilder()
            extractor.mapping
                .toSortedMap()
                .forEach { key, value ->
                    mapping.appendln("$key;$value")
                }

            val mappingFile = outputDir.resolve("translationTable.txt")
            Files.write(mappingFile, mapping.toString().toByteArray())

            println("Grammar:")
            val grammarStr = extractor.grammar.asString()
            print(grammarStr)

            println("\nMapping: $mappingFile")
            print(mapping.toString())
        }
    }
}
