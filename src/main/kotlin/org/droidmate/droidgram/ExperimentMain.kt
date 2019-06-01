package org.droidmate.droidgram

import com.natpryce.konfig.CommandLineOption
import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.droidgram.exploration.GrammarExplorationRunner
import org.droidmate.droidgram.exploration.GrammarReplayMF
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

object ExperimentMain {
    @JvmStatic
    private val log: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @JvmStatic
    private fun extraCmdOptions() = arrayOf(
        CommandLineOption(
            CommandLineConfig.inputDir,
            description = "Path to a directory with all inputs files to be used.",
            short = "i",
            metavar = "Path"
        )
    )

    private fun getTerminals(inputs: List<String>): Set<String> {
        return inputs.flatMap { input ->
            input.split(" ").filter {
                it.isNotEmpty()
            }
        }.toSet()
    }

    private fun getReachedElementsFiles(path: Path): List<Path> {
        return Files.walk(path)
            .filter { it.fileName.toString() == GrammarReplayMF.reachedTerminals }
            .toList()
    }

    private fun getReachedStatementsFiles(path: Path): List<Path> {
        return Files.walk(path)
            .filter { it.toAbsolutePath().toString().contains("/coverage/") }
            .filter { it.fileName.toString().contains("-statements-") }
            .toList()
    }

    private fun getReachedTerminals(cfg: ConfigurationWrapper): Set<String> {
        return getReachedElementsFiles(cfg.droidmateOutputDirPath)
            .flatMap { Files.readAllLines(it) }
            .filter { it.isNotEmpty() }
            .map { stmt -> stmt.takeWhile { it != ';' } }
            .toSet()
    }

    private fun getReachedStatements(cfg: ConfigurationWrapper): Set<Long> {
        return getReachedStatementsFiles(cfg.droidmateOutputDirPath)
            .flatMap { Files.readAllLines(it) }
            .filter { it.isNotEmpty() }
            .map { stmt -> stmt.takeWhile { it != ';' }.toLong() }
            .toSet()
    }

    @JvmStatic
    private fun calculateCodeCoverage(allStatements: Set<Long>, cfg: ConfigurationWrapper): Double {
        val coveredStatements = getReachedStatements(cfg)

        val missingStatements = allStatements - coveredStatements

        val difference = missingStatements.size.toDouble()

        log.info("Terminal coverage: ${1 - (difference / coveredStatements.size)}")

        val outputFile = cfg.droidmateOutputDirPath.resolve("codeCoverage.txt")

        val sb = StringBuilder()
        sb.appendln("Reached: $coveredStatements")
        sb.appendln("Missed: $missingStatements")
        sb.appendln("Coverage: ${1 - (difference / coveredStatements.size)}")

        Files.write(outputFile, sb.toString().toByteArray())

        return difference
    }

    @JvmStatic
    private fun calculateGrammarCoverage(terminals: Set<String>, cfg: ConfigurationWrapper): Double {
        val reachedTerminals = getReachedTerminals(cfg)

        val missingTerminals = terminals - reachedTerminals

        val difference = missingTerminals.size.toDouble()

        log.info("Terminal coverage: ${1 - (difference / terminals.size)}")

        val outputFile = cfg.droidmateOutputDirPath.resolve("grammarCoverage.txt")

        val sb = StringBuilder()
        sb.appendln("Terminals: $terminals")
        sb.appendln("Reached: $reachedTerminals")
        sb.appendln("Missed: $missingTerminals")
        sb.appendln("Coverage: ${1 - (difference / terminals.size)}")

        Files.write(outputFile, sb.toString().toByteArray())

        return difference
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val mainCfg = ExplorationAPI.config(args, *extraCmdOptions())

            val data = InputConfig(mainCfg)

            log.info("Reading inputs from: ${data.inputDir}")
            val seedList = data.inputs

            seedList.forEachIndexed { seed, inputs ->

                val seedArgs = arrayOf("--Output-outputDir=out/seed$seed", *args)
                val seedCfg = ExplorationAPI.config(seedArgs, *extraCmdOptions())

                val terminals = getTerminals(inputs)

                inputs.forEachIndexed { index, input ->
                    val experimentArgs = arrayOf("--Output-outputDir=out/seed$seed/input$index", *args)

                    val experimentCfg = ExplorationAPI.config(experimentArgs, *extraCmdOptions())
                    GrammarExplorationRunner.exploreWithGrammarInput(experimentCfg, input, data.translationTable)

                    calculateGrammarCoverage(terminals, experimentCfg)
                    calculateCodeCoverage(data.coverage, experimentCfg)
                }

                calculateGrammarCoverage(terminals, seedCfg)
                calculateCodeCoverage(data.coverage, seedCfg)
            }
        }
    }
}