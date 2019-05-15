package org.droidmate.droidgram.exploration

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import org.droidmate.deviceInterface.exploration.ActionType
import org.droidmate.deviceInterface.exploration.ExplorationAction
import org.droidmate.deviceInterface.exploration.GlobalAction
import org.droidmate.exploration.ExplorationContext
import org.droidmate.exploration.actions.click
import org.droidmate.exploration.actions.clickEvent
import org.droidmate.exploration.actions.longClick
import org.droidmate.exploration.actions.longClickEvent
import org.droidmate.exploration.actions.resetApp
import org.droidmate.exploration.actions.terminateApp
import org.droidmate.exploration.actions.tick
import org.droidmate.exploration.modelFeatures.ModelFeature
import org.droidmate.explorationModel.interaction.State
import org.droidmate.explorationModel.interaction.Widget
import java.lang.IllegalStateException
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class GrammarReplayMF(generatedInput: String, val grammarMapping: Map<String, String>) : ModelFeature() {
    override val coroutineContext: CoroutineContext = CoroutineName("GrammarReplayMF") + Job()

    private var currIndex: Int = -2
    private lateinit var context: ExplorationContext

    private val translationTable: Map<String, UUID> by lazy {
        grammarMapping
            .mapValues { UUID.fromString(it.value) }
    }

    private val inputList by lazy {
        generatedInput
            .split(" ")
            .flatMap {
                val targetAction = it.split(".")
                assert(targetAction.size == 2) { "Invalid target action: $targetAction. Expecting only one '.'" }
                val target = targetAction[0]
                val action = targetAction[1]

                listOf(Pair(getUID(target), action), Pair(getUID(target), "FetchGUI"))
            }
    }

    private fun getUID(key: String): UUID {
        return translationTable.get(key) ?: throw IllegalArgumentException("Key $key not found in the translation table")
    }

    override fun onAppExplorationStarted(context: ExplorationContext) {
        this.context = context
    }

    private fun Widget.toAction(): ExplorationAction {
        val actionStr = inputList[currIndex].second

        return when (actionStr) {
            "Click" -> this.click(0, true)
            "ClickEvent" -> this.clickEvent(0, true)

            "LongClick" -> this.longClick(0, true)
            "LongClickEvent" -> this.longClickEvent(0, true)

            "Tick" -> this.tick(0, true)

            else -> throw IllegalStateException("Unsupported action type: $actionStr")
        }
    }

    fun nextAction(state: State): ExplorationAction {
        currIndex++
        return when {
            currIndex >= inputList.size -> terminateApp()

            currIndex < 0 -> context.resetApp()

            else -> {
                val target = inputList[currIndex]
                val targetUID = target.first
                val targetWidget = state.actionableWidgets.firstOrNull { it.uid == targetUID }

                if (targetWidget != null) {
                    targetWidget.toAction()
                } else if (target.second == "FetchGUI") {
                    GlobalAction(ActionType.FetchGUI)
                } else {
                    nextAction(state)
                }
            }
        }
    }
}