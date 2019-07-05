package org.droidmate.droidgram

import com.natpryce.konfig.CommandLineOption
import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.droidgram.runner.GrammarExploration
import org.droidmate.droidgram.runner.DefaultExploration
import org.droidmate.droidgram.mining.GrammarExtractor
import org.droidmate.droidgram.reporter.ResultBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

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
        ),
        CommandLineOption(
            CommandLineConfig.seedNr,
            description = "Number of the seed to be processed.",
            short = "s",
            metavar = "Int"
        )
    )

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            if (args.contains("result")) {
                ResultBuilder.main(args.filterNot { it.contains("result") }.toTypedArray())
                exitProcess(0)
            } else if (args.contains("extract")) {
                GrammarExtractor.main(args.filterNot { it.contains("extract") }.toTypedArray())
                exitProcess(0)
            } else if (args.contains("run")) {
                DefaultExploration.main(args.filterNot { it.contains("run") }.toTypedArray())
                exitProcess(0)
            }

            val mainCfg = ExplorationAPI.config(args, *extraCmdOptions())

            val data = InputConfig(mainCfg)

            log.info("Reading inputs from: ${data.inputDir}")

            val seedList = data.inputs

            seedList.forEachIndexed { seed, inputs ->
                val seedDir = mainCfg.droidmateOutputDirPath.resolve("seed$seed")
                val seedArgs = arrayOf(*args, "--Output-outputDir=$seedDir")
                val seedCfg = ExplorationAPI.config(seedArgs, *extraCmdOptions())

                inputs.forEachIndexed { index, input ->
                    val experimentDir = seedCfg.droidmateOutputDirPath.resolve("input$index")
                    val experimentArgs = arrayOf(*args,
                        "--UiAutomatorServer-imgQuality=10",
                        "--Output-outputDir=$experimentDir")

                    val experimentCfg = ExplorationAPI.config(experimentArgs, *extraCmdOptions())
                    GrammarExploration.exploreWithGrammarInput(experimentCfg, input, data.translationTable)

                    // Grammar coverage relative to current input
                    ResultBuilder.generateGrammarCoverage(input, experimentCfg.droidmateOutputDirPath)
                    // Code coverage relative to the overall grammar
                    ResultBuilder.generateCodeCoverage(data.coverage, experimentCfg.droidmateOutputDirPath)
                }

                ResultBuilder.generateInputSize(inputs, seedCfg.droidmateOutputDirPath)
                // Grammar coverage relative to the overall grammar
                ResultBuilder.generateGrammarCoverage(inputs, seedCfg.droidmateOutputDirPath)
                // Code coverage relative to the overall grammar
                ResultBuilder.generateCodeCoverage(data.coverage, seedCfg.droidmateOutputDirPath)
            }

            ResultBuilder.generateSummary(data.inputs, data.coverage, mainCfg.droidmateOutputDirPath, data.seedNr)
        }
    }
}