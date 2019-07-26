package org.droidmate.droidgram.grammar

import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.droidgram.exploration.CustomState
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.strategy.AExplorationStrategy
import org.droidmate.explorationModel.factory.AbstractModel
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import java.util.UUID

class GrammarStrategy(
    private val priority: Int,
    private val generatedInput: String,
    private val grammarMapping: Map<String, UUID>,
    private val delay: Long
) : AExplorationStrategy() {
    private lateinit var grammarWatcher: GrammarReplayMF

    override fun getPriority(): Int = priority

    override suspend fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> hasNext(eContext: ExplorationContext<M, S, W>): Boolean {
        // Sync with grammar
        check(::grammarWatcher.isInitialized) { "Grammar watcher has not yet been initialized" }
        grammarWatcher.join()

        return true
    }

    override fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> initialize(initialContext: ExplorationContext<M, S, W>) {
        super.initialize(initialContext)

        grammarWatcher = (initialContext.findWatcher { it is GrammarReplayMF }
            ?: GrammarReplayMF(generatedInput, grammarMapping, delay)
                .also { initialContext.addWatcher(it) }) as GrammarReplayMF
    }

    override suspend fun <M : AbstractModel<S, W>, S : State<W>, W : Widget> computeNextAction(
        eContext: ExplorationContext<M, S, W>
    ): ExplorationAction {
        return grammarWatcher.nextAction(eContext.getCurrentState() as CustomState, true)
    }
}