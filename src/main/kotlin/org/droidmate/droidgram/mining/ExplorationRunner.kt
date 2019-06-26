package org.droidmate.droidgram.mining

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.droidgram.exploration.CustomModelProvider

object ExplorationRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val cfg = ExplorationAPI.config(args)

            val modelProvider = CustomModelProvider()
            ExplorationAPI.explore(cfg, modelProvider = modelProvider)
        }
    }
}
