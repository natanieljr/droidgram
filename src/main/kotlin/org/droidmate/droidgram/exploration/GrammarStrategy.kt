package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.exploration.strategy.widget.ExplorationStrategy

class GrammarStrategy(generatedInput: String, grammarMapping: Map<String, String>) : ExplorationStrategy() {
    internal val grammarWatcher by lazy {
        (eContext.findWatcher { it is GrammarReplayMF }
            ?: GrammarReplayMF(generatedInput, grammarMapping)
                .also { eContext.addWatcher(it) }) as GrammarReplayMF
    }

    override suspend fun chooseAction(): ExplorationAction {
        return grammarWatcher.nextAction(currentState, true)
    }
}