package org.droidmate.droidgram.runner

import kotlinx.coroutines.runBlocking
import org.droidmate.api.ExplorationAPI
import org.droidmate.droidgram.exploration.CustomModelProvider

object DefaultExploration {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val cfg = ExplorationAPI.config(args)

            val modelProvider = CustomModelProvider()
            ExplorationAPI.explore(
                cfg,
                watcher = emptyList(),
                modelProvider = modelProvider
            )
        }
    }
}
