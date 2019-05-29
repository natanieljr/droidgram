package org.droidmate.droidgram.exploration

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.exploration.strategy.AbstractStrategy
import java.util.UUID

class GrammarStrategy(generatedInput: String, grammarMapping: Map<String, UUID>) : AbstractStrategy() {
    internal val grammarWatcher by lazy {
        (eContext.findWatcher { it is GrammarReplayMF }
            ?: GrammarReplayMF(generatedInput, grammarMapping)
                .also { eContext.addWatcher(it) }) as GrammarReplayMF
    }

    override suspend fun internalDecide(): ExplorationAction {
        return grammarWatcher.nextAction(currentState, true)
    }
}