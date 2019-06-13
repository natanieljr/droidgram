package org.droidmate.droidgram

import com.natpryce.konfig.CommandLineOption
import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.droidgram.ResultBuilder.uniqueSet
import org.droidmate.droidgram.exploration.GrammarExplorationRunner
import org.droidmate.droidgram.exploration.GrammarReplayMF
import org.droidmate.droidgram.mining.ExplorationRunner
import org.droidmate.droidgram.mining.GrammarExtractor
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

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            if (args.contains("extract")) {
                GrammarExtractor.main(args.filterNot { it.contains("extract") }.toTypedArray())
                System.exit(0)
            } else if (args.contains("run")) {
                ExplorationRunner.main(args.filterNot { it.contains("run") }.toTypedArray())
                System.exit(0)
            }

            val mainCfg = ExplorationAPI.config(args, *extraCmdOptions())

            val data = InputConfig(mainCfg)

            log.info("Reading inputs from: ${data.inputDir}")
            val seedList = data.inputs

            seedList.forEachIndexed { seed, inputs ->

                val seedDir = mainCfg.droidmateOutputDirPath.resolve("seed$seed")
                val seedArgs = arrayOf(*args, "--Output-outputDir=$seedDir")
                val seedCfg = ExplorationAPI.config(seedArgs, *extraCmdOptions())

                val terminals = inputs

                inputs.forEachIndexed { index, input ->
                    val experimentDir = seedCfg.droidmateOutputDirPath.resolve("input$index")
                    val experimentArgs = arrayOf(*args, "--Output-outputDir=$experimentDir")

                    val experimentCfg = ExplorationAPI.config(experimentArgs, *extraCmdOptions())
                    GrammarExplorationRunner.exploreWithGrammarInput(experimentCfg, input, data.translationTable)

                    ResultBuilder.generateGrammarCoverage(terminals, experimentCfg.droidmateOutputDirPath)
                    ResultBuilder.generateCodeCoverage(data.coverage, experimentCfg.droidmateOutputDirPath)
                }

                ResultBuilder.generateGrammarCoverage(terminals, seedCfg.droidmateOutputDirPath)
                ResultBuilder.generateCodeCoverage(data.coverage, seedCfg.droidmateOutputDirPath)
            }
        }
    }
}