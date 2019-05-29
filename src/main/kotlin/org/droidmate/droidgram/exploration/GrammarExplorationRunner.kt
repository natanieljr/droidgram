package org.droidmate.droidgram.exploration

import org.droidmate.api.ExplorationAPI
import org.droidmate.command.ExploreCommandBuilder
import org.droidmate.configuration.ConfigurationWrapper
import org.droidmate.device.android_sdk.Apk
import org.droidmate.exploration.SelectorFunction
import org.droidmate.exploration.StrategySelector
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig
import org.droidmate.misc.FailableExploration
import java.util.UUID

object GrammarExplorationRunner {
    suspend fun exploreWithGrammarInput(
        cfg: ConfigurationWrapper,
        input: String,
        grammarMapping: Map<String, UUID>
    ): Map<Apk, FailableExploration> {
        val selector: SelectorFunction = { _, pool, _ ->
            val strategy = pool.getFirstInstanceOf(GrammarStrategy::class.java)

            // Sync with grammar
            strategy?.grammarWatcher?.join()

            strategy
        }

        val builder = ExploreCommandBuilder.fromConfig(cfg)
            .insertBefore(StrategySelector.startExplorationReset, "Grammar", selector)
            .withStrategy(GrammarStrategy(input, grammarMapping))

        val modelProvider: (String) -> Model = { appName -> CustomModel(ModelConfig(appName, cfg = cfg)) }
        return ExplorationAPI.explore(cfg, commandBuilder = builder, modelProvider = modelProvider)
    }
}