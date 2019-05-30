package org.droidmate.droidgram

import com.natpryce.konfig.CommandLineOption
import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.droidgram.exploration.GrammarExplorationRunner
import org.droidmate.droidgram.exploration.GrammarReplayMF
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import kotlin.streams.toList

object ExperimentMain {
    @JvmStatic
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @JvmStatic
    private fun extraCmdOptions() = arrayOf(
        CommandLineOption(
            CommandLineConfig.inputs,
            description = "Path to a file with all inputs produced by the grammar (one input per line)",
            short = "i",
            metavar = "Path"
        ),
        CommandLineOption(
            CommandLineConfig.translation,
            description = "Path to a translation table between the grammar symbols and UUID (one per record line, split by ;)",
            short = "t",
            metavar = "Path"
        )
    )

    /**
     * Validate input arguments and return path to input file and translation table
     */
    @Throws(IllegalArgumentException::class)
    private fun preprocessCustomInputs(cfg: ConfigurationWrapper): Pair<Path, Path> {
        if (!cfg.contains(CommandLineConfig.inputs)) {
            throw IllegalArgumentException("Input file not set. Use -i <PATH> to set the path")
        }

        if (!cfg.contains(CommandLineConfig.translation)) {
            throw IllegalArgumentException("Translation table file not set. Use -m <PATH> to set the path")
        }

        val inputFile = Paths.get(cfg[CommandLineConfig.inputs].path).toAbsolutePath()

        if (!Files.exists(inputFile)) {
            throw IllegalArgumentException("Input file $inputFile does not exist")
        }

        val translationTable = Paths.get(cfg[CommandLineConfig.translation].path).toAbsolutePath()

        if (!Files.exists(inputFile)) {
            throw IllegalArgumentException("Translation table file $translationTable does not exist")
        }

        return Pair(inputFile, translationTable)
    }

    private fun getInputs(inputFile: Path): List<String> {
        return Files.readAllLines(inputFile)
            .filter { it.isNotEmpty() }
    }

    private fun getTerminals(inputs: List<String>): Set<String> {
        return inputs.flatMap {
            it.split(" ")
                .filter { it.isNotEmpty() }
        }.toSet()
    }

    private fun getTranslationTable(translationTableFile: Path): Map<String, UUID> {
        return Files.readAllLines(translationTableFile)
            .filter { it.isNotEmpty() }
            .map { line ->
                val data = line.split(";")

                assert(data.size == 2) { "Each line in the translation table should have 2 elements (ID;UUID)" }

                val id = data.first().trim()
                val uuid = UUID.fromString(data.last().trim())

                Pair(id, uuid)
            }.toMap()
    }

    private fun getReachedElementsFiles(path: Path): List<Path> {
        return Files.walk(path)
            .filter { it.fileName.toString() == GrammarReplayMF.reachedTerminals }
            .toList()
    }

    private fun getReachedTerminals(cfg: ConfigurationWrapper): Set<String> {
        return getReachedElementsFiles(cfg.droidmateOutputDirPath.parent)
            .flatMap { Files.readAllLines(it) }
            .filter { it.isNotEmpty() }
            .map { stmt -> stmt.takeWhile { it != ';' } }
            .toSet()
    }

    @JvmStatic
    private fun calculateGrammarCoverage(terminals: Set<String>, cfg: ConfigurationWrapper): Double {
        val reachedTerminals = getReachedTerminals(cfg)

        val missingTerminals = terminals - reachedTerminals

        val difference = missingTerminals.size.toDouble()

        log.info("Terminal coverage: ${1 - (difference / terminals.size)}")

        return difference
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val cfg = ExplorationAPI.config(args, *extraCmdOptions())

            val data = preprocessCustomInputs(cfg)

            val inputFile = data.first
            val translationTableFile = data.second

            log.info("Reading inputs from: $inputFile")
            log.info("Reading translation table from: $translationTableFile")

            val inputValues = getInputs(inputFile)
            val translationTable = getTranslationTable(translationTableFile)
            val terminals = getTerminals(inputValues)

            inputValues.forEachIndexed { index, input ->
                val experimentArgs = arrayOf("--Output-outputDir=out/$index", *args)

                val experimentCfg = ExplorationAPI.config(experimentArgs, *extraCmdOptions())
                GrammarExplorationRunner.exploreWithGrammarInput(experimentCfg, input, translationTable)
            }

            calculateGrammarCoverage(terminals, cfg)
        }
    }
}