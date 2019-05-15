package org.droidmate.droidgram.exploration

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.command.ExploreCommandBuilder
import org.droidmate.exploration.SelectorFunction
import org.droidmate.exploration.StrategySelector
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig

object GrammarExplorationRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val cfg = ExplorationAPI.config(args)

            val generatedInput = ""
            val grammarMapping = mapOf("<s1>" to "")

            val selector: SelectorFunction = { _, pool, _ ->
                val strategy = pool.getFirstInstanceOf(GrammarStrategy::class.java)

                // Sync with grammar
                strategy?.grammarWatcher?.join()

                strategy
            }

            val builder = ExploreCommandBuilder.fromConfig(cfg)
                .insertBefore(StrategySelector.startExplorationReset, "Grammar", selector)
                .withStrategy(GrammarStrategy(generatedInput, grammarMapping))

            val modelProvider: (String) -> Model = { appName -> CustomModel(ModelConfig(appName, cfg = cfg)) }
            ExplorationAPI.explore(cfg, commandBuilder = builder, modelProvider = modelProvider)
        }
    }
}