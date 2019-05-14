package org.droidmate.droidgram.mining

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.droidgram.exploration.CustomModel
import org.droidmate.explorationModel.Model
import org.droidmate.explorationModel.config.ModelConfig

object ExplorationRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val cfg = ExplorationAPI.config(args)

            val modelProvider: (String) -> Model = { appName -> CustomModel(ModelConfig(appName, cfg = cfg)) }
            ExplorationAPI.explore(cfg, modelProvider = modelProvider)
        }
    }
}